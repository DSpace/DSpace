/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.filter;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * DSpace filter that only allows requests from authenticated shib users
 * to proceed. Anonymous requests prompt the authentication procedure.
 *
 * @author  <a href="mailto:bliong@melcoe.mq.edu.au">Bruc Liong, MELCOE</a>
 * @author  <a href="mailto:kli@melcoe.mq.edu.au">Xiang Kevin Li, MELCOE</a>
 * @version $Revision$
 */
public class ShibbolethFilter implements Filter
{
    /** log4j category */
    private static Logger log = Logger.getLogger(ShibbolethFilter.class);


    public void init(FilterConfig config)
    {
        // Do nothing
    }


    public void doFilter(ServletRequest request,
        ServletResponse response,
        FilterChain chain)
            throws ServletException, IOException
    {
        Context context = null;

        // We need HTTP request objects
        HttpServletRequest hrequest = (HttpServletRequest) request;
        HttpServletResponse hresponse = (HttpServletResponse) response;

        try
        {
            // Obtain a context
            context = UIUtil.obtainContext(hrequest);

            if (context.getCurrentUser() == null)
            {
				java.util.Enumeration names = ((HttpServletRequest) request).getHeaderNames();
				String name;
				while(names.hasMoreElements())
                {
                    name = names.nextElement().toString();
                    log.debug("header:" + name + "=" + ((HttpServletRequest) request).getHeader(name));
                }

                // No current user, prompt authentication
            	Authenticate.startAuthentication(context, hrequest, hresponse);
            }else{
				chain.doFilter(hrequest, hresponse);
				return;
			}
        }
        catch (SQLException se)
        {
            log.warn(LogManager.getHeader(context,
                "database_error",
                se.toString()), se);
            JSPManager.showInternalError(hrequest, hresponse);
        }

        // Abort the context if it's still valid
           if (context != null && context.isValid())
           {
               context.abort();
           }
        }
        
        
        public void destroy()
        {
           // Nothing
        }
}

