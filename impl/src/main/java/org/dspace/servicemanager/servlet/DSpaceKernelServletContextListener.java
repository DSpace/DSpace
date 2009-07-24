/*
 * $URL: $
 * 
 * $Revision: $
 * 
 * $Date: $
 *
 * Copyright (c) 2008, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its 
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
