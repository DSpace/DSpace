/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * The Plugin Manager is a very simple component container.  It creates and
 * organizes components (plugins), and helps select a plugin in the cases
 * where there are many possible choices.  It also gives some limited
 * control over the lifecycle of a plugin.  It manages three different types
 * (usage patterns) of plugins:
 * <p>
 * <ol><li> Singleton Plugin
 * <br>  There is only one implementation class for the plugin.  It is indicated
 *   in the configuration.  This type of plugin chooses an implementations of
 *   a service, for the entire system, at configuration time.  Your
 *   application just fetches the plugin for that interface and gets the
 *   configured-in choice.
 *
 * <p><li> Sequence Plugins
 *  <br> You need a sequence or series of plugins, to implement a mechanism like
 *   StackableAuthenticationMethods or a pipeline, where each plugin is
 *   called in order to contribute its implementation of a process to the
 *   whole.
 *  <p><li> Named Plugins
 *  <br> Use a named plugin when the application has to choose one plugin
 *   implementation out of many available ones.  Each implementation is bound
 *   to one or more names (symbolic identifiers) in the configuration.
 *  </ol><p>
 *  The name is just a <code>String</code> to be associated with the
 *  combination of implementation class and interface.  It may contain
 *  any characters except for comma (,) and equals (=).  It may contain
 *  embedded spaces.  Comma is a special character used to separate
 *  names in the configuration entry.
 *
 * @author Larry Stone
 * @see SelfNamedPlugin
 */
public class PluginManager
{
    /** log4j category */
    private static Logger log = Logger.getLogger(PluginManager.class);

    /**
     * Prefixes of names of properties to look for in DSpace Configuration
     */
    private static final String SINGLE_PREFIX = "plugin.single.";
    private static final String SEQUENCE_PREFIX = "plugin.sequence.";
    private static final String NAMED_PREFIX = "plugin.named.";
    private static final String SELFNAMED_PREFIX = "plugin.selfnamed.";
    private static final String REUSABLE_PREFIX = "plugin.reusable.";

    /** Configuration name of paths to search for third-party plugins. */
    private static final String CLASSPATH = "plugin.classpath";

    // Separator character (from perl $;) to make "two dimensional"
    // hashtable key out of interface classname and plugin name;
    // this character separates the words.
    private static final String SEP = "\034";

    /** Paths to search for third-party plugins. */
    private static final String[] classPath;
    static {
        String path = ConfigurationManager.getProperty(CLASSPATH);
        if (null == path)
            classPath = new String[0];
        else
            classPath = path.split(":");
    }

    /** Custom class loader to search for third-party plugins. */
    private static final PathsClassLoader loader
            = new PathsClassLoader(PluginManager.class.getClassLoader(), classPath);

    // Map of plugin class to "reusable" metric (as Boolean, must be Object)
    // Key is Class, value is Boolean (true by default).
    private static Map<Class<Object>, Boolean> cacheMeCache = new HashMap<Class<Object>, Boolean>();

    /**
     * Whether or not to cache instances of this class. Ironically,
     * the cacheability information is itself cached.
     * <P>
     * By default, all plugin class instances ARE cached. To disable instance
     * caching for a specific plugin class, you must add a configuration similar
     * to this in your dspace.cfg:
     * <code>
     * plugin.reusable.[full-class-name] = false
     * </code>
     * @return true if class instances should be cached in memory, false otherwise.
     */
    private static boolean cacheMe(String module, Class implClass)
    {
        if (cacheMeCache.containsKey(implClass))
        {
            return (cacheMeCache.get(implClass)).booleanValue();
        }
        else
        {
            String key = REUSABLE_PREFIX+implClass.getName();
            boolean reusable = (module != null) ?
                ConfigurationManager.getBooleanProperty(module, key, true) :
                ConfigurationManager.getBooleanProperty(key, true);
            cacheMeCache.put(implClass, Boolean.valueOf(reusable));
            return reusable;
        }
    }

    /**
     * Returns an instance of the singleton (single) plugin implementing
     * the given interface.  There must be exactly one single plugin
     * configured for this interface, otherwise the
     * <code>PluginConfigurationError</code> is thrown.
     * <p>
     * Note that this is the only "get plugin" method which throws an
     * exception.  It is typically used at initialization time to set up
     * a permanent part of the system so any failure is fatal.
     *
     * @param interfaceClass interface Class object
     * @return instance of plugin
     * @throws PluginConfigurationError
     */
    public static Object getSinglePlugin(Class interfaceClass)
        throws PluginConfigurationError, PluginInstantiationException
    {
        return getSinglePlugin(null, interfaceClass);
    }

