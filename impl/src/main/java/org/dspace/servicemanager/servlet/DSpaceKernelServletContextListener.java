/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager.servlet;

import java.io.File;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.servicemanager.config.DSpaceConfigurationService;


/**
 * This servlet context listener will handle startup of the kernel if it is not there,
 * shutdown of the context listener does not shutdown the kernel though,
 * that is tied to the shutdown of the JVM <br/>
 * 
 * This is implemented in the web application web.xml using:
 * 
 * <web-app>
 *
 * <listener>
 *   <listener-class>
 *  org.dspace.servicemanager.servlet.DSpaceKernelServletContextListener
 *  </listener-class>
 *</listener>
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 * @author Mark Diggory (mdiggory @ gmail.com)
 */
public class DSpaceKernelServletContextListener implements ServletContextListener {

    private transient DSpaceKernelImpl kernelImpl;

    /*
     * Initially look for JNDI Resource called java:/comp/env/dspace.dir
     * otherwise, look for "dspace.dir" initial context param 
     */
    private String getProvidedHome(ServletContextEvent arg0){
    	String providedHome = null;
    	try {
			Context ctx = new InitialContext();
			providedHome = (String) ctx.lookup("java:/comp/env/" + DSpaceConfigurationService.DSPACE_HOME);
		} catch (Exception e) {
			// do nothing
		}
		
		if (providedHome == null)
		{
			String dspaceHome = arg0.getServletContext().getInitParameter(DSpaceConfigurationService.DSPACE_HOME);
			if(dspaceHome != null && !dspaceHome.equals("") && 
					!dspaceHome.equals("${" + DSpaceConfigurationService.DSPACE_HOME + "}")){
				File test = new File(dspaceHome);
				if(test.exists() && new File(test,DSpaceConfigurationService.DSPACE_CONFIG_PATH).exists())
					providedHome = dspaceHome;
			}
		}
		return providedHome;
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent arg0)
    {
        // start the kernel when the webapp starts
        try {
            this.kernelImpl = DSpaceKernelInit.getKernel(null);
            if (! this.kernelImpl.isRunning()) {
            	this.kernelImpl.start(getProvidedHome(arg0)); // init the kernel
            }
        } catch (Exception e) {
            // failed to start so destroy it and log and throw an exception
            try {
                this.kernelImpl.destroy();
            } catch (Exception e1) {
                // nothing
            }
            String message = "Failure during filter init: " + e.getMessage();
            System.err.println(message + ":" + e);
            throw new RuntimeException(message, e);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0)
    {
        // currently we are stopping the kernel when the webapp stops
        if (this.kernelImpl != null) {
            this.kernelImpl.destroy();
            this.kernelImpl = null;
        }
        // No longer going to use JCL
//        // clean up the logger for this webapp
//        LogFactory.release(Thread.currentThread().getContextClassLoader());
        // No longer cleaning this up here since it causes failures
//        // cleanup the datasource
//        try {
//            for (Enumeration<?> e = DriverManager.getDrivers(); e.hasMoreElements(); ) {
//                Driver driver = (Driver) e.nextElement();
//                if (driver.getClass().getClassLoader() == getClass().getClassLoader()) {
//                    DriverManager.deregisterDriver(driver);
//                }
//            }
//        } catch (Throwable e) {
//            System.err.println("Unable to clean up JDBC driver: " + e.getMessage());
//        }
    }

}
