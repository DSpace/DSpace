/*
 * ConfigurationManager
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Category;


/**
 * Class for reading the DSpace system configuration.  The main configuration
 * is read in as properties from a standard properties file.  Email templates
 * and configuration files for other tools are also be accessed via this class.
 * <P>
 * The main configuration is by default read from the <em>resource</em>
 * <code>/dspace.cfg</code>.  To specify a different configuration, the
 * system property <code>dspace.configuration</code> should be set to the
 * <em>filename</em> of the configuration file.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class ConfigurationManager
{
    /** log4j category */
    private static Category log =
        Category.getInstance(ConfigurationManager.class);

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
            loadProperties();
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
            loadProperties();
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
            loadProperties();
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
     * @return  the template for that email, or null if that template doesn't
     *          exist.
     */
    public static String getEmail(String template)
    {
        // FIXME: Read in template
        return "";
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
            loadProperties();
        }

        return license;
    }


    /**
     * Load the properties if they aren't already loaded
     */
    private static void loadProperties()
    {
        InputStream is;

        if (properties != null)
        {
            return;
        }
    
        try
        {
            // Has the default configuration location been overridden?
            String configProperty = System.getProperty("dspace.configuration");

            if (configProperty != null)
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
                license = license + lineIn;
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
}