    /**
     * Returns an instance of the singleton (single) plugin implementing
     * the given interface.  There must be exactly one single plugin
     * configured for this interface, otherwise the
     * <code>PluginConfigurationError</code> is thrown.
     * <p>
     * Note that this is the only "get plugin" method which throws an
     * exception.  It is typically used at initialization time to set up
     * a permanent part of the system so any failure is fatal.
     *
     * @param module name of config module, or <code>null</code> for standard location
     * @param interfaceClass interface Class object
     * @return instance of plugin
     * @throws PluginConfigurationError
     */
    public static Object getSinglePlugin(String module, Class interfaceClass)
        throws PluginConfigurationError, PluginInstantiationException
    {
        String iname = interfaceClass.getName();

        // configuration format is  prefix.<interface> = <classname>
        String classname = getConfigProperty(module, SINGLE_PREFIX+iname);

        if (classname != null)
        {
            return getAnonymousPlugin(module, classname.trim());
        }
        else
        {
            throw new PluginConfigurationError("No Single Plugin configured for interface \""+iname+"\"");
        }
    }

    // cache of config data for Sequence Plugins; format its
    // <interface-name> -> [ <classname>.. ]  (value is Array)
    private static Map<String, String[]> sequenceConfig = new HashMap<String, String[]>();

    /**
     * Returns instances of all plugins that implement the interface
     * intface, in an Array.  Returns an empty array if no there are no
     * matching plugins.
     * <p>
     * The order of the plugins in the array is the same as their class
     * names in the configuration's value field.
     *
     * @param intfc interface for which to find plugins.
     * @return an array of plugin instances; if none are
     *   available an empty array is returned.
     */
    public static Object[] getPluginSequence(Class intfc)
        throws PluginInstantiationException
    {
        return getPluginSequence(null, intfc);
    }

    /**
     * Returns instances of all plugins that implement the interface
     * intface, in an Array.  Returns an empty array if no there are no
     * matching plugins.
     * <p>
     * The order of the plugins in the array is the same as their class
     * names in the configuration's value field.
     *
     * @param module name of config module, or <code>null</code> for standard
     * @param intfc interface for which to find plugins.
     * @return an array of plugin instances; if none are
     *   available an empty array is returned.
     */
    public static Object[] getPluginSequence(String module, Class intfc)
        throws PluginInstantiationException
    {
        // cache the configuration for this interface after grovelling it once:
        // format is  prefix.<interface> = <classname>
        String iname = intfc.getName();
        String classname[] = null;
        if (!sequenceConfig.containsKey(iname))
        {
            String val = getConfigProperty(module, SEQUENCE_PREFIX+iname);
            if (val == null)
            {
                log.warn("No Configuration entry found for Sequence Plugin interface="+iname);
                return (Object[]) Array.newInstance(intfc, 0);
            }
            classname = val.trim().split("\\s*,\\s*");
            sequenceConfig.put(iname, classname);
        }
        else
        {
            classname = sequenceConfig.get(iname);
        }

        Object result[] = (Object[])Array.newInstance(intfc, classname.length);
        for (int i = 0; i < classname.length; ++i)
        {
            log.debug("Adding Sequence plugin for interface= "+iname+", class="+classname[i]);
            result[i] = getAnonymousPlugin(module, classname[i]);
        }
        return result;
    }


    // Map of cached (reusable) single plugin instances - class -> instance.
    private static Map<Serializable, Object> anonymousInstanceCache = new HashMap<Serializable, Object>();

