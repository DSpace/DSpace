/*
 * DSpaceCocoonServlet.java
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

package org.dspace.app.xmlui.cocoon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cocoon.servlet.CocoonServlet;
import org.dspace.app.xmlui.configuration.XMLUIConfiguration;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;

/**
 * This is a wrapper servlet around the cocoon servlet that prefroms two functions, 1) it 
 * initializes DSpace / XML UI configuration parameters, and 2) it will preform inturrupted 
 * request resumption.
 * 
 * @author scott philips
 */
public class DSpaceCocoonServlet extends CocoonServlet 
{
	
	private static final long serialVersionUID = 1L;
	
	
    /**
     * The DSpace config paramater, this is where the path to the DSpace
     * configuration file can be obtained
     */
    public static final String DSPACE_CONFIG_PARAMETER = "dspace-config";
	
    
    
    
    /**
     * Before this servlet will become functional replace 
     */
    public void init() throws ServletException
    {
        // On Windows, URL caches can cause problems, particularly with undeployment
        // So, here we attempt to disable them if we detect that we are running on Windows
        try
        {
            String osName = System.getProperty("os.name");
            if (osName != null)
                osName = osName.toLowerCase();

            if (osName != null && osName.contains("windows"))
            {
                URL url = new URL("http://localhost/");
                URLConnection urlConn = url.openConnection();
                urlConn.setDefaultUseCaches(false);
            }
        }
        catch (Throwable t)
        {
            // Any errors thrown in disabling the caches aren't significant to
            // the normal execution of the application, so we ignore them
        }

    	// Check if cocoon needs to do anything at init time?
    	super.init();

    	// Paths to the various config files
    	String dspaceConfig = null;
    	String log4jConfig  = null;
    	String webappConfigPath    = null;
    	String installedConfigPath = null;
    	
    	/**
    	 * Stage 1
    	 * 
    	 * Locate the dspace config
    	 */
    	
    	// first check the local per webapp parameter, then check the global parameter.
    	dspaceConfig = super.getInitParameter(DSPACE_CONFIG_PARAMETER);
    	if (dspaceConfig == null)
    		dspaceConfig = super.getServletContext().getInitParameter(DSPACE_CONFIG_PARAMETER);
        
    	// Finaly, if no config parameter found throw an error
    	if (dspaceConfig == null || "".equals(dspaceConfig))
    	{
    		throw new ServletException(
    				"\n\nDSpace has failed to initialize. This has occurred because it was unable to determine \n" +
    				"where the dspace.cfg file is located. The path to the configuration file should be stored \n" +
    				"in a context variable, '"+DSPACE_CONFIG_PARAMETER+"', in either the local servlet or global contexts. \n" +
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
    		throw new ServletException(
    				"\n\nDSpace has failed to initialize, during stage 2. Error while attempting to read the \n" +
    				"DSpace configuration file (Path: '"+dspaceConfig+"'). \n" +
    				"This has likely occurred because either the file does not exist, or it's permissions \n" +
    				"are set incorrectly, or the path to the configuration file is incorrect. The path to \n" +
    				"the DSpace configuration file is stored in a context variable, 'dspace-config', in \n" +
    				"either the local servlet or global context.\n\n",t);
    	}
                	            
        /**
         * Stage 3
         * 
         * Load the XML UI configuration
         */
    	try
    	{
    		// There are two places we could find the XMLUI configuration, 
    		// 1) inside the webapp's WEB-INF directory, or 2) inside the 
    		// installed dspace config directory along side the dspace.cfg.
    		
    		webappConfigPath = super.getServletContext().getRealPath("/") 
    				+ File.separator + "WEB-INF" + File.separator + "xmlui.xconf";
    		
    		installedConfigPath = ConfigurationManager.getProperty("dspace.dir")
	                + File.separator + "config" + File.separator + "xmlui.xconf";
    		
	        XMLUIConfiguration.loadConfig(webappConfigPath,installedConfigPath);
    	}   
    	catch (Throwable t)
    	{
    		throw new ServletException(
    				"\n\nDSpace has failed to initialize, during stage 3. Error while attempting to read \n" +
    				"the XML UI configuration file (Path: "+webappConfigPath+" or '"+installedConfigPath+"').\n" + 
    				"This has likely occurred because either the file does not exist, or it's permissions \n" +
    				"are set incorrectly, or the path to the configuration file is incorrect. The XML UI \n" +
    				"configuration file should be named \"xmlui.xconf\" and located inside the standard \n" +
    				"DSpace configuration directory. \n\n",t);
    	}
   
    	
    }
    
	
	/**
     * Before passing off a request to the cocoon servlet check to see if there is a request that 
     * should be resumed? If so replace the real request with a faked request and pass that off to 
     * cocoon.
     */
    public void service(HttpServletRequest realRequest, HttpServletResponse realResponse)
    throws ServletException, IOException 
    {
    	// Check if there is a request to be resumed.
    	realRequest = AuthenticationUtil.resumeRequest(realRequest);
        
        // Send the real request or the resumed request off to
        // cocoon....
    	
    	// if force ssl is on and the user has authenticated and the request is not secure redirect to https
    	if ((ConfigurationManager.getBooleanProperty("xmlui.force.ssl")) && (realRequest.getSession().getAttribute("dspace.current.user.id")!=null) && (!realRequest.isSecure())) {
				StringBuffer location = new StringBuffer("https://");
				location.append(ConfigurationManager.getProperty("dspace.hostname")).append(realRequest.getContextPath()).append(realRequest.getServletPath()).append(
						realRequest.getQueryString() == null ? ""
								: ("?" + realRequest.getQueryString()));
				realResponse.sendRedirect(location.toString());
		}
    	
    	super.service(realRequest, realResponse);
    	
    	// Close out the DSpace context no matter what.
    	ContextUtil.closeContext(realRequest);
    }
    
    

}
