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
package org.dspace.utils.servlet;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.dspace.services.CachingService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;


/**
 * This servlet filter will handle the hookup and setup for DSpace requests and should
 * be applied to any webapp that is using the DSpace core <br/>
 * It will also do anything necessary to the requests that are coming into a DSpace web application
 * and the responses on their way out
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceWebappServletFilter implements Filter {

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        // ensure the kernel is running, if not then we have to die here
        try {
            getKernel();
        } catch (IllegalStateException e) {
            // no kernel so we die
            String message = "Could not start up DSpaceWebappServletFilter because the DSpace Kernel is unavailable or not running: " + e.getMessage();
            System.err.println(message);
            throw new ServletException(message, e);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        // clean up the logger for this webapp
        // No longer using commons-logging (JCL), use slf4j instead
        //LogFactory.release(Thread.currentThread().getContextClassLoader());
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // make these into useful http request/response objects
        HttpServletRequest req = null;
        HttpServletResponse res = null;
        if (request instanceof HttpServletRequest) {
            req = (HttpServletRequest) request;
        }
        if (response instanceof HttpServletResponse) {
            res = (HttpServletResponse) response;
        }
        // now do some DSpace stuff
        //try {
            DSpaceKernel kernel = getKernel();
            // place the incoming request into the request cache
            CachingService cachingService = kernel.getServiceManager().getServiceByName(CachingService.class.getName(), CachingService.class);
            if (cachingService == null) {
                throw new IllegalStateException("No caching service is available to hold the request state");
            }
            Cache reqCache = cachingService.getCache(CachingService.REQUEST_CACHE, new CacheConfig(CacheScope.REQUEST));
            // store the servlet req/resp
            reqCache.put("request", request);
            reqCache.put("response", response);
            // store the http servlet req/resp
            if (req != null && res != null) {
                reqCache.put(CachingService.HTTP_REQUEST_KEY, req);
                reqCache.put(CachingService.HTTP_RESPONSE_KEY, res);
                reqCache.put("locale", req.getLocale());
            } else {
                // store the locale
                reqCache.put("locale", Locale.getDefault());
            }

            // establish the request service startup
            RequestService requestService = kernel.getServiceManager().getServiceByName(RequestService.class.getName(), RequestService.class);
            if (requestService == null) {
                throw new IllegalStateException("Could not get the DSpace RequestService to start the request transaction");
            }
            // establish a request related to the current session
            requestService.startRequest(); // will trigger the various request listeners
            try {
                // invoke the next filter
                chain.doFilter(request, response);

                // ensure we close out the request (happy request)
                requestService.endRequest(null);
            } catch (Exception e) {
                // failure occurred in the request so we destroy it
                requestService.endRequest(e);
                throw new ServletException(e); // rethrow the exception
            }
            /*
        } catch (Exception e) {
            String message = "Failure in the DSpaceWebappServletFilter: " + e.getMessage();
            System.err.println(message);
            if (res != null) {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
            } else {
                throw new ServletException(message, e);
            }*/
        //}
    }

    /**
     * @return the current DSpace kernel or fail
     */
    public DSpaceKernel getKernel() {
        DSpaceKernel kernel = new DSpaceKernelManager().getKernel();
        if (! kernel.isRunning()) {
            throw new IllegalStateException("The DSpace kernel is not running: " + kernel);
        }
        return kernel;
    }

}
