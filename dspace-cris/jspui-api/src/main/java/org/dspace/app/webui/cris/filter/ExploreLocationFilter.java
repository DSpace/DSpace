/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.cris.components.BrowseProcessor;
import org.dspace.app.webui.cris.components.ExploreMapProcessors;
import org.dspace.app.webui.cris.components.ExploreProcessor;
import org.dspace.utils.DSpace;

/**
 * Filter to ensure the correct explore location for browse page is set
 * 
 * @author Andrea Bollini
 * 
 */
public class ExploreLocationFilter implements Filter
{
    /** log4j category */
    private static Logger log = Logger.getLogger(ExploreLocationFilter.class);

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

        Object location = hrequest.getAttribute("location");
        // if a location attribute is already present in the request we don't do anything
        if (location == null) {
        	// is it a search?
        	if (request.getParameter("location") != null) {
        		location = request.getParameter("location"); 
        	}
        	else {
        		// it could be a browse
        		String type = hrequest.getParameter("type");
        		if (type != null) {
	        		ExploreMapProcessors processorsMap = new DSpace().getSingletonService(ExploreMapProcessors.class);
	    			if (processorsMap != null) {
	    				Map<String, List<ExploreProcessor>> procMap = processorsMap.getProcessorsMap();
	    				if (procMap != null) {
	    					external: for (String loc : procMap.keySet()) {
	    						List<ExploreProcessor> procs = procMap.get(loc);
	    						for (ExploreProcessor p : procs) {
	    							if (p instanceof BrowseProcessor) 
	    							{
	    								if (((BrowseProcessor) p).getBrowseNames().contains(type)) {
		    								location = loc;
		    								break external;
	    								}
	    							}
	    						}
	    					}
	    				}
	    			}
        		}
        	}
        	if (location != null) {
        		hrequest.setAttribute("location", location);
        	}
        }
        chain.doFilter(hrequest, hresponse);
    }

    public void destroy()
    {
        // Nothing
    }
}