    // Get possibly-cached plugin instance for un-named plugin,
    // this is shared by Single and Sequence plugins.
    private static Object getAnonymousPlugin(String module, String classname)
        throws PluginInstantiationException
    {
        try
        {
            Class pluginClass = Class.forName(classname, true, loader);
            if (cacheMe(module, pluginClass))
            {
                Object cached = anonymousInstanceCache.get(pluginClass);
                if (cached == null)
                {
                    cached = pluginClass.newInstance();
                    anonymousInstanceCache.put(pluginClass, cached);
                }
                return cached;
            }
            else
            {
                return pluginClass.newInstance();
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginInstantiationException("Cannot load plugin class: " +
            		                               e.toString(), e);
        }
        catch (InstantiationException e)
        {
            throw new PluginInstantiationException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new PluginInstantiationException(e);
        }
    }

    // Map of named plugin classes, [intfc,name] -> class
    // Also contains intfc -> "marker" to mark when interface has been loaded.
    private static Map<String, String> namedPluginClasses = new HashMap<String, String>();

    // Map of cached (reusable) named plugin instances, [class,name] -> instance
    private static Map<Serializable, Object> namedInstanceCache = new HashMap<Serializable, Object>();

    // load and cache configuration data for the given interface.
    private static void configureNamedPlugin(String module, String iname)
        throws ClassNotFoundException
    {
        int found = 0;

        /**
         * First load the class map for this interface (if not done yet):
         * key is [intfc,name], value is class.
         * There is ALSO a "marker key" of "intfc" by itself to show we
         * loaded this intfc's configuration.
         */
        if (!namedPluginClasses.containsKey(iname))
        {
            // 1. Get classes named by the configuration. format is:
            //    plugin.named.<INTF> = <CLASS> = <name>, <name> [,] \
            //                        <CLASS> = <name>, <name> [ ... ]
            String namedVal = getConfigProperty(module, NAMED_PREFIX+iname);
            if (namedVal != null)
            {
                namedVal = namedVal.trim();
                log.debug("Got Named configuration for interface="+iname+", config="+namedVal);

                // match  "<classname> ="
                Pattern classnameEqual = Pattern.compile("([\\w\\p{Sc}\\.]+)\\s*\\=");

                int prevEnd = -1;
                String prevClassName = null;
                Matcher classMatcher = classnameEqual.matcher(namedVal);
                while (classMatcher.find())
                {
                    if (prevClassName != null)
                    {
                        found += installNamedConfigs(iname, prevClassName,
                                namedVal.substring(prevEnd, classMatcher.start()).trim().split("\\s*,\\s*"));
                    }
                    prevClassName = classMatcher.group(1);
                    prevEnd = classMatcher.end();
                }
                if (prevClassName != null)
                {
                    found += installNamedConfigs(iname, prevClassName,
                            namedVal.substring(prevEnd).trim().split("\\s*,\\s*"));
                }
            }

            // 2. Get Self-named config entries:
            // format is plugin.selfnamed.<INTF> = <CLASS> , <CLASS> ..
            String selfNamedVal = getConfigProperty(module, SELFNAMED_PREFIX+iname);
            if (selfNamedVal != null)
            {
                String classnames[] = selfNamedVal.trim().split("\\s*,\\s*");
                for (int i = 0; i < classnames.length; ++i)
                {
                    try
                    {
                        Class pluginClass = Class.forName(classnames[i], true, loader);
                        String names[] = (String[])pluginClass.getMethod("getPluginNames").
                                                   invoke(null);
                        if (names == null || names.length == 0)
                        {
                            log.error("Self-named plugin class \"" + classnames[i] + "\" returned null or empty name list!");
                        }
                        else
                        {
                            found += installNamedConfigs(iname, classnames[i], names);
                        }
                    }
                    catch (NoSuchMethodException e)
                    {
                        log.error("Implementation Class \""+classnames[i]+"\" is not a subclass of SelfNamedPlugin, it has no getPluginNames() method.");
                    }
                    catch (Exception e)
                    {
                        log.error("While configuring self-named plugin: " + e.toString());
                    }
                }
            }
            namedPluginClasses.put(iname, "org.dspace.core.marker");
            if (found == 0)
            {
                log.error("No named plugins found for interface=" + iname);
            }
        }
    }

    // add info for a named plugin to cache, under all its names.
    private static int installNamedConfigs(String iname, String classname, String names[])
        throws ClassNotFoundException
    {
        int found = 0;
        for (int i = 0; i < names.length; ++i)
        {
            String key = iname+SEP+names[i];
            if (namedPluginClasses.containsKey(key))
            {
                log.error("Name collision in named plugin, implementation class=\"" + classname +
                        "\", name=\"" + names[i] + "\"");
            }
            else
            {
                namedPluginClasses.put(key, classname);
            }
            log.debug("Got Named Plugin, intfc="+iname+", name="+names[i]+", class="+classname);
            ++found;
        }
        return found;
    }

    /**
     * Returns an instance of a plugin that implements the interface
     * intface and is bound to a name matching name.  If there is no
     * matching plugin, it returns null.  The names are matched by
     * String.equals().
     *
     * @param intfc the interface class of the plugin
     * @param name under which the plugin implementation is configured.
     * @return instance of plugin implementation, or null if there is no match or an error.
     */
    public static Object getNamedPlugin(Class intfc, String name)
         throws PluginInstantiationException
    {
        return getNamedPlugin(null, intfc, name);
    }

    /**
     * Returns an instance of a plugin that implements the interface
     * intface and is bound to a name matching name.  If there is no
     * matching plugin, it returns null.  The names are matched by
     * String.equals().
     *
     * @param module config module, or <code>null</code> for standard location
     * @param intfc the interface class of the plugin
     * @param name under which the plugin implementation is configured.
     * @return instance of plugin implementation, or null if there is no match or an error.
     */
    public static Object getNamedPlugin(String module, Class intfc, String name)
         throws PluginInstantiationException
    {
        try
        {
            String iname = intfc.getName();
            configureNamedPlugin(module, iname);
            String key = iname + SEP + name;
            String cname = namedPluginClasses.get(key);
            if (cname == null)
            {
                log.warn("Cannot find named plugin for interface=" + iname + ", name=\"" + name + "\"");
            }
            else
            {
                Class pluginClass = Class.forName(cname, true, loader);
                if (cacheMe(module, pluginClass))
                {
                    String nkey = pluginClass.getName() + SEP + name;
                    Object cached = namedInstanceCache.get(nkey);
                    if (cached == null)
                    {
                        log.debug("Creating cached instance of: " + cname +
                                          " for interface=" + iname +
                                          " pluginName=" + name );
                        cached = pluginClass.newInstance();
                        if (cached instanceof SelfNamedPlugin)
                        {
                            ((SelfNamedPlugin) cached).setPluginInstanceName(name);
                        }
                        namedInstanceCache.put(nkey, cached);
                    }
                    return cached;
                }
                else
                {
                        log.debug("Creating UNcached instance of: " + cname +
                                  " for interface=" + iname +
                                  " pluginName=" + name );
                    Object result = pluginClass.newInstance();
                    if (result instanceof SelfNamedPlugin)
                    {
                        ((SelfNamedPlugin) result).setPluginInstanceName(name);
                    }
                    return result;
                }
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginInstantiationException("Cannot load plugin class: " +
            		                               e.toString(), e);
        }
        catch (InstantiationException e)
        {
            throw new PluginInstantiationException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new PluginInstantiationException(e);
        }

        return null;
    }

    /**
     * Returns whether a plugin exists which implements the specified interface
     * and has a specified name.  If a matching plugin is found to be configured,
     * return true. If there is no matching plugin, return false.
     *
     * @param intfc the interface class of the plugin
     * @param name under which the plugin implementation is configured.
     * @return true if plugin was found to be configured, false otherwise
     */
    public static boolean hasNamedPlugin(Class intfc, String name)
         throws PluginInstantiationException
    {
        return hasNamedPlugin(null, intfc, name);
    }

   /**
     * Returns whether a plugin exists which implements the specified interface
     * and has a specified name.  If a matching plugin is found to be configured,
     * return true. If there is no matching plugin, return false.
     *
     * @param module the config module or <code>null</code> for regular location
     * @param intfc the interface class of the plugin
     * @param name under which the plugin implementation is configured.
     * @return true if plugin was found to be configured, false otherwise
     */
    public static boolean hasNamedPlugin(String module, Class intfc, String name)
         throws PluginInstantiationException
    {
        try
        {
            String iname = intfc.getName();
            configureNamedPlugin(module, iname);
            String key = iname + SEP + name;
            return namedPluginClasses.get(key) != null;
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginInstantiationException("Cannot load plugin class: " +
            		                               e.toString(), e);
        }
    }

    /**
     * Returns all of the names under which a named plugin implementing
     * the interface intface can be requested (with getNamedPlugin()).
     * The array is empty if there are no matches.  Use this to populate
     * a menu of plugins for interactive selection, or to document what
     * the possible choices are.
     * <p>
     * NOTE: The names are NOT returned in any deterministic order.
     *
     * @param intfc plugin interface for which to return names.
     * @return an array of strings with every name; if none are
     *   available an empty array is returned.
     */
    public static String[] getAllPluginNames(Class intfc)
    {
            return getAllPluginNames(null, intfc);
    }

    /**
     * Returns all of the names under which a named plugin implementing
     * the interface intface can be requested (with getNamedPlugin()).
     * The array is empty if there are no matches.  Use this to populate
     * a menu of plugins for interactive selection, or to document what
     * the possible choices are.
     * <p>
     * NOTE: The names are NOT returned in any deterministic order.
     *
     * @param module the module name
     * @param intfc plugin interface for which to return names.
     * @return an array of strings with every name; if none are
     *   available an empty array is returned.
     */
    public static String[] getAllPluginNames(String module, Class intfc)
    {
        try
        {
            String iname = intfc.getName();
            configureNamedPlugin(module, iname);
            String prefix = iname + SEP;
            ArrayList<String> result = new ArrayList<String>();

            for (String key : namedPluginClasses.keySet())
            {
                if (key.startsWith(prefix))
                {
                    result.add(key.substring(prefix.length()));
                }
            }
            if (result.size() == 0)
            {
                log.error("Cannot find any names for named plugin, interface=" + iname);
            }

            return result.toArray(new String[result.size()]);
        }
        catch (ClassNotFoundException e)
        {
            return new String[0];
        }
    }

    /**
     * Tells the Plugin Manager to let go of any references to a
     * reusable plugin, to prevent it from being given out again and to
     * allow the object to be garbage-collected.  Call this when a
     * plugin instance must be taken out of circulation.
     *
     * @param plugin the object to release, must have been created by
     *   <code>getNamedPlugin</code> etc.
     */
    public static void releasePlugin(Object plugin)
    {
        forgetInstance(plugin, namedInstanceCache);
        forgetInstance(plugin, anonymousInstanceCache);
    }

    private static void forgetInstance(Object plugin, Map<Serializable, Object> cacheMap)
    {
        Collection values = cacheMap.values();
        Iterator ci = values.iterator();
        while (ci.hasNext())
        {
            // Identity comparison is valid for this usage
            Object val = ci.next();
            if (val == plugin)
            {
                values.remove(val);
            }
        }
    }

    /* -----------------------------------------------------------------
     *  Code to check configuration is all below this line
     * -----------------------------------------------------------------
     */

    // true if classname is valid and loadable.
    private static boolean checkClassname(String iname, String msg)
    {
        try
        {
            if (Class.forName(iname, true, loader) != null)
            {
                return true;
            }
        }
        catch (ClassNotFoundException ce)
        {
            log.error("No class definition found for "+msg+": \""+iname+"\"");
        }
        return false;
    }

    // true if classname is loadable AND is subclass of SelfNamedPlugin
    private static boolean checkSelfNamed(String iname)
    {
        try
        {
            if (!checkSelfNamed(Class.forName(iname, true, loader)))
            {
                log.error("The class \"" + iname + "\" is NOT a subclass of SelfNamedPlugin but it should be!");
            }
        }
        catch (ClassNotFoundException ce)
        {
            log.error("No class definition found for self-named class interface: \""+iname+"\"");
        }
        return false;
    }

    // recursively climb superclass stack until we find SelfNamedPlugin
    private static boolean checkSelfNamed(Class cls)
    {
        Class sup = cls.getSuperclass();
        if (sup == null)
        {
            return false;
        }
        else if (sup.equals(SelfNamedPlugin.class))
        {
            return true;
        }
        else
        {
            return checkSelfNamed(sup);
        }
    }

    // check named-plugin names by interface -- call the usual
    // configuration and let it find missing or duplicate names.
    private static void checkNames(String iname)
    {
        try
        {
            configureNamedPlugin(null, iname);
        }
        catch (ClassNotFoundException ce)
        {
            // bogus classname should be old news by now.
        }
    }

    // get module-specific, or generic configuration property
    private static String getConfigProperty(String module, String property)
    {
        if (module != null) {
            return ConfigurationManager.getProperty(module, property);
        }
        return ConfigurationManager.getProperty(property);
    }

    /**
     * Validate the entries in the DSpace Configuration relevant to
     * PluginManager.  Look for inconsistencies, illegal syntax, etc.
     * Announce violations with "log.error" so they appear in the log
     * or in the standard error stream if this is run interactively.
     * <ul>
     * <li>Look for duplicate keys (by parsing the config file)
     * <li>Interface in plugin.single, plugin.sequence, plugin.named, plugin.selfnamed is valid.
     * <li>Classname in plugin.reusable exists and matches a plugin config.
     * <li>Classnames in config values exist.
     * <li>Classnames in plugin.selfnamed loads and is subclass of <code>SelfNamedPlugin</code>
     * <li>Implementations of named plugin have no name collisions.
     * <li>Named plugin entries lacking names.
     * </ul>
     */
    public static void checkConfiguration()
        throws IOException
    {
        FileReader fr = null;
        BufferedReader cr = null;

        /*  XXX TODO:  (maybe) test that implementation class is really a
         *  subclass or impl of the plugin "interface"
         */

        // tables of config keys for each type of config line:
        Map<String, String> singleKey = new HashMap<String, String>();
        Map<String, String> sequenceKey = new HashMap<String, String>();
        Map<String, String> namedKey = new HashMap<String, String>();
        Map<String, String> selfnamedKey = new HashMap<String, String>();
        Map<String, String> reusableKey = new HashMap<String, String>();
        HashMap<String, String> keyMap = new HashMap<String, String>();

        // 1. First pass -- grovel the actual config file to check for
        //    duplicate keys, since Properties class hides them from us.
        //    Also build lists of each type of key, check for misspellings.
        File config = ConfigurationManager.getConfigurationFile();
        try
        {
            fr = new FileReader(config);
            cr = new BufferedReader(fr);
            String line = null;
            boolean continued = false;
            Pattern keyPattern = Pattern.compile("([^\\s\\=\\:]+)");
            while ((line = cr.readLine()) != null)
            {
                line = line.trim();
                if (line.startsWith("!") || line.startsWith("#"))
                {
                    continued = false;
                }
                else
                {
                    if (!continued && line.startsWith("plugin."))
                    {
                        Matcher km = keyPattern.matcher(line);
                        if (km.find())
                        {
                            String key = line.substring(0, km.end(1));
                            if (keyMap.containsKey(key))
                            {
                                log.error("Duplicate key \"" + key + "\" in DSpace configuration file=" + config.toString());
                            }
                            else
                            {
                                keyMap.put(key, key);
                            }

                            if (key.startsWith(SINGLE_PREFIX))
                            {
                                singleKey.put(key.substring(SINGLE_PREFIX.length()), key);
                            }
                            else if (key.startsWith(SEQUENCE_PREFIX))
                            {
                                sequenceKey.put(key.substring(SEQUENCE_PREFIX.length()), key);
                            }
                            else if (key.startsWith(NAMED_PREFIX))
                            {
                                namedKey.put(key.substring(NAMED_PREFIX.length()), key);
                            }
                            else if (key.startsWith(SELFNAMED_PREFIX))
                            {
                                selfnamedKey.put(key.substring(SELFNAMED_PREFIX.length()), key);
                            }
                            else if (key.startsWith(REUSABLE_PREFIX))
                            {
                                reusableKey.put(key.substring(REUSABLE_PREFIX.length()), key);
                            }
                            else
                            {
                                log.error("Key with unknown prefix \"" + key + "\" in DSpace configuration file=" + config.toString());
                            }
                        }
                    }
                    continued = line.length() > 0 && line.charAt(line.length()-1) == '\\';
                }
            }
        }
        finally
        {
            if (cr != null)
            {
                try
                {
                    cr.close();
                }
                catch (IOException ioe)
                {
                }
            }

            if (fr != null)
            {
                try
                {
                    fr.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }

        // 1.1 Sanity check, make sure keyMap == set of keys from Configuration
        Enumeration<String> pne = (Enumeration<String>)ConfigurationManager.propertyNames();
        HashSet<String> pn = new HashSet<String>();
        while (pne.hasMoreElements())
        {
            String nk = pne.nextElement();
            if (nk.startsWith("plugin."))
            {
                pn.add(nk);
                if (!keyMap.containsKey(nk))
                {
                    log.error("Key is in ConfigurationManager.propertyNames() but NOT text crawl: \"" + nk + "\"");
                }
            }
        }
        Iterator<String> pi = keyMap.keySet().iterator();
        while (pi.hasNext())
        {
            String key = pi.next();
            if (!pn.contains(key))
            {
                log.error("Key is in text crawl but NOT ConfigurationManager.propertyNames(): \"" + key + "\"");
            }
        }

        // 2. Build up list of all interfaces and test that they are loadable.
        // don't bother testing that they are "interface" rather than "class"
        // since either one will work for the Plugin Manager.
        ArrayList<String> allInterfaces = new ArrayList<String>();
        allInterfaces.addAll(singleKey.keySet());
        allInterfaces.addAll(sequenceKey .keySet());
        allInterfaces.addAll(namedKey.keySet());
        allInterfaces.addAll(selfnamedKey.keySet());
        allInterfaces.addAll(reusableKey.keySet());
        Iterator<String> ii = allInterfaces.iterator();
        while (ii.hasNext())
        {
            checkClassname(ii.next(), "key interface or class");
        }

        // Check implementation classes:
        //  - each class is loadable.
        //  - plugin.selfnamed values are each  subclass of SelfNamedPlugin
        //  - save classname in allImpls
        Map<String, String> allImpls = new HashMap<String, String>();

        // single plugins - just check that it has a valid impl. class
        ii = singleKey.keySet().iterator();
        while (ii.hasNext())
        {
            String key = ii.next();
            String val = ConfigurationManager.getProperty(SINGLE_PREFIX+key);
            if (val == null)
            {
                log.error("Single plugin config not found for: " + SINGLE_PREFIX + key);
            }
            else
            {
                val = val.trim();
                if (checkClassname(val, "implementation class"))
                {
                    allImpls.put(val, val);
                }
            }
        }

        // sequence plugins - all values must be classes
        ii = sequenceKey.keySet().iterator();
        while (ii.hasNext())
        {
            String key = ii.next();
            String val = ConfigurationManager.getProperty(SEQUENCE_PREFIX+key);
            if (val == null)
            {
                log.error("Sequence plugin config not found for: " + SEQUENCE_PREFIX + key);
            }
            else
            {
                val = val.trim();
                String classname[] = val.split("\\s*,\\s*");
                for (int i = 0; i < classname.length; ++i)
                {
                    if (checkClassname(classname[i], "implementation class"))
                    {
                        allImpls.put(classname[i], classname[i]);
                    }
                }
            }
        }

        // 3. self-named plugins - grab and check all values
        //   then make sure it is a subclass of SelfNamedPlugin
        ii = selfnamedKey.keySet().iterator();
        while (ii.hasNext())
        {
            String key = ii.next();
            String val = ConfigurationManager.getProperty(SELFNAMED_PREFIX+key);
            if (val == null)
            {
                log.error("Selfnamed plugin config not found for: " + SELFNAMED_PREFIX + key);
            }
            else
            {
                val = val.trim();
                String classname[] = val.split("\\s*,\\s*");
                for (int i = 0; i < classname.length; ++i)
                {
                    if (checkClassname(classname[i], "selfnamed implementation class"))
                    {
                        allImpls.put(classname[i], classname[i]);
                        checkSelfNamed(classname[i]);
                    }
                }
                checkNames(key);
            }
        }

        // 4. named plugins - extract the classnames and treat same as sequence.
        // use named plugin config mechanism to test for duplicates, unnamed.
        ii = namedKey.keySet().iterator();
        Pattern classnameEqual = Pattern.compile("([\\w\\p{Sc}\\.]+)\\s*\\=");
        while (ii.hasNext())
        {
            String key = ii.next();
            String val = ConfigurationManager.getProperty(NAMED_PREFIX+key);
            if (val == null)
            {
                log.error("Named plugin config not found for: " + NAMED_PREFIX + key);
            }
            else
            {
                checkNames(key);
                val = val.trim();
                Matcher classMatcher = classnameEqual.matcher(val);
                while (classMatcher.find())
                {
                    String classname = classMatcher.group(1);

                    if (checkClassname(classname, "implementation class"))
                    {
                        allImpls.put(classname, classname);
                    }
                }
            }
        }

        // 5. all classes named in Reusable config lines must be other classes.
        Iterator<String> ri = reusableKey.keySet().iterator();
        while (ri.hasNext())
        {
            String rk = ri.next();
            if (!(allImpls.containsKey(rk)))
            {
                log.error("In plugin.reusable configuration, class \"" + rk + "\" is NOT a plugin implementation class.");
            }
        }
    }

    /**
     * Invoking this class from the command line just runs
     * <code>checkConfiguration</code> and shows the results.
     * There are no command-line options.
     */
    public static void main(String[] argv) throws Exception
    {
        checkConfiguration();
    }

}
