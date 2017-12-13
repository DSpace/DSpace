/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import java.sql.SQLException;

/**
 * Miscellaneous UI utility methods methods for managing DSpace context.
 * 
 * This class was "adapted" from the class of the same name in old XMLUI.
 * 
 * @author Tim Donohue
 */
public class ContextUtil 
{
    /** The log4j logger */
    private static final Logger log = Logger.getLogger(ContextUtil.class);
    
    /** Where the context is stored on an HTTP Request object */
    public static final String DSPACE_CONTEXT = "dspace.context";
    
    /** 
     * Inspection method to check if a DSpace context has been created for this request.
     * 
     * @param request
     *            the servlet request object
     * @return True if a context has previously been created, false otherwise.
     */
    public static boolean isContextAvailable(ServletRequest request) {
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
     *            the servlet request object
     * 
     * @return a context object
     */
    public static Context obtainContext(ServletRequest request)
    {
        Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

        if (context == null)
        {
            try {
                context = ContextUtil.intializeContext();
            } catch (SQLException e) {
                log.error("Unable to initialize context", e);
                return null;
            }

            // Store the context in the request
            request.setAttribute(DSPACE_CONTEXT, context);
        }

        return context;
    }

    /**
     * Initialize a new Context object
     * @return a DSpace Context Object
     * @throws SQLException 
     */
    private static Context intializeContext() throws SQLException
    {
        // Create a new Context
        Context context = new Context();

        // Set the session ID
        /**context.setExtraLogInfo("session_id="
                + request.getSession().getId());

        AuthenticationUtil.resumeLogin(context, request);

        // Set any special groups - invoke the authentication mgr.
        int[] groupIDs = AuthenticationManager.getSpecialGroups(context, request);

        for (int i = 0; i < groupIDs.length; i++)
        {
            context.setSpecialGroup(groupIDs[i]);
            log.debug("Adding Special Group id="+String.valueOf(groupIDs[i]));
        }

        // Set the session ID and IP address
        String ip = request.getRemoteAddr();
        if (useProxies == null) {
            useProxies = ConfigurationManager.getBooleanProperty("useProxies", false);
        }
        if(useProxies && request.getHeader("X-Forwarded-For") != null)
        {
            // This header is a comma delimited list
            for(String xfip : request.getHeader("X-Forwarded-For").split(","))
            {
                if(!request.getHeader("X-Forwarded-For").contains(ip))
                {
                    ip = xfip.trim();
                }
            }
        }
        context.setExtraLogInfo("session_id=" + request.getSession().getId() + ":ip_addr=" + ip);
        */

        return context;
    }
    
    /**
     * Check if a context exists for this request, if so complete the context.
     * 
     * @param request
     *            The request object 
     */
    public static void completeContext(ServletRequest request) throws ServletException
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

	public static void abortContext(ServletRequest request)
	{
    	Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

    	if (context != null && context.isValid())
    	{
   			context.abort();
    	}
	}
}