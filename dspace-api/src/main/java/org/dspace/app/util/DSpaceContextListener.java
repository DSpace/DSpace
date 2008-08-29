/*
 * DSpaceContextListener.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/05 21:39:29 $
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

package org.dspace.app.util;

import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.apache.log4j.Logger;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

import java.beans.Introspector;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

/**
 * Class to initialize / cleanup resources used by DSpace when the web application
 * is started or stopped
 */
public class DSpaceContextListener implements ServletContextListener
{
    private static Logger log = Logger.getLogger(DSpaceContextListener.class);

    /**
     * The DSpace config parameter, this is where the path to the DSpace
     * configuration file can be obtained
     */
    public static final String DSPACE_CONFIG_PARAMETER = "dspace-config";
    
    /**
     * Initialize any resources required by the application
     * @param event
     */
    public void contextInitialized(ServletContextEvent event)
    {

        // On Windows, URL caches can cause problems, particularly with undeployment
        // So, here we attempt to disable them if we detect that we are running on Windows
        try
        {
            String osName = System.getProperty("os.name");

            if (osName != null && osName.toLowerCase().contains("windows"))
            {
                URL url = new URL("http://localhost/");
                URLConnection urlConn = url.openConnection();
                urlConn.setDefaultUseCaches(false);
            }
        }
        catch (Throwable t)
        {
            log.error(t.getMessage(), t);
            // Any errors thrown in disabling the caches aren't significant to
            // the normal execution of the application, so we ignore them
        }

        // Paths to the various config files
        String dspaceConfig = null;
        
        /**
         * Stage 1
         * 
         * Locate the dspace config
         */
        
        // first check the local per webapp parameter, then check the global parameter.
        dspaceConfig = event.getServletContext().getInitParameter(DSPACE_CONFIG_PARAMETER);
        
        // Finaly, if no config parameter found throw an error
        if (dspaceConfig == null || "".equals(dspaceConfig))
        {
            throw new RuntimeException(
                    "\n\nDSpace has failed to initialize. This has occurred because it was unable to determine \n" +
                    "where the dspace.cfg file is located. The path to the configuration file should be stored \n" +
                    "in a context variable, '"+DSPACE_CONFIG_PARAMETER+"', in the global context. \n" +
                    "No context variable was found in either location.\n\n");
        }
            
        /**
         * Stage 2
         * 
         * Load the dspace config. Also may load log4j configuration.
         * (Please rely on ConfigurationManager or Log4j to configure logging)
         * 
         */
        try 
        {
            ConfigurationManager.loadConfig(dspaceConfig);
        }
        catch (Throwable t)
        {
            throw new RuntimeException(
                    "\n\nDSpace has failed to initialize, during stage 2. Error while attempting to read the \n" +
                    "DSpace configuration file (Path: '"+dspaceConfig+"'). \n" +
                    "This has likely occurred because either the file does not exist, or it's permissions \n" +
                    "are set incorrectly, or the path to the configuration file is incorrect. The path to \n" +
                    "the DSpace configuration file is stored in a context variable, 'dspace-config', in \n" +
                    "either the local servlet or global context.\n\n",t);
        }

    }

    /**
     * Clean up resources used by the application when stopped
     * 
     * @param event
     */
    public void contextDestroyed(ServletContextEvent event)
    {
        try
        {
            // Remove the database pool
            DatabaseManager.shutdown();

            // Clean out the introspector
            Introspector.flushCaches();

            // Remove any drivers registered by this classloader
            for (Enumeration e = DriverManager.getDrivers(); e.hasMoreElements();)
            {
                Driver driver = (Driver) e.nextElement();
                if (driver.getClass().getClassLoader() == getClass().getClassLoader())
                {
                    DriverManager.deregisterDriver(driver);
                }
            }
        }
        catch (Throwable e)
        {
            log.error("Failed to cleanup ClassLoader for webapp", e);
        }
    }
}
