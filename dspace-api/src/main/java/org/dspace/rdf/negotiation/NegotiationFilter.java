/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rdf.negotiation;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.rdf.RDFUtil;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class NegotiationFilter implements Filter
{
    public static final String ACCEPT_HEADER_NAME = "Accept";
    
    private static final Logger log = Logger.getLogger(NegotiationFilter.class);
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to todo here.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
            FilterChain chain)
            throws IOException, ServletException
    {
        try
        {
            if (!DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getBooleanProperty(RDFUtil.CONTENT_NEGOTIATION_KEY, false))
            {
                chain.doFilter(request, response);
                return;
            }
        }
        catch (Exception ex)
        {
            log.warn("Will deliver HTML, as I cannot determine if content "
                    + "negotiation should be enabled or not:\n" 
                    + ex.getMessage(), ex);
            chain.doFilter(request, response);
            return;
        }

        if (!(request instanceof HttpServletRequest) 
                || !(response instanceof HttpServletResponse))
        {
            // just pass request and response to the next filter, if we don't
            // have a HttpServletRequest.
            chain.doFilter(request, response);
            return;
        }
        // cast HttpServletRequest and HttpServletResponse
        HttpServletRequest hrequest = (HttpServletRequest) request;
        HttpServletResponse hresponse = (HttpServletResponse) response;
        
        String acceptHeader = hrequest.getHeader(ACCEPT_HEADER_NAME);
        
        String handle = null;
        String extraPathInfo = null;
        String path = hrequest.getPathInfo();
        // in JSPUI the pathInfo starts after /handle, in XMLUI it starts with /handle
        Pattern handleCheckPattern = Pattern.compile("^/*handle/(.*)$");
        Matcher handleCheckMatcher = handleCheckPattern.matcher(path);
        if (handleCheckMatcher.matches())
        {
            // remove trailing /handle
            path = handleCheckMatcher.group(1);
        }
        // we expect the path to be in the form <prefix>/<suffix>/[<stuff>],
        // where <prefix> is a handle prefix, <suffix> is the handle suffix
        // and <stuff> may be further information.
        log.debug("PathInfo: " + path);
        if (path == null) path = "";
        Pattern pathPattern = 
                Pattern.compile("^/*([^/]+)/+([^/]+)(?:/*||/+(.*))?$");
        Matcher pathMatcher = pathPattern.matcher(path);
        if (pathMatcher.matches())
        {
            handle = pathMatcher.group(1) + "/" + pathMatcher.group(2);
            extraPathInfo = pathMatcher.group(3);
        }
        log.debug("handle: " + handle + "\n" + "extraPathInfo: " + extraPathInfo);
        
        int requestedContent = Negotiator.negotiate(acceptHeader);
        
        if (!Negotiator.sendRedirect(hresponse, handle, extraPathInfo, 
                requestedContent, false))
        {
            // as we do content negotiation, we should send a vary caching so
            // browsers can adopt their caching strategy
            // the method Negotiator.sendRedirect does this only if it actually
            // does the redirection itself.
            hresponse.setHeader("Vary", "Accept");

            // send html as default => no forwarding necessary
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // nothing to do here.
    }
}
