/*
 * ConfigurationManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

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
 * <P>
 * Configuration files for other tools are kept in <code>config/templates</code>
 * and can contain placeholders for configuration values from
 * <code>dspace.cfg</code>. See <code>installConfigurations</code> for
 * details.
 * 
 * @author Robert Tansley
 * @author Larry Stone - Interpolated values.
 * @version $Revision$
 */
public class ConfigurationManager
{
    /** log4j category */
    private static Logger log = null;

    /** The configuration properties */
    private static Properties properties = null;

    /** The default license */
    private static String license;

    // limit of recursive depth of property variable interpolation in
    // configuration; anything greater than this is very likely to be a loop.
    private final static int RECURSION_LIMIT = 9;

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
        if (properties == null)
        {
            loadConfig(null);
        }

        return properties.getProperty(property);
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
        if (properties == null)
        {
            loadConfig(null);
        }

        String stringValue = properties.getProperty(property);
        int intValue = 0;

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
     * Get the License
     * 
     * @param
     *         license file name
     *  
     *  @return
     *         license text
     * 
     */
    public static String getLicenseText(String licenseFile)
    {
    // Load in default license

        try
        {
            BufferedReader br = new BufferedReader(new FileReader(licenseFile));
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
            System.exit(1);
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
        if (properties == null)
        {
            loadConfig(null);
        }

        String stringValue = properties.getProperty(property);

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
    public static Enumeration propertyNames()
    {
        if (properties == null)
            loadConfig(null);

        return properties.propertyNames();
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
     * @param position
     *            a constant indicating which file (top or side) should be read
     *            in.
     */
    public static String readNewsFile(String newsFile)
    {
        String fileName = getNewsFilePath();
        
        fileName += newsFile;
        
        String text = "";

        try
        {
            // retrieve existing news from file
            FileInputStream fir = new FileInputStream(fileName);
            InputStreamReader ir = new InputStreamReader(fir, "UTF-8");
            BufferedReader br = new BufferedReader(ir);

            String lineIn;

            while ((lineIn = br.readLine()) != null)
            {
                text += lineIn;
            }

            br.close();
        }
        catch (IOException e)
        {
            warn("news_read: " + e.getLocalizedMessage());
        }

        return text;
    }

    /**
     * Writes news to a text file.
     * 
     * @param position
     *            a constant indicating which file (top or side) should be
     *            written to.
     * 
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
     * @param news
     *            the text to be written to the file.
     */
    public static void writeLicenseFile(String newLicense)
    {
        String licenseFile = getProperty("dspace.dir") + File.separator
                             + "config" + File.separator + "default.license";

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
     * @return File naming configuration data file, or null if not loaded yet.
     */
    public static File getConfigurationFile()
    {
        // in case it hasn't been done yet.
        loadConfig(null);

        return loadedFile;
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
    public static void loadConfig(String configFile)
    {
        InputStream is;

        if (properties != null)
        {
            return;
        }

        String configProperty = System.getProperty("dspace.configuration");

        try
        {
            if (configFile != null)
            {
                is = new FileInputStream(configFile);
                loadedFile = new File(configFile);
            }
            // Has the default configuration location been overridden?
            else if (configProperty != null)
            {
                // Load the overriding configuration
                is = new FileInputStream(configProperty);
                loadedFile = new File(configProperty);
            }
            else
            {
                // Load configuration from default location
                is = ConfigurationManager.class
                        .getResourceAsStream("/dspace.cfg");
                loadedFile = new File(ConfigurationManager.class.getResource(
                        "/dspace.cfg").getPath());
            }

            if (is == null)
            {
                fatal("Cannot find dspace.cfg");
                throw new RuntimeException("Cannot find dspace.cfg");
            }
            else
            {
                properties = new Properties();
                properties.load(is);

                // walk values, interpolating any embedded references.
                for (Enumeration pe = properties.propertyNames(); pe.hasMoreElements(); )
                {
                    String key = (String)pe.nextElement();
                    String value = interpolate(key, 1);
                    if (value != null)
                        properties.setProperty(key, value);
                }
            }

            // Load in default license
            String licenseFile = getProperty("dspace.dir") + File.separator
                    + "config" + File.separator + "default.license";
            
            FileInputStream fir = new FileInputStream(licenseFile);
            InputStreamReader ir = new InputStreamReader(fir, "UTF-8");
            BufferedReader br = new BufferedReader(ir);
            String lineIn;
            license = "";

            while ((lineIn = br.readLine()) != null)
            {
                license = license + lineIn + '\n';
            }

            is.close();

            // Load in log4j config
            // Load the log4j config, if a log4j.xml version exists use that
            // configuration format over the log4j.properties version
            String log4jConfProp = ConfigurationManager
                    .getProperty("dspace.dir")
                    + File.separator
                    + "config"
                    + File.separator
                    + "log4j.properties";
            String log4jConfXml = ConfigurationManager
                    .getProperty("dspace.dir")
                    + File.separator + "config" + File.separator + "log4j.xml";

            File xmlFile = new File(log4jConfXml);
            if (xmlFile.exists())
            {
                try
                {
                    DOMConfigurator.configure(xmlFile.toURL());
                    initLog();
                    info("DSpace logging installed using log4j.xml");
                }
                catch (MalformedURLException e)
                {
                    PropertyConfigurator.configure(log4jConfProp);
                    initLog();
                    error("Logger failed to load log4j.xml, defaulted to "
                            + "log4j.properties: " + e);
                }
            }
            else
            {
                PropertyConfigurator.configure(log4jConfProp);
                initLog();
                info("DSpace logging installed using log4j.properties");
            }
        }
        catch (IOException e)
        {
            fatal("Can't load configuration", e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new RuntimeException("Cannot find dspace.cfg",e);
        }
    }

    /**
     * Recursively interpolate variable references in value of
     * property named "key".
     * @return new value if it contains interpolations, or null
     *   if it had no variable references.
     */
    private static String interpolate(String key, int level)
    {
        if (level > RECURSION_LIMIT)
            throw new IllegalArgumentException("ConfigurationManager: Too many levels of recursion in configuration property variable interpolation, property="+key);
        String value = (String)properties.get(key);
        int from = 0;
        StringBuffer result = null;
        while (from < value.length())
        {
            int start = value.indexOf("${", from);
            if (start >= 0)
            {
                int end = value.indexOf("}", start);
                if (end < 0)
                    break;
                String var = value.substring(start+2, end);
                if (result == null)
                    result = new StringBuffer(value.substring(from, start));
                else
                    result.append(value.substring(from, start));
                if (properties.containsKey(var))
                {
                    String ivalue = interpolate(var, level+1);
                    if (ivalue != null)
                    {
                        result.append(ivalue);
                        properties.setProperty(var, ivalue);
                    }
                    else
                        result.append((String)properties.getProperty(var));
                }
                else
                {
                    log.warn("Interpolation failed in value of property \""+key+
                             "\", there is no property named \""+var+"\"");
                }
                from = end+1;
            }
            else
                break;
        }
        if (result != null && from < value.length())
            result.append(value.substring(from));
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
        else
        {
            System.err
                    .println("Usage: ConfigurationManager OPTION\n  -installTemplates    install config files for external tools\n  -property prop.name  get value of prop.name from dspace.cfg");
        }

        System.exit(1);
    }

    private static void info(String string)
    {
        if (log == null)
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
        if (log == null)
        {
            System.out.println("WARN: " + string);
        }
        else
        {
            log.warn(string);
        }
    }

    private static void error(String string)
    {
        if (log == null)
        {
            System.err.println("ERROR: " + string);
        }
        else
        {
            log.error(string);
        }
    }

    private static void fatal(String string, Exception e)
    {
        if (log == null)
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
        if (log == null)
        {
            System.out.println("FATAL: " + string);
        }
        else
        {
            log.fatal(string);
        }
    }

    private static void initLog()
    {
        log = Logger.getLogger(ConfigurationManager.class);
    }
    
    

}
