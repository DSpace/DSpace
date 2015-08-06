/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Class for reading the DSpace system configuration. The main configuration is
 * read in as properties from a standard properties file. Email templates and
 * configuration files for other tools are also be accessed via this class.
 * <P>
 * The main configuration is by default read from the <em>resource</em>
 * <code>/dspace.cfg</code>.
 * To specify a different configuration, the system property
 * <code>dspace.configuration</code> should be set to the <em>filename</em>
 * of the configuration file.
 * <P>
 * Other configuration files are read from the <code>config</code> directory
 * of the DSpace installation directory (specified as the property
 * <code>dspace.dir</code> in the main configuration file.)
 *
 *
 * @author Robert Tansley
 * @author Larry Stone - Interpolated values.
 * @author Mark Diggory - General Improvements to detection, logging and loading.
 * @version $Revision$
 */
public class ConfigurationManager
{
    /** log4j category */
    private static Logger log = Logger.getLogger(ConfigurationManager.class);

    /** The configuration properties */
    private static Properties properties = null;

    /** module configuration properties */
    private static Map<String, Properties> moduleProps = new HashMap<String, Properties>();

    // limit of recursive depth of property variable interpolation in
    // configuration; anything greater than this is very likely to be a loop.
    private static final int RECURSION_LIMIT = 9;

    protected ConfigurationManager()
    {

    }

    /**
     * Identify if DSpace is properly configured
     * @return boolean true if configured, false otherwise
     */
    public static boolean isConfigured()
    {
        return properties != null;
    }

    public static boolean isConfigured(String module)
    {
        return moduleProps.get(module) != null;
    }

    /**
     * REMOVED - Flushing the properties could be dangerous in the current DSpace state
     * Need to consider how it will affect in-flight processes
     *
     * Discard all current properties - will force a reload from disk when
     * any properties are requested.
     */
//    public static void flush()
//    {
//        properties = null;
//    }

    /**
     * REMOVED - Flushing the properties could be dangerous in the current DSpace state
     * Need to consider how it will affect in-flight processes
     *
     * Discard properties for a module -  will force a reload from disk
     * when any of module's properties are requested
     *
     * @param module the module name
     */
//    public static void flush(String module)
//    {
//        moduleProps.remove(module);
//    }

    /**
     * Returns all properties in main configuration
     *
     * @return properties - all non-modular properties
     */
    public static Properties getProperties()
    {
        Properties props = getMutableProperties();
        return props == null ? null : (Properties)props.clone();
    }

    private static Properties getMutableProperties()
    {
        if (properties == null)
        {
            loadConfig(null);
        }

        return properties;
    }

    /**
     * Returns all properties for a given module
     *
     * @param module
     *        the name of the module
     * @return properties - all module's properties
     */
    public static Properties getProperties(String module)
    {
        Properties props = getMutableProperties(module);
        return props == null ? null : (Properties)props.clone();
    }

    private static Properties getMutableProperties(String module)
    {
        if (module == null)
            return properties;

        if (moduleProps == null)
        {
            loadConfig(null);
        }
        Properties retProps = moduleProps.get(module);
        if (retProps == null)
        {
            loadModuleConfig(module);
            retProps = moduleProps.get(module);
        }

        return retProps;
    }

    /**
     * Get a configuration property
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property, or <code>null</code> if the property
     *         does not exist.
     */
    public static String getProperty(String property)
    {
        Properties props = getMutableProperties();
        String value = props == null ? null : props.getProperty(property);
        return (value != null) ? value.trim() : null;
    }

    /**
     * Get a module configuration property value.
     *
     * @param module
     *      the name of the module, or <code>null</code> for regular configuration
     *      property
     * @param property
     *      the name (key) of the property
     * @return
     *      the value of the property, or <code>null</code> if the
     *      property does not exist
     */
    public static String getProperty(String module, String property)
    {
        if (module == null)
        {
            return getProperty(property);
        }

        String value = null;
        Properties modProps = getMutableProperties(module);

        if (modProps != null)
        {
            value = modProps.getProperty(property);
        }

        if (value == null)
        {
            // look in regular properties with module name prepended
            value = getProperty(module + "." + property);
        }

        return (value != null) ? value.trim() : null;
    }

