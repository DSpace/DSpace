/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * is started or stopped.
 */
public class DSpaceContextListener implements ServletContextListener
{
    private static Logger log = Logger.getLogger(DSpaceContextListener.class);

    /**
     * Name of the context parameter giving the path to the DSpace configuration file.
     */
    public static final String DSPACE_CONFIG_PARAMETER = "dspace-config";

    private AbstractDSpaceWebapp webApp;

    /**
     * Initialize any resources required by the application.
     * @param event
     */
    @Override
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
        // Any errors thrown in disabling the caches aren't significant to
        // the normal execution of the application, so we ignore them
        catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
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
        
        // Finally, if no config parameter found throw an error
        if (dspaceConfig == null || "".equals(dspaceConfig))
        {
            throw new IllegalStateException(
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
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new IllegalStateException(
                    "\n\nDSpace has failed to initialize, during stage 2. Error while attempting to read the \n" +
                    "DSpace configuration file (Path: '"+dspaceConfig+"'). \n" +
                    "This has likely occurred because either the file does not exist, or it's permissions \n" +
                    "are set incorrectly, or the path to the configuration file is incorrect. The path to \n" +
                    "the DSpace configuration file is stored in a context variable, 'dspace-config', in \n" +
                    "either the local servlet or global context.\n\n",e);
        }

        /**
         * Stage 3
         *
         * Register that this application is running.
         */

        try {
            Class webappClass = Class.forName("org.dspace.utils.DSpaceWebapp");
            webApp = (AbstractDSpaceWebapp) webappClass.newInstance();
            webApp.register();
        } catch (ClassNotFoundException ex) {
            event.getServletContext().log("Can't create webapp MBean:  " + ex.getMessage());
        } catch (InstantiationException ex) {
            event.getServletContext().log("Can't create webapp MBean:  " + ex.getMessage());
        } catch (IllegalAccessException ex) {
            event.getServletContext().log("Can't create webapp MBean:  " + ex.getMessage());
        }
    }

    /**
     * Clean up resources used by the application when stopped
     * 
     * @param event
     */
    @Override
    public void contextDestroyed(ServletContextEvent event)
    {
        webApp.deregister();

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
        catch (RuntimeException e)
        {
            log.error("Failed to cleanup ClassLoader for webapp", e);
        }
        catch (Exception e)
        {
            log.error("Failed to cleanup ClassLoader for webapp", e);
        }
    }
}
