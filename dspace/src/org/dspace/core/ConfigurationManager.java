/*
 * ConfigurationManager
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.dspace.core.Constants;


/**
 * Class for reading the DSpace system configuration.  The main configuration
 * is read in as properties from a standard properties file.  Email templates
 * and configuration files for other tools are also be accessed via this class.
 * <P>
 * The main configuration is by default read from the <em>resource</em>
 * <code>/dspace.cfg</code>.  To specify a different configuration, the
 * system property <code>dspace.configuration</code> should be set to the
 * <em>filename</em> of the configuration file.
 * <P>
 * Other configuration files are read from the <code>config</code> directory
 * of the DSpace installation directory (specified as the property
 * <code>dspace.dir</code> in the main configuration file.)
 * <P>
 * Configuration files for other tools are kept in <code>config/templates</code>
 * and can contain placeholders for configuration values from
 * <code>dspace.cfg</code>.  See <code>installConfigurations</code> for details.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class ConfigurationManager
{
    /** log4j category */
    private static Logger log =
        Logger.getLogger(ConfigurationManager.class);

    /** The configuration properties */
    private static Properties properties = null;

    /** The default license */
    private static String license;


    /**
     * Get a configuration property
     *
     * @param  property   the name of the property
     *
     * @return   the value of the property, or <code>null</code> if the property
     *           does not exist.
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
     * @param  property   the name of the property
     *
     * @return   the value of the property.  <code>0</code> is returned if the
     *           property does not exist.  To differentiate between this case
     *           and when the property actually is zero, use
     *           <code>getProperty</code>.
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
                intValue = Integer.parseInt(stringValue);
            }
            catch (NumberFormatException e)
            {
                log.warn("Warning: Number format error in property: "
                    + property);
            }
        }            

        return intValue;
    }

    
    /**
     * Get a configuration property as a boolean.  True is indicated if the
     * value of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param  property   the name of the property
     *
     * @return   the value of the property.  <code>false</code> is returned if
     *           the property does not exist.  To differentiate between this
     *           case and when the property actually is false, use
     *           <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property)
    {
        if (properties == null)
        {
            loadConfig(null);
        }

        String stringValue = properties.getProperty(property);
        boolean boolValue = false;
        
        if (stringValue != null)
        {
            if (stringValue.equalsIgnoreCase("true"))
            {
                boolValue = true;
            }
            else if (stringValue.equalsIgnoreCase("yes"))
            {
                boolValue = true;
            }
        }

        return boolValue;
    }
    

    /**
     * Get the template for an email message.  The message is suitable for
     * inserting values using <code>java.text.MessageFormat</code>.
     *
     * @param template   name for the email template, for example "register".
     *
     * @return  the email object, with the content and subject filled out
     *          from the template
     *
     * @throws IOException   if the template couldn't be found, or there
     *                       was some other error reading the template
     */
    public static Email getEmail(String template)
        throws IOException
    {
        String subject = "";
        StringBuffer contentBuffer = new StringBuffer();

        // Read in template
        BufferedReader reader = new BufferedReader(new FileReader(
            getProperty("dspace.dir") + File.separator +
            "config" + File.separator + "emails" + File.separator +
            template));
        
        boolean more = true;
        
        while (more)
        {
            String line = reader.readLine();

            if (line==null)
            {
                more = false;
            }
            else if (line.toLowerCase().startsWith("subject:"))
            {
                // Extract the first subject line - everything to the right
                // of the colon, trimmed of whitespace
                subject = line.substring(8).trim();
            }
            else if(!line.startsWith("#"))
            {
                // Add non-comment lines to the content
                contentBuffer.append(line);
                contentBuffer.append("\n");
            }
        }

        reader.close();
        
        // Create an email
        Email email = new Email();
        email.setSubject(subject);
        email.setContent(contentBuffer.toString());

        return email;
    }


    /**
     * Get the site-wide default license that submitters need to grant
     *
     * @return  the default license
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
        String filePath = ConfigurationManager.getProperty("dspace.dir") + File.separator +
            "config" + File.separator;    
        
        return filePath;
    }
     
     /**
     * Reads news from a text file.  
     *
     * @param position a constant indicating which file (top or side)
      *should be read in.
     */
     public static String readNewsFile(int position)
     {
     
        String fileName = getNewsFilePath();
        
        if(position == Constants.NEWS_TOP)
        {
            fileName += "news-top.html";
        }
        else
        {
            fileName += "news-side.html";
        }
             
        String text = "";
                     
        try
        {
            //retrieve existing news from file
            BufferedReader br = new BufferedReader( new FileReader(fileName) );
            String lineIn;

            while((lineIn = br.readLine()) != null)
            {
                text += lineIn;
            }

            br.close();            
        }
        catch(IOException e )
        {
            log.warn("news_read: " + e.getLocalizedMessage());
        }
        
        return text;
    }
     
      /**
     * Writes news to a text file. 
     *
     * @param position a constant indicating which file (top or side)
      *should be written to.
       *
       *@param news the text to be written to the file.
     */
     public static String writeNewsFile(int position, String news)
     {
     
        String fileName = getNewsFilePath();
        
        if(position == Constants.NEWS_TOP)
        {
            fileName += "news-top.html";
        }
        else
        {
            fileName += "news-side.html";
        }
        
         
        try
        {

            //write the news out to the appropriate file
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));                 
            PrintWriter out = new PrintWriter( bw );    
            out.print(news);           
            out.close();

        }catch(IOException e)
        {
            log.warn("news_write: " + e.getLocalizedMessage());
        }
        
        return news;
    }


    /**
     * Load the DSpace configuration properties.  Only does anything if properties
     * are not already loaded.  Properties are loaded in from the specified file,
     * or default locations. 
     *
     * @param configFile  The <code>dspace.cfg</code> configuration file to use,
     *                    or <code>null</code> to try default locations 
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
        	}
        	// Has the default configuration location been overridden?
        	else if (configProperty != null)
            {
                // Load the overriding configuration
                is = new FileInputStream(configProperty);
            }
            else
            {
                // Load configuration from default location
                is = ConfigurationManager.class.getResourceAsStream(
                    "/dspace.cfg");
            }

            if (is == null)
            {
                log.fatal("Cannot find dspace.cfg");
                System.exit(1);
            }
            else
            {
                properties = new Properties();
                properties.load(is);
            }

            // Load in default license
            String licenseFile = getProperty("dspace.dir") + File.separator + 
                "config" + File.separator + "default.license";

            BufferedReader br = new BufferedReader(new FileReader(licenseFile));
            String lineIn;            
            license = "";

            while ((lineIn = br.readLine()) != null)
            {
                license = license + lineIn + '\n';
            }
            
            is.close();
        }
        catch (IOException e)
        {
            log.fatal("Can't load configuration", e);
            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            System.exit(1);
        }
    }


    /**
     * Fill out of the configuration file templates in
     * <code>dspace.dir/config/templates</code> with appropriate values from
     * <code>dspace.cfg</code>, and copy to their appropriate destination.
     * The destinations are defined as properties in <code>dspace.cfg</code>
     * called <code>config.template.XXX</code> where <code>XXX</code> is the
     * filename of the template.  If this property doesn't exist, the
     * configuration file template is skipped.
     *
     * @throws IOException   if there was some problem reading the templates
     *                       or writing the filled-out configuration files
     */
    public static void installConfigurations()
        throws IOException
    {
        // Get the templates
        File templateDir = new File(getProperty("dspace.dir") +
            File.separator + "config" + File.separator + "templates");
        
        File[] templateFiles = templateDir.listFiles();
        
        for (int i=0; i<templateFiles.length; i++)
        {
            installConfigurationFile(templateFiles[i].getName());
        }
    }
     
     
    /**
     * Install the given configuration file template in its required
     * location.  Configuration values in the template, specified as
     * <code>@@property.name@@</code> are filled out with appropriate
     * properties from the configuration.  The filled-out configuration file
     * is written to the file named by the property
     * <code>config.template.XXX</code> where <code>XXX</code> is the name
     * of the template as passed in to this method.
     *
     * @param template  the name of the configuration template.  This must
     *   correspond to the filename in <code>dspace.dir/config/templates</code>
     *   and the property starting with <code>config.template.</code>.
     *
     * @throws IOException   if there was some problem reading the template
     *                       or writing the filled-out configuration file
     */
    private static void installConfigurationFile(String template)
        throws IOException
    {
        // Get the destination: specified in property config.template.XXX
        String destination = getProperty("config.template." + template);

        if (destination == null)
        {
            // If no destination is specified
            log.info("Not processing config file template " + template +
                " because no destination specified (no property " +
                "config.template." + template + ")");
            return;
        }

        log.info("Installing configuration file template " + template +
            " to " + destination);

        // Open the template
        BufferedReader in = new BufferedReader(new FileReader(
            getProperty("dspace.dir") + File.separator + "config" +
            File.separator + "templates" + File.separator + template));
        
        // Open the destination for writing
        PrintWriter out = new PrintWriter(new FileWriter(destination));
        
        // We'll keep track of line numbers for error messages etc.
        int lineNumber = 0;
        String line;

        // Copy the template, filling out config values
        while ((line = in.readLine()) != null)
        {
            lineNumber++;

            // Find configuration values
            boolean moreValues = true;
            while (moreValues)
            {
                // Look for "@@"
                int first = line.indexOf("@@");

                if (first > -1)
                {
                    // Look for the "@@" on the other side
                    int second = line.indexOf("@@", first + 2);
                    if (second > -1)
                    {
                        // We have a property
                        String propName = line.substring(first+2, second);
                        
                        String propValue = getProperty(propName);
                        
                        if (propValue == null)
                        {
                            log.warn(template + " line " + lineNumber +
                                ": Property " + propName +
                                " not defined in DSpace configuration - " +
                                "using empty string");

                            propValue = "";
                        }

                        // Fill in the value
                        line = line.substring(0, first) + propValue +
                            line.substring(second+2);
                    }
                    else
                    {
                        // There's a "@@" with no second one... just leave as-is
                            log.warn(template + " line " + lineNumber +
                                ": Single @@ - leaving as-is");
                        moreValues = false;
                    }
                }
                else
                {
                    // No more @@'s
                    moreValues = false;
                }
            }

            // Write the line
            out.println(line);
        }

        in.close();
        out.close();
    }

        

    /**
     * Command-line interface for running configuration tasks.  Possible
     * arguments:
     * <UL>
     * <LI><code>-installTemplates</code> processes and installs the
     * configuration file templates for other tools</LI>
     * <LI><code>-property name</code> prints the value of the property
     * <code>name</code> from <code>dspace.cfg</code> to the standard output.
     * If the property does not exist, nothing is written.</LI>
     * </UL>
     *
     * @param argv   command-line arguments
     */
    public static void main(String argv[])
    {
        if (argv.length == 1 && argv[0].equals("-installTemplates"))
        {
            try
            {
                log.info("Installing configuration files for other tools");
                installConfigurations();
                System.exit(0);
            }
            catch(IOException ie)
            {
                log.warn("Error installing configuration files", ie);
            }
        }
        else if (argv.length == 2 && argv[0].equals("-property"))
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
            System.err.println("Usage: ConfigurationManager OPTION\n  -installTemplates    install config files for external tools\n  -property prop.name  get value of prop.name from dspace.cfg");
        }
        
        System.exit(1);
    }
    
    

     
    
    
}