    /**
     * Get a configuration property as an integer
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static int getIntProperty(String property)
    {
        return getIntProperty(property, 0);
    }

    /**
     * Get a module configuration property as an integer
     *
     * @param module
     *         the name of the module
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static int getIntProperty(String module, String property)
    {
        return getIntProperty(module, property, 0);
    }

    /**
     * Get a configuration property as an integer, with default
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not an Integer.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static int getIntProperty(String property, int defaultValue)
    {
        return getIntProperty(null, property, defaultValue);
    }

    /**
     * Get a module configuration property as an integer, with default
     *
     * @param module
     *         the name of the module
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not an Integer.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static int getIntProperty(String module, String property, int defaultValue)
    {
       String stringValue = getProperty(module, property);
       int intValue = defaultValue;

       if (stringValue != null)
       {
           try
           {
               intValue = Integer.parseInt(stringValue.trim());
           }
           catch (NumberFormatException e)
           {
               warn("Warning: Number format error in property: " + property);
           }
        }

        return intValue;
    }

    /**
     * Get a configuration property as a long
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static long getLongProperty(String property)
    {
        return getLongProperty(property, 0);
    }

    /**
     * Get a module configuration property as a long
     *
     * @param module
     *         the name of the module
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static long getLongProperty(String module, String property)
    {
        return getLongProperty(module, property, 0);
    }

   /**
     * Get a configuration property as an long, with default
     *
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not a Long.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static long getLongProperty(String property, int defaultValue)
    {
        return getLongProperty(null, property, defaultValue);
    }

    /**
     * Get a configuration property as an long, with default
     *
     * @param module  the module, or <code>null</code> for regular property
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not a Long.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static long getLongProperty(String module, String property, int defaultValue)
    {
        String stringValue = getProperty(module, property);
        long longValue = defaultValue;

        if (stringValue != null)
        {
            try
            {
                longValue = Long.parseLong(stringValue.trim());
            }
            catch (NumberFormatException e)
            {
                warn("Warning: Number format error in property: " + property);
            }
        }

        return longValue;
    }

    /**
     * Get a configuration property as a boolean. True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>false</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property)
    {
        return getBooleanProperty(property, false);
    }

    /**
     * Get a module configuration property as a boolean. True is indicated if
     * the value of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param module the module, or <code>null</code> for regular property
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>false</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String module, String property)
    {
        return getBooleanProperty(module, property, false);
    }

   /**
     * Get a configuration property as a boolean, with default.
     * True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property, boolean defaultValue)
    {
        return getBooleanProperty(null, property, defaultValue);
    }

    /**
     * Get a module configuration property as a boolean, with default.
     * True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param module     module, or <code>null</code> for regular property
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String module, String property, boolean defaultValue)
    {
        String stringValue = getProperty(module, property);

        if (stringValue != null)
        {
        	stringValue = stringValue.trim();
            return  stringValue.equalsIgnoreCase("true") ||
                    stringValue.equalsIgnoreCase("yes");
        }
        else
        {
            return defaultValue;
        }
    }

    /**
     * Returns an enumeration of all the keys in the DSpace configuration
     *
     * @return an enumeration of all the keys in the DSpace configuration
     */
    public static Enumeration<?> propertyNames()
    {
        return propertyNames(null);
    }

    /**
     * Returns an enumeration of all the keys in a module configuration
     *
     * @param  module    module, or <code>null</code> for regular property
     *
     * @return an enumeration of all the keys in the module configuration,
     *         or <code>null</code> if the module does not exist.
     */
    public static Enumeration<?> propertyNames(String module)
    {
        Properties props = getProperties(module);
        return props == null ? null : props.propertyNames();
    }

