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

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;


public class ConfigurationManager
{
    /** The configuration properties */
    private static Properties properties = null;


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
                // FIXME: Should be logged properly
                System.err.println("Warning: Number format error in property"
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
        if (properties==null)
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
    public static String getEmail(String name)
    {
        // FIXME: Read in template
        return "";
    }


    /**
     * Load the properties if they aren't already loaded
     */
    private static void loadProperties()
    {
        if (properties != null) return;
    
        try
        {
            InputStream is = ConfigurationManager.class.getResourceAsStream(
                "/dspace.cfg");
            properties = new Properties();
            properties.load(is);
        }
        catch (IOException e)
        {
            // FIXME: Should be logged properly
            System.err.println("Can't load configuration: " + e);
            e.printStackTrace();
            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            System.exit(1);
        }
    }
}
