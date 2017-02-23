/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.solr.filters;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class LocalHostRestrictionFilter implements Filter {

    private boolean enabled = true;

    public LocalHostRestrictionFilter() {
        // TODO Auto-generated constructor stub
    }

    public void destroy() {
        // TODO Auto-generated method stub

    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
        throws IOException, ServletException
    {

        if (enabled){
            InetAddress ia = InetAddress.getLocalHost();
            String localAddr = ia.getHostAddress();
            String remoteAddr = request.getRemoteAddr();

            if (!(localAddr.equals(remoteAddr) ||
                remoteAddr.equals("127.0.0.1") ||
                remoteAddr.startsWith("0:0:0:0:0:0:0:1")))
            {
                ((HttpServletResponse)response).sendError(403);
                    return;
            }

        }

        chain.doFilter(request, response);
    }

    /**
     *
     */
    public void init(FilterConfig arg0)
        throws ServletException
    {
        String restrict = arg0.getServletContext().getInitParameter(
            "LocalHostRestrictionFilter.localhost");
        if ("false".equalsIgnoreCase(restrict))
        {
            enabled = false;
        }
    }
}