    /** The configuration that was loaded. */
    private static File loadedFile = null;

    /**
     * Return the file that configuration was actually loaded from.
     *
     * @deprecated Please remove all direct usage of the configuration file.
     * @return File naming configuration data file.
     */
    protected static File getConfigurationFile()
    {
        // in case it hasn't been done yet.
        if (loadedFile == null)
        {
            loadConfig(null);
        }

        return loadedFile;
    }

    private static synchronized void loadModuleConfig(String module)
    {
        // try to find it in modules
        File modFile = null;
        try
        {
            modFile = new File(getProperty("dspace.dir") +
                                File.separator + "config" +
                                File.separator + "modules" +
                                File.separator + module + ".cfg");

            if (modFile.exists())
            {
                Properties modProps = new Properties();
                InputStream modIS = null;
                InputStreamReader modIR = null;
                try
                {
                    modIS = new FileInputStream(modFile);
                    modIR = new InputStreamReader(modIS, "UTF-8");
                    modProps.load(modIR);
                }
                finally
                {
                    if (modIR != null)
                    {
                        modIR.close();
                    }

                    if (modIS != null)
                    {
                        modIS.close();
                    }
                }

                for (Enumeration pe = modProps.propertyNames(); pe.hasMoreElements(); )
                {
                    String key = (String)pe.nextElement();
                    String ival = interpolate(key, modProps.getProperty(key), 1);
                    if (ival != null)
                    {
                        modProps.setProperty(key, ival);
                    }
                }
                moduleProps.put(module, modProps);
            }
            else
            {
                // log invalid request
                warn("Requested configuration module: " + module + " not found");
            }
        }
        catch (IOException ioE)
        {
            fatal("Can't load configuration: " + (modFile == null ? "<unknown>" : modFile.getAbsolutePath()), ioE);
        }
    }

