/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;

/**
 * A Servlet Filter whose only role is to clean up open Context objects in
 * the request. (These Context objects may have been created by Controllers
 * in order to populate Views).
 *
 * @author Tim Donohue
 * @see ContextUtil
 */
public class DSpaceRequestContextFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //noop
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        Context context = null;
        try {
            // First, process any other servlet filters, along with the controller & view
            chain.doFilter(request, response);

            // *After* view was processed, check for an open Context object in the ServletRequest
            // (This Context object may have been opened by a @Controller via ContextUtil.obtainContext())
            context = (Context) request.getAttribute(ContextUtil.DSPACE_CONTEXT);
        } finally {
            // Abort the context if it's still valid, thus closing any open
            // database connections
            if ((context != null) && context.isValid()) {
                ContextUtil.abortContext(request);
            }
        }
    }

    @Override
    public void destroy() {
        //noop
    }
}