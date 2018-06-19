/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.File;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.configuration.XMLUIConfiguration;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestSchedulingService;

/**
 * This is a wrapper servlet around the cocoon servlet that performs two functions, 1) it 
 * initializes DSpace / XML UI configuration parameters, and 2) it will perform interrupted 
 * request resumption.
 * 
 * @author Scott Phillips
 */
public class DSpaceCocoonServletFilter implements Filter 
{
    private static final Logger LOG = Logger.getLogger(DSpaceCocoonServletFilter.class);
	
	private static final long serialVersionUID = 1L;

    protected HarvestSchedulingService harvestSchedulingService = HarvestServiceFactory.getInstance().getHarvestSchedulingService();

    /**
     * Before this servlet will become functional replace 
     */
    public void init(FilterConfig arg0) throws ServletException {

    	// Paths to the various config files
    	String webappConfigPath    = null;
    	String installedConfigPath = null;
             	            
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
    		
    		webappConfigPath = arg0.getServletContext().getRealPath("/") 
    				+ File.separator + "WEB-INF" + File.separator + "xmlui.xconf";
    		
    		installedConfigPath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
	                + File.separator + "config" + File.separator + "xmlui.xconf";
    		
	        XMLUIConfiguration.loadConfig(webappConfigPath,installedConfigPath);
    	}   
    	catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
    	{
    		throw new ServletException(
    				"\n\nDSpace has failed to initialize, during stage 3. Error while attempting to read \n" +
    				"the XML UI configuration file (Path: "+webappConfigPath+" or '"+installedConfigPath+"').\n" + 
    				"This has likely occurred because either the file does not exist, or its permissions \n" +
    				"are set incorrectly, or the path to the configuration file is incorrect. The XML UI \n" +
    				"configuration file should be named \"xmlui.xconf\" and located inside the standard \n" +
    				"DSpace configuration directory. \n\n",e);
    	}
   
		if (DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("oai.harvester.autoStart"))
    	{
    		try {
                harvestSchedulingService.startNewScheduler();
    		}
            catch (RuntimeException e)
            {
                LOG.error(e.getMessage(), e);
            }
    		catch (Exception e)
    		{
                LOG.error(e.getMessage(), e);
    		}
    	}
    	
    }
    
	
	/**
     * Before passing off a request to the cocoon servlet check to see if there is a request that 
     * should be resumed? If so replace the real request with a faked request and pass that off to 
     * cocoon.
     */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain arg2) throws IOException, ServletException { 
    
            HttpServletRequest realRequest = (HttpServletRequest)request;
            HttpServletResponse realResponse = (HttpServletResponse) response;

            try {
	    	// Check if there is a request to be resumed.
	        realRequest = AuthenticationUtil.resumeRequest(realRequest);
	
	        // Send the real request or the resumed request off to
	        // cocoon....right after we check our URL...
	
                // Get the Request URI, this will include the Context Path
                String requestUri = realRequest.getRequestURI();
                // Get the Context Path of the XMLUI web application
                String contextPath = realRequest.getContextPath();
                // Remove the Context Path from the Request URI -- this is the URI within our webapp
                String uri = requestUri.replace(contextPath, "");
                
                // If the URI within XMLUI is an empty string, this means user 
                // accessed XMLUI homepage *without* a trailing slash
                if(uri==null || uri.length()==0)
                {
                    // Redirect the user to XMLUI homepage with a trailing slash
                    // (This is necessary to ensure our Session Cookie, which ends 
                    // in a trailing slash, isn't lost by some browsers, e.g. IE)
                    String locationWithTrailingSlash = realRequest.getRequestURI() + "/";
                    
                    // Reset any existing response headers -- instead we are going to redirect user to correct path
                    realResponse.reset();
                    
                    // Redirect user to homepage with trailing slash
                    realResponse.sendRedirect(locationWithTrailingSlash);
                }    
	        // if force ssl is on and the user has authenticated and the request is not secure redirect to https
                else if ((DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.force.ssl"))
                        && (AuthenticationUtil.isLoggedIn(realRequest))
                        && (!realRequest.isSecure()))
                {
                    StringBuffer location = new StringBuffer("https://");
                    location.append(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.hostname"))
                        .append(realRequest.getRequestURI())
                            .append(realRequest.getQueryString() == null ? ""
                                    : ("?" + realRequest.getQueryString()));
                    realResponse.sendRedirect(location.toString());
	        }
                else
                {   // invoke the next filter
                    arg2.doFilter(realRequest, realResponse);
                }
    } catch (IOException e) {
        ContextUtil.abortContext(realRequest);
        if (LOG.isDebugEnabled()) {
              LOG.debug("The connection was reset", e);
            }
        else {
            LOG.error("Client closed the connection before file download was complete");
        }
    } catch (RuntimeException e) {
        ContextUtil.abortContext(realRequest);
        LOG.error("Serious Runtime Error Occurred Processing Request!", e);
        throw e;
	} catch (Exception e) {
	    ContextUtil.abortContext(realRequest);
            LOG.error("Serious Error Occurred Processing Request!", e);
	} finally {
	    // Close out the DSpace context no matter what.
	    ContextUtil.completeContext(realRequest);
	}
    }

	public void destroy() {
		// TODO Auto-generated method stub
		
	}



}
