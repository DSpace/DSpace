/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * The Legacy Plugin Service is a very simple component container (based on the
 * legacy PluginManager class from 5.x or below). It reads defined "plugins" (interfaces)
 * from config file(s) and makes them available to the API. (TODO: Someday, this
 * entire "plugin" framework needs to be replaced by Spring Beans.)
 * </p>
 * <p>
 * It creates and organizes components (plugins), and helps select a plugin in
 * the cases where there are many possible choices. It also gives some limited
 * control over the lifecycle of a plugin.  It manages three different types
 * (usage patterns) of plugins:
 * </p>
 * <ol>
 *   <li>Singleton Plugin<br>
 *   There is only one implementation class for the plugin.  It is indicated
 *   in the configuration.  This type of plugin chooses an implementations of
 *   a service, for the entire system, at configuration time.  Your
 *   application just fetches the plugin for that interface and gets the
 *   configured-in choice.</li>
 *
 *   <li>Sequence Plugins<br>
 *   You need a sequence or series of plugins, to implement a mechanism like
 *   StackableAuthenticationMethods or a pipeline, where each plugin is
 *   called in order to contribute its implementation of a process to the
 *   whole.</li>
 *   <li>Named Plugins<br>
 *   Use a named plugin when the application has to choose one plugin
 *   implementation out of many available ones.  Each implementation is bound
 *   to one or more names (symbolic identifiers) in the configuration.</li>
 *  </ol>
 *  <p>
 *  The name is just a <code>String</code> to be associated with the
 *  combination of implementation class and interface.  It may contain
 *  any characters except for comma (,) and equals (=).  It may contain
 *  embedded spaces.  Comma is a special character used to separate
 *  names in the configuration entry.
 *  </p>
 *
 * @author Larry Stone
 * @author Tim Donohue (turned old PluginManager into a PluginService)
 * @see SelfNamedPlugin
 */
public class LegacyPluginServiceImpl implements PluginService
{
    /** log4j category */
    private static Logger log = Logger.getLogger(LegacyPluginServiceImpl.class);

    /**
     * Prefixes of names of properties to look for in DSpace Configuration
     */
    private static final String SINGLE_PREFIX = "plugin.single.";
    private static final String SEQUENCE_PREFIX = "plugin.sequence.";
    private static final String NAMED_PREFIX = "plugin.named.";
    private static final String SELFNAMED_PREFIX = "plugin.selfnamed.";

    /** Configuration name of paths to search for third-party plugins. */
    private static final String CLASSPATH = "plugin.classpath";

    // Separator character (from perl $;) to make "two dimensional"
    // hashtable key out of interface classname and plugin name;
    // this character separates the words.
    private static final String SEP = "\034";

    /** Paths to search for third-party plugins. */
    private String[] classPath;

    /** Custom class loader to search for third-party plugins. */
    private PathsClassLoader loader;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    protected LegacyPluginServiceImpl() {
    }

