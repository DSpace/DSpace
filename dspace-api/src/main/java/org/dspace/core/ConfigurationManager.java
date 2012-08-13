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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.dspace.content.configuration.DSpacePropertiesConfiguration;

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
    private static DSpacePropertiesConfiguration properties = null;
    
    /** module configuration properties */
    private static Map<String, DSpacePropertiesConfiguration> moduleProps = new HashMap<String, DSpacePropertiesConfiguration>();

    /** The default license */
    private static String license;

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
    public static DSpacePropertiesConfiguration getProperties()
    {
    	return getMutableProperties();
    }

    private static DSpacePropertiesConfiguration getMutableProperties()
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
    public static DSpacePropertiesConfiguration getProperties(String module)
    {
    	return getMutableProperties(module);
    }

    private static DSpacePropertiesConfiguration getMutableProperties(String module)
    {
        if (module == null)
            return properties;

        DSpacePropertiesConfiguration retProps = moduleProps.get(module);
        if (retProps == null)
        {
            loadModuleConfig(module);
            retProps = moduleProps.get(module);
        }

        return retProps;
    }
    

    /**
     * Returns a String from DSpace configuration
     * 
     * @param key Property Key
     * @param defaultValue Return value if undefined
     * @return Returns a String from configuration
     */
    public static String getString (String key, String defaultValue) {
    	DSpacePropertiesConfiguration props = getMutableProperties();
        String value = props == null ? null : props.getString(key, defaultValue);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Returns a List of Strings from DSpace configuration
     * 
     * @param key Property Key
     * @return Returns a List of Strings from configuration
     */
    public static List<String> getList (String key) {
    	DSpacePropertiesConfiguration props = getMutableProperties();
        List<String> value = null;
		if (props != null)
			value = props.getList(key);
        
		return (value != null) ? value : new ArrayList<String>();
    }
    /**
     * Returns a List of Strings from DSpace configuration
     * 
     * @param key Property Key
     * @return Returns a List of Strings from configuration
     */
    public static List<String> getList (String module, String key) {
    	if (module == null)
        {
            return getList(key);
        }
        
    	List<String> value = null;
        DSpacePropertiesConfiguration modProps = getMutableProperties(module);
        
        if (modProps != null && modProps.containsKey(key))
        {
        	value = modProps.getList(key);
        } else {
        	value = getList(module + "." + key);
        }

        return value;
    }

    /**
     * Returns a String from DSpace configuration
     * 
     * @param module Module name
     * @param key Property Key
     * @param defaultValue Return value if undefined
     * @return Returns a String from configuration
     */
    public static String getString (String module, String key, String defaultValue) {
    	if (module == null)
        {
            return getString(key, defaultValue);
        }
        
        String value = null;
        DSpacePropertiesConfiguration modProps = getMutableProperties(module);
        
        if (modProps != null)
        {
            value = modProps.getString(key, null);
        }

        if (value == null)
        {
            // look in regular properties with module name prepended
            value = getString(module + "." + key, defaultValue);
        }

        return (value != null) ? value : defaultValue;
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
    	DSpacePropertiesConfiguration props = getMutableProperties();
        String value = props == null ? null : props.getString(property);
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
        DSpacePropertiesConfiguration modProps = getMutableProperties(module);

        if (modProps != null)
        {
            value = modProps.getString(property);
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
     * Get the License
     * 
     * @param
     *         licenseFile   file name
     *  
     *  @return
     *         license text
     * 
     */
    public static String getLicenseText(String licenseFile)
    {
    // Load in default license

        FileReader fr = null;
        BufferedReader br = null;
        try
        {
            fr = new FileReader(licenseFile);
            br = new BufferedReader(fr);
            String lineIn;
            license = "";
            while ((lineIn = br.readLine()) != null)
            {
                license = license + lineIn + '\n';
            }
        }
        catch (IOException e)
        {
            fatal("Can't load configuration", e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new IllegalStateException("Failed to read default license.", e);
        }
        finally
        {
            if (br != null)
            {
                try
                { 
                    br.close();
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

        return license;
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
    	DSpacePropertiesConfiguration props = getProperties(module);
        return (Enumeration<?>) (props == null ? null : props.getKeys());
    }

    /**
     * Get the template for an email message. The message is suitable for
     * inserting values using <code>java.text.MessageFormat</code>.
     * 
     * @param emailFile
     *            full name for the email template, for example "/dspace/config/emails/register".
     * 
     * @return the email object, with the content and subject filled out from
     *         the template
     * 
     * @throws IOException
     *             if the template couldn't be found, or there was some other
     *             error reading the template
     */
    public static Email getEmail(String emailFile) throws IOException
    {
        String charset = null;
        String subject = "";
        StringBuffer contentBuffer = new StringBuffer();

        // Read in template
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(emailFile));

            boolean more = true;

            while (more)
            {
                String line = reader.readLine();

                if (line == null)
                {
                    more = false;
                }
                else if (line.toLowerCase().startsWith("subject:"))
                {
                    // Extract the first subject line - everything to the right
                    // of the colon, trimmed of whitespace
                    subject = line.substring(8).trim();
                }
                else if (line.toLowerCase().startsWith("charset:"))
                {
                    // Extract the character set from the email
                    charset = line.substring(8).trim();
                }
                else if (!line.startsWith("#"))
                {
                    // Add non-comment lines to the content
                    contentBuffer.append(line);
                    contentBuffer.append("\n");
                }
            }
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }
        // Create an email
        Email email = new Email();
        email.setSubject(subject);
        email.setContent(contentBuffer.toString());

        if (charset != null)
        {
            email.setCharset(charset);
        }

        return email;
    }

    /**
     * Get the site-wide default license that submitters need to grant
     * 
     * @return the default license
     */
    public static String getDefaultSubmissionLicense()
    {
        if (properties == null)
        {
            loadConfig(null);
        }

        return license;
    }

    /**
     * Get the path for the news files.
     * 
     */
    public static String getNewsFilePath()
    {
        String filePath = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator;

        return filePath;
    }

    /**
     * Reads news from a text file.
     * 
     * @param newsFile
     *        name of the news file to read in, relative to the news file path.
     */
    public static String readNewsFile(String newsFile)
    {
        String fileName = getNewsFilePath();
        
        fileName += newsFile;
        
        StringBuilder text = new StringBuilder();

        try
        {
            // retrieve existing news from file
            FileInputStream fir = new FileInputStream(fileName);
            InputStreamReader ir = new InputStreamReader(fir, "UTF-8");
            BufferedReader br = new BufferedReader(ir);

            String lineIn;

            while ((lineIn = br.readLine()) != null)
            {
                text.append(lineIn);
            }

            br.close();
        }
        catch (IOException e)
        {
            warn("news_read: " + e.getLocalizedMessage());
        }

        return text.toString();
    }

    /**
     * Writes news to a text file.
     * 
     * @param newsFile
     *        name of the news file to read in, relative to the news file path.
     * @param news
     *            the text to be written to the file.
     */
    public static String writeNewsFile(String newsFile, String news)
    {
        String fileName = getNewsFilePath();

        fileName += newsFile;

        try
        {
            // write the news out to the appropriate file
            FileOutputStream fos = new FileOutputStream(fileName);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.print(news);
            out.close();
        }
        catch (IOException e)
        {
            warn("news_write: " + e.getLocalizedMessage());
        }

        return news;
    }

    /**
     * Writes license to a text file.
     * 
     * @param licenseFile
     *            name for the file int which license will be written, 
     *            relative to the current directory.
     */
    public static void writeLicenseFile(String licenseFile, String newLicense)
    {
        try
        {
            // write the news out to the appropriate file
            FileOutputStream fos = new FileOutputStream(licenseFile);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.print(newLicense);
            out.close();
        }
        catch (IOException e)
        {
            warn("license_write: " + e.getLocalizedMessage());
        }

        license = newLicense;
     }

    private static File loadedFile = null;

    /**
     * Return the file that configuration was actually loaded from. Only returns
     * a valid File after configuration has been loaded.
     * 
     * @deprecated Please remove all direct usage of the configuration file.
     * @return File naming configuration data file, or null if not loaded yet.
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
                DSpacePropertiesConfiguration modProps = new DSpacePropertiesConfiguration(modFile);
                if (loadedFile != null)
                	modProps.load(loadedFile);
                
                moduleProps.put(module, modProps);
            }
            else
            {
                // log invalid request
                warn("Requested configuration module: " + module + " not found");
            }
        }
        catch (Exception ioE)
        {
            fatal("Can't load configuration: " + (modFile == null ? "<unknown>" : modFile.getAbsolutePath()), ioE);
        } 

        return;
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
                properties = new DSpacePropertiesConfiguration(url);

                // walk values, interpolating any embedded references.
                /*
                for (Enumeration<?> pe = properties.propertyNames(); pe.hasMoreElements(); )
                {
                    String key = (String)pe.nextElement();
                    String value = interpolate(key, properties.getProperty(key), 1);
                    if (value != null)
                    {
                        properties.setProperty(key, value);
                    }
                }*/
            }

        }
        catch (Exception e)
        {
            fatal("Can't load configuration: " + url, e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new IllegalStateException("Cannot load configuration: " + url, e);
        }
        finally
        {
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

        // Load in default license
        File licenseFile = new File(getProperty("dspace.dir") + File.separator
                + "config" + File.separator + "default.license");

        FileInputStream  fir = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        try
        {
            
            fir = new FileInputStream(licenseFile);
            ir = new InputStreamReader(fir, "UTF-8");
            br = new BufferedReader(ir);
            String lineIn;
            license = "";

            while ((lineIn = br.readLine()) != null)
            {
                license = license + lineIn + '\n';
            }

            br.close();
            
        }
        catch (IOException e)
        {
            fatal("Can't load license: " + licenseFile.toString() , e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new IllegalStateException("Cannot load license: " + licenseFile.toString(),e);
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                } 
                catch (IOException ioe)
                {                  
                }
            }

            if (ir != null)
            {
                try
                { 
                    ir.close();
                } 
                catch (IOException ioe)
                {             
                }
            }

            if (fir != null)
            {
                try
                {
                    fir.close();
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
             * This is selection from a property in dspace.cfg, if the property
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
             * "log.configuration" it needs to be sent to System.err
             * so do not instantiate another Logging configuration.
             *
             */
            String dsLogConfiguration = ConfigurationManager.getProperty("log.init.config");

            if (dsLogConfiguration == null ||  System.getProperty("dspace.log.init.disable") != null)
            {
                /* 
                 * Do nothing if log config not set in dspace.cfg or "dspace.log.init.disable" 
                 * system property set.  Leave it upto log4j to properly init its logging 
                 * via classpath or system properties.
                 */
                info("Using default log4j provided log configuration," +
                        "if unintended, check your dspace.cfg for (log.init.config)");
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