    /**
     * Load the DSpace configuration properties. Only does anything if
     * properties are not already loaded. Properties are loaded in from the
     * specified file, or default locations.
     *
     * @param configFile
     *            The <code>dspace.cfg</code> configuration file to use, or
     *            <code>null</code> to try default locations
     */
    public static synchronized void loadConfig(String configFile)
    {
        if (properties != null)
        {
            return;
        }

        URL url = null;

        InputStream is = null;
        InputStreamReader reader = null;
        try
        {
            String configProperty = null;
            try
            {
                configProperty = System.getProperty("dspace.configuration");
            }
            catch (SecurityException se)
            {
                // A security manager may stop us from accessing the system properties.
                // This isn't really a fatal error though, so catch and ignore
                log.warn("Unable to access system properties, ignoring.", se);
            }

            // should only occur after a flush()
            if (loadedFile != null)
            {
                info("Reloading current config file: " + loadedFile.getAbsolutePath());

                url = loadedFile.toURI().toURL();
            }
            else if (configFile != null)
            {
                info("Loading provided config file: " + configFile);

                loadedFile = new File(configFile);
                url = loadedFile.toURI().toURL();

            }
            // Has the default configuration location been overridden?
            else if (configProperty != null)
            {
                info("Loading system provided config property (-Ddspace.configuration): " + configProperty);

                // Load the overriding configuration
                loadedFile = new File(configProperty);
                url = loadedFile.toURI().toURL();
            }
            // Load configuration from default location
            else
            {
                url = ConfigurationManager.class.getResource("/dspace.cfg");
                if (url != null)
                {
                    info("Loading from classloader: " + url);

                    loadedFile = new File(url.getPath());
                }
            }

            if (url == null)
            {
                fatal("Cannot find dspace.cfg");
                throw new IllegalStateException("Cannot find dspace.cfg");
            }
            else
            {
                properties = new Properties();
                is = url.openStream();
                reader = new InputStreamReader(is, "UTF-8");
                properties.load(reader);

                // walk values, interpolating any embedded references.
                for (Enumeration<?> pe = properties.propertyNames(); pe.hasMoreElements(); )
                {
                    String key = (String)pe.nextElement();
                    String value = interpolate(key, properties.getProperty(key), 1);
                    if (value != null)
                    {
                        properties.setProperty(key, value);
                    }
                }
            }

        }
        catch (IOException e)
        {
            fatal("Can't load configuration: " + url, e);

            // FIXME: Maybe something more graceful here, but without a
            // configuration we can't do anything.
            throw new IllegalStateException("Cannot load configuration: " + url, e);
        }
        finally
        {
            if (reader != null)
            {
                try {
                    reader.close();
                }
                catch (IOException ioe)
                {
                }
            }
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }

        try
        {
            /*
             * Initialize Logging once ConfigurationManager is initialized.
             *
             * This is controlled by a property in dspace.cfg.  If the property
             * is absent then nothing will be configured and the application
             * will use the defaults provided by log4j.
             *
             * Property format is:
             *
             * log.init.config = ${dspace.dir}/config/log4j.properties
             * or
             * log.init.config = ${dspace.dir}/config/log4j.xml
             *
             * See default log4j initialization documentation here:
             * http://logging.apache.org/log4j/docs/manual.html
             *
             * If there is a problem with the file referred to in
             * "log.configuration", it needs to be sent to System.err
             * so do not instantiate another Logging configuration.
             *
             */
            String dsLogConfiguration = ConfigurationManager.getProperty("log.init.config");

            if (dsLogConfiguration == null || System.getProperty("dspace.log.init.disable") != null)
            {
                /*
                 * Do nothing if log config not set in dspace.cfg or "dspace.log.init.disable"
                 * system property set.  Leave it upto log4j to properly init its logging
                 * via classpath or system properties.
                 */
                info("Using default log4j provided log configuration." +
                        "  If unintended, check your dspace.cfg for (log.init.config)");
            }
            else
            {
                info("Using dspace provided log configuration (log.init.config)");


                File logConfigFile = new File(dsLogConfiguration);

                if(logConfigFile.exists())
                {
                    info("Loading: " + dsLogConfiguration);

                    OptionConverter.selectAndConfigure(logConfigFile.toURI()
                            .toURL(), null, org.apache.log4j.LogManager
                            .getLoggerRepository());
                }
                else
                {
                    info("File does not exist: " + dsLogConfiguration);
                }
            }

        }
        catch (MalformedURLException e)
        {
            fatal("Can't load dspace provided log4j configuration", e);
            throw new IllegalStateException("Cannot load dspace provided log4j configuration",e);
        }

    }

    /**
     * Wrapper for {@link NewsManager#getNewsFilePath()}.
     * @deprecated since 4.0
     */
    public static String getNewsFilePath()
    {
        return NewsManager.getNewsFilePath();
    }

    /**
     * Wrapper for {@link NewsManager#readNewsFile(String)}.
     * @deprecated since 4.0
     */
    public static String readNewsFile(String name)
    {
        return NewsManager.readNewsFile(name);
    }

    /**
     * Wrapper for {@link NewsManager#writeNewsFile(String, String)}.
     * @deprecated since 4.0
     */
    public static String writeNewsFile(String file, String news)
    {
        return NewsManager.writeNewsFile(file, news);
    }

    /**
     * Wrapper for {@link LicenseManager#getLicenseText(String)}.
     * @deprecated since 4.0
     */
    public static String getLicenseText(String licenseFile)
    {
        return LicenseManager.getLicenseText(licenseFile);
    }

    /**
     * Wrapper for {@link LicenseManager#getDefaultSubmissionLicense()}.
     * @deprecated since 4.0
     */
    public static String getDefaultSubmissionLicense()
    {
        return LicenseManager.getDefaultSubmissionLicense();
    }

    /**
     * Wrapper for {@link LicenseManager#writeLicenseFile(String, String)}.
     * @deprecated since 4.0
     */
    public static void writeLicenseFile(String licenseFile, String newLicense)
    {
        LicenseManager.writeLicenseFile(licenseFile, newLicense);
    }