    /**
     * Initialize the bean (after dependency injection has already taken place).
     * Ensures the configurationService is injected, so that we can load
     * plugin classpath info from config.
     * Called by "init-method" in Spring config.
     */
    void init()
    {
        String path = configurationService.getProperty(CLASSPATH);
        if (null == path)
            classPath = new String[0];
        else
            classPath = path.split(":");

        loader = new PathsClassLoader(LegacyPluginServiceImpl.class.getClassLoader(), classPath);
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
     * @throws PluginConfigurationError if no matching singleton plugin is configured.
     */
    @Override
    public Object getSinglePlugin(Class interfaceClass)
        throws PluginConfigurationError, PluginInstantiationException
    {
        String iname = interfaceClass.getName();

        // NOTE: module name is ignored, as single plugins ALWAYS begin with SINGLE_PREFIX
        String key = SINGLE_PREFIX+iname;
        // configuration format is  prefix.<interface> = <classname>
        String classname = configurationService.getProperty(key);

        if (classname != null)
        {
            return getAnonymousPlugin(classname.trim());
        }
        else
        {
            throw new PluginConfigurationError("No Single Plugin configured for interface \""+iname+"\"");
        }
    }

    /**
     * Returns instances of all plugins that implement the interface,
     * in an Array.  Returns an empty array if no there are no
     * matching plugins.
     * <p>
     * The order of the plugins in the array is the same as their class
     * names in the configuration's value field.
     *
     * @param interfaceClass interface for which to find plugins.
     * @return an array of plugin instances; if none are
     *   available an empty array is returned.
     */
    @Override
    public Object[] getPluginSequence(Class interfaceClass)
        throws PluginInstantiationException
    {
        // cache of config data for Sequence Plugins; format its
        // <interface-name> -> [ <classname>.. ]  (value is Array)
        Map<String, String[]> sequenceConfig = new HashMap<String, String[]>();

        // cache the configuration for this interface after grovelling it once:
        // format is  prefix.<interface> = <classname>
        String iname = interfaceClass.getName();
        String[] classname = null;
        if (!sequenceConfig.containsKey(iname))
        {
            // NOTE: module name is ignored, as sequence plugins ALWAYS begin with SEQUENCE_PREFIX
            String key = SEQUENCE_PREFIX+iname;

            classname = configurationService.getArrayProperty(key);
            if (classname == null || classname.length==0)
            {
                log.warn("No Configuration entry found for Sequence Plugin interface="+iname);
                return (Object[]) Array.newInstance(interfaceClass, 0);
            }
            sequenceConfig.put(iname, classname);
        }
        else
        {
            classname = sequenceConfig.get(iname);
        }

        Object result[] = (Object[])Array.newInstance(interfaceClass, classname.length);
        for (int i = 0; i < classname.length; ++i)
        {
            log.debug("Adding Sequence plugin for interface= "+iname+", class="+classname[i]);
            result[i] = getAnonymousPlugin(classname[i]);
        }
        return result;
    }

    // Get possibly-cached plugin instance for un-named plugin,
    // this is shared by Single and Sequence plugins.
    private Object getAnonymousPlugin(String classname)
        throws PluginInstantiationException
    {
        try
        {
            Class pluginClass = Class.forName(classname, true, loader);
            return pluginClass.newInstance();
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginInstantiationException("Cannot load plugin class: " +
                                                   e.toString(), e);
        }
        catch (InstantiationException|IllegalAccessException e)
        {
            throw new PluginInstantiationException(e);
        }
    }

    // Map of named plugin classes, [intfc,name] -> class
    // Also contains intfc -> "marker" to mark when interface has been loaded.
    private Map<String, String> namedPluginClasses = new HashMap<String, String>();

    // Map of cached (reusable) named plugin instances, [class,name] -> instance
    private Map<Serializable, Object> namedInstanceCache = new HashMap<Serializable, Object>();

    // load and cache configuration data for the given interface.
    private void configureNamedPlugin(String iname)
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
            //    plugin.named.<INTF> = <CLASS> = <name>\, <name> [,] \
            //                        <CLASS> = <name>\, <name> [ ... ]
            // NOTE: module name is ignored, as named plugins ALWAYS begin with NAMED_PREFIX
            String key = NAMED_PREFIX+iname;
            String[] namedVals = configurationService.getArrayProperty(key);
            if (namedVals != null && namedVals.length>0)
            {
                String prevClassName = null;
                for(String namedVal : namedVals)
                {
                    String[] valSplit = namedVal.trim().split("\\s*=\\s*");

                    String className = null;
                    String name = null;
                    
                    // If there's no "=" separator in this value, assume it's
                    // just a "name" that belongs with previous class.
                    // (This may occur if there's an unescaped comma between names)
                    if (prevClassName!=null && valSplit.length==1)
                    {
                        className = prevClassName;
                        name = valSplit[0];
                    }
                    else
                    {
                        // first part is class name
                        className = valSplit[0];
                        prevClassName = className;
                        // second part is one or more names
                        name = valSplit[1];
                    }

                    // The name may be *multiple* names (separated by escaped commas: \,)
                    String[] names = name.trim().split("\\s*,\\s*");

                    found += installNamedConfigs(iname, className, names);
                }
            }

            // 2. Get Self-named config entries:
            // format is plugin.selfnamed.<INTF> = <CLASS> , <CLASS> ..
            // NOTE: module name is ignored, as self-named plugins ALWAYS begin with SELFNAMED_PREFIX
            key = SELFNAMED_PREFIX+iname;
            String[] selfNamedVals = configurationService.getArrayProperty(key);
            if (selfNamedVals != null && selfNamedVals.length>0)
            {
                for (String classname : selfNamedVals)
                {
                    try
                    {
                        Class pluginClass = Class.forName(classname, true, loader);
                        String names[] = (String[])pluginClass.getMethod("getPluginNames").
                                                   invoke(null);
                        if (names == null || names.length == 0)
                        {
                            log.error("Self-named plugin class \"" + classname + "\" returned null or empty name list!");
                        }
                        else
                        {
                            found += installNamedConfigs(iname, classname, names);
                        }
                    }
                    catch (NoSuchMethodException e)
                    {
                        log.error("Implementation Class \""+classname+"\" is not a subclass of SelfNamedPlugin, it has no getPluginNames() method.");
                    }
                    catch (Exception e)
                    {
                        log.error("Error while configuring self-named plugin", e);
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
    private int installNamedConfigs(String iname, String classname, String names[])
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
     * and is bound to a name matching name.  If there is no
     * matching plugin, it returns null.  The names are matched by
     * String.equals().
     *
     * @param interfaceClass the interface class of the plugin
     * @param name under which the plugin implementation is configured.
     * @return instance of plugin implementation, or null if there is no match or an error.
     */
    @Override
    public Object getNamedPlugin(Class interfaceClass, String name)
         throws PluginInstantiationException
    {
        try
        {
            String iname = interfaceClass.getName();
            configureNamedPlugin(iname);
            String key = iname + SEP + name;
            String cname = namedPluginClasses.get(key);
            if (cname == null)
            {
                log.warn("Cannot find named plugin for interface=" + iname + ", name=\"" + name + "\"");
            }
            else
            {
                Class pluginClass = Class.forName(cname, true, loader);
                log.debug("Creating instance of: " + cname +
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
        catch (ClassNotFoundException e)
        {
            throw new PluginInstantiationException("Cannot load plugin class: " +
                                                   e.toString(), e);
        }
        catch (InstantiationException|IllegalAccessException e)
        {
            throw new PluginInstantiationException(e);
        }

        return null;
    }

    /**
     * Returns whether a plugin exists which implements the specified interface
     * and has a specified name. If a matching plugin is found to be configured,
     * return true. If there is no matching plugin, return false.
     *
     * @param interfaceClass the interface class of the plugin
     * @param name under which the plugin implementation is configured.
     * @return true if plugin was found to be configured, false otherwise
     */
    @Override
    public boolean hasNamedPlugin(Class interfaceClass, String name)
         throws PluginInstantiationException
    {
        try
        {
            String iname = interfaceClass.getName();
            configureNamedPlugin(iname);
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
     * the interface can be requested (with getNamedPlugin()).
     * The array is empty if there are no matches.  Use this to populate
     * a menu of plugins for interactive selection, or to document what
     * the possible choices are.
     * <p>
     * NOTE: The names are NOT returned in any deterministic order.
     *
     * @param interfaceClass plugin interface for which to return names.
     * @return an array of strings with every name; if none are
     *   available an empty array is returned.
     */
    @Override
    public String[] getAllPluginNames(Class interfaceClass)
    {
        try
        {
            String iname = interfaceClass.getName();
            configureNamedPlugin(iname);
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

    /* -----------------------------------------------------------------
     *  Code to check configuration is all below this line
     * -----------------------------------------------------------------
     */

    // true if classname is valid and loadable.
    private boolean checkClassname(String iname, String msg)
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
    private boolean checkSelfNamed(String iname)
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
    private boolean checkSelfNamed(Class cls)
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
    private void checkNames(String iname)
    {
        try
        {
            configureNamedPlugin(iname);
        }
        catch (ClassNotFoundException ce)
        {
            // bogus classname should be old news by now.
        }
    }

    /**
     * Validate the entries in the DSpace Configuration relevant to
 LegacyPluginServiceImpl.  Look for inconsistencies, illegal syntax, etc.
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
     * @throws IOException if IO error
     */
    public void checkConfiguration()
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

        // Find all property keys starting with "plugin."
        List<String> keys = configurationService.getPropertyKeys("plugin.");

        for(String key : keys)
        {
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
            else
            {
                log.error("Key with unknown prefix \"" + key + "\" in DSpace configuration");
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
            String val = configurationService.getProperty(SINGLE_PREFIX+key);
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
            String[] vals = configurationService.getArrayProperty(SEQUENCE_PREFIX+key);
            if (vals == null || vals.length==0)
            {
                log.error("Sequence plugin config not found for: " + SEQUENCE_PREFIX + key);
            }
            else
            {
                for (String val : vals)
                {
                    if (checkClassname(val, "implementation class"))
                    {
                        allImpls.put(val, val);
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
            String[] vals = configurationService.getArrayProperty(SELFNAMED_PREFIX+key);
            if (vals == null || vals.length==0)
            {
                log.error("Selfnamed plugin config not found for: " + SELFNAMED_PREFIX + key);
            }
            else
            {
                for (String val : vals)
                {
                    if (checkClassname(val, "selfnamed implementation class"))
                    {
                        allImpls.put(val, val);
                        checkSelfNamed(val);
                    }
                }
                checkNames(key);
            }
        }

        // 4. named plugins - extract the classnames and treat same as sequence.
        // use named plugin config mechanism to test for duplicates, unnamed.
        ii = namedKey.keySet().iterator();
        while (ii.hasNext())
        {
            String key = ii.next();
            String[] vals = configurationService.getArrayProperty(NAMED_PREFIX+key);
            if (vals == null || vals.length==0)
            {
                log.error("Named plugin config not found for: " + NAMED_PREFIX + key);
            }
            else
            {
                checkNames(key);
                for (String val : vals)
                {
                    // each named plugin has two parts to the value, format:
                    // [classname] = [plugin-name]
                    String val_split[] = val.split("\\s*=\\s*");
                    String classname = val_split[0];
                    if (checkClassname(classname, "implementation class"))
                    {
                        allImpls.put(classname, classname);
                    }
                }
            }
        }
    }

    /**
     * Invoking this class from the command line just runs
     * <code>checkConfiguration</code> and shows the results.
     * There are no command-line options.
     *
     * @param argv the command line arguments given
     * @throws Exception if error
     */
    public void main(String[] argv) throws Exception
    {
        checkConfiguration();
    }

}
