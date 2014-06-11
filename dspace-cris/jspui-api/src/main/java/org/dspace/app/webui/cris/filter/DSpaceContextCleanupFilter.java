/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.filter;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * Filter to ensure the correct closing of DSpace Context when used by the RP
 * module
 * 
 * @author cilea
 * 
 */
public class DSpaceContextCleanupFilter implements Filter
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DSpaceContextCleanupFilter.class);

    public void init(FilterConfig config)
    {
        // Do nothing
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws ServletException, IOException
    {
        // We need HTTP request objects
        HttpServletRequest hrequest = (HttpServletRequest) request;
        HttpServletResponse hresponse = (HttpServletResponse) response;

        try
        {
            
            chain.doFilter(hrequest, hresponse);
        }
        finally
        {
            Context context = null;
            try
            {
                context = UIUtil.obtainContext(hrequest);
            }
            catch (SQLException e)
            {
               log.error(LogManager.getHeader(null, "cleanup", "Exception during DSpace context cleanup"), e);
            }
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
    }

    public void destroy()
    {
        // Nothing
    }
}