    /**
     * Recursively interpolate variable references in value of
     * property named "key".
     * @return new value if it contains interpolations, or null
     *   if it had no variable references.
     */
    private static String interpolate(String key, String value, int level)
    {
        if (level > RECURSION_LIMIT)
        {
            throw new IllegalArgumentException("ConfigurationManager: Too many levels of recursion in configuration property variable interpolation, property=" + key);
        }
        //String value = (String)properties.get(key);
        int from = 0;
        StringBuffer result = null;
        while (from < value.length())
        {
            int start = value.indexOf("${", from);
            if (start >= 0)
            {
                int end = value.indexOf('}', start);
                if (end < 0)
                {
                    break;
                }
                String var = value.substring(start+2, end);
                if (result == null)
                {
                    result = new StringBuffer(value.substring(from, start));
                }
                else
                {
                    result.append(value.substring(from, start));
                }
                if (properties.containsKey(var))
                {
                    String ivalue = interpolate(var, properties.getProperty(var), level+1);
                    if (ivalue != null)
                    {
                        result.append(ivalue);
                        properties.setProperty(var, ivalue);
                    }
                    else
                    {
                        result.append(((String)properties.getProperty(var)).trim());
                    }
                }
                else
                {
                    log.warn("Interpolation failed in value of property \""+key+
                             "\", there is no property named \""+var+"\"");
                }
                from = end+1;
            }
            else
            {
                break;
            }
        }
        if (result != null && from < value.length())
        {
            result.append(value.substring(from));
        }
        return (result == null) ? null : result.toString();
    }

    /**
     * Command-line interface for running configuration tasks. Possible
     * arguments:
     * <ul>
     * <li><code>-property name</code> prints the value of the property
     * <code>name</code> from <code>dspace.cfg</code> to the standard
     * output. If the property does not exist, nothing is written.</li>
     * </ul>
     *
     * @param argv
     *            command-line arguments
     */
    public static void main(String[] argv)
    {
        if ((argv.length == 2) && argv[0].equals("-property"))
        {
            String val = getProperty(argv[1]);

            if (val != null)
            {
                System.out.println(val);
            }
            else
            {
                System.out.println("");
            }

            System.exit(0);
        }
        else if ((argv.length == 4) && argv[0].equals("-module") &&
                                        argv[2].equals("-property"))
        {
            String val = getProperty(argv[1], argv[3]);

            if (val != null)
            {
                System.out.println(val);
            }
            else
            {
                System.out.println("");
            }

            System.exit(0);
        }
        else
        {
            System.err
                    .println("Usage: ConfigurationManager OPTION\n  [-module mod.name] -property prop.name  get value of prop.name from module or dspace.cfg");
        }

        System.exit(1);
    }

    private static void info(String string)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("INFO: " + string);
        }
        else
        {
            log.info(string);
        }
    }

    private static void warn(String string)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("WARN: " + string);
        }
        else
        {
            log.warn(string);
        }
    }

    private static void fatal(String string, Exception e)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("FATAL: " + string);
            e.printStackTrace();
        }
        else
        {
            log.fatal(string, e);
        }
    }

    private static void fatal(String string)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("FATAL: " + string);
        }
        else
        {
            log.fatal(string);
        }
    }

    /*
     * Only current solution available to detect
     * if log4j is truly configured.
     */
    private static boolean isLog4jConfigured()
    {
        Enumeration<?> en = org.apache.log4j.LogManager.getRootLogger()
                .getAllAppenders();

        if (!(en instanceof org.apache.log4j.helpers.NullEnumeration))
        {
            return true;
        }
        else
        {
            Enumeration<?> cats = Category.getCurrentCategories();
            while (cats.hasMoreElements())
            {
                Category c = (Category) cats.nextElement();
                if (!(c.getAllAppenders() instanceof org.apache.log4j.helpers.NullEnumeration))
                {
                    return true;
                }
            }
        }
        return false;
    }

}
