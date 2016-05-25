/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * Miscellaneous UI utility methods methods for managing DSpace context.
 * 
 * This class was "adapted" from the UIUtil.java in the DSpace webui.
 * 
 * @author Robert Tansley
 * @author Scott Phillips
 */
public class ContextUtil
{
    /** Whether to look for x-forwarded headers for logging IP addresses */
    private static Boolean useProxies;

    /** The log4j logger */
    private static final Logger log = Logger.getLogger(ContextUtil.class);
    
    /** Where the context is stored on an HTTP Request object */
    public static final String DSPACE_CONTEXT = "dspace.context";

    protected static final AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();

    /**
     * Obtain a new context object. If a context object has already been created
     * for this HTTP request, it is re-used, otherwise it is created.
     * 
     * @param objectModel
     *            the cocoon object model.
     * 
     * @return a context object
     * @throws java.sql.SQLException passed through.
     */
    public static Context obtainContext(Map objectModel) throws SQLException
    {
        return obtainContext(ObjectModelHelper.getRequest(objectModel));
    }
    
    /** 
     * Inspection method to check if a DSpace context has been created for this request.
     * 
     * @param objectModel The cocoon ObjectModel
     * @return True if a context has previously been created, false otherwise.
     */
    public static boolean isContextAvailable(Map objectModel) {
    	Request request = ObjectModelHelper.getRequest(objectModel);
    	Object object = request.getAttribute(DSPACE_CONTEXT);
    	
    	if (object instanceof Context)
    		return true;
    	else
    		return false;
    }
    
    /**
     * Obtain a new context object. If a context object has already been created
     * for this HTTP request, it is re-used, otherwise it is created.
     * 
     * @param request
     *            the Cocoon or Servlet request object
     * 
     * @return a context object
     * @throws java.sql.SQLException passed through.
     */
    public static Context obtainContext(HttpServletRequest request) throws SQLException
    {
        Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

        if (context == null)
        {
            // No context for this request yet
            context = new Context();

            // Set the session ID
            context.setExtraLogInfo("session_id="
                    + request.getSession().getId());

            AuthenticationUtil.resumeLogin(context, request);

            // Set any special groups - invoke the authentication mgr.
            List<Group> groups = authenticationService.getSpecialGroups(context, request);

            for (int i = 0; i < groups.size(); i++)
            {
                context.setSpecialGroup(groups.get(i).getID());
                log.debug("Adding Special Group id="+String.valueOf(groups.get(i).getID()));
            }

            // Set the session ID and IP address
            String ip = request.getRemoteAddr();
            if (useProxies == null) {
                useProxies = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("useProxies", false);
            }
            if(useProxies && request.getHeader("X-Forwarded-For") != null)
            {
                /* This header is a comma delimited list */
	            for(String xfip : request.getHeader("X-Forwarded-For").split(","))
                {
                    if(!request.getHeader("X-Forwarded-For").contains(ip))
                    {
                        ip = xfip.trim();
                    }
                }
	        }
            context.setExtraLogInfo("session_id=" + request.getSession().getId() + ":ip_addr=" + ip);

            // Store the context in the request
            request.setAttribute(DSPACE_CONTEXT, context);
        }

        return context;
    }

    /**
     * Check if a context exists for this request, if so complete the context.
     * 
     * @param request
     *            The request object 
     * @throws javax.servlet.ServletException on failure.
     */
    public static void completeContext(HttpServletRequest request) throws ServletException
    {
    	Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

    	if (context != null && context.isValid())
    	{
   			try
			{
				context.complete();
			}
			catch (SQLException e)
			{
				throw new ServletException(e);
			}
    	}
    }

    /**
     * Abort the context of a request.
     *
     * @param request the request to be aborted.
     */
	public static void abortContext(HttpServletRequest request)
	{
    	Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

    	if (context != null && context.isValid())
    	{
   			context.abort();
    	}
	}

}
