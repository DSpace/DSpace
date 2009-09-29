/*
 * OpenSearchServlet.java
 *
 * Version: $Revision: 1.20 $
 *
 * Date: $Date: 2005/08/25 17:20:27 $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved. 
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
 
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.OpenSearch;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;
import org.dspace.sort.SortOption;

/**
 * Servlet for producing OpenSearch-compliant search results, and the
 * OpenSearch description document.
 * <p>
 * OpenSearch is a specification for describing and advertising search-engines
 * and their result formats. Commonly, RSS and Atom formats are used, which
 * the current implementation supports, as is HTML (used directly in browsers).
 * NB: this is baseline OpenSearch, no extensions currently supported.
 * </p>
 * <p>
 * The value of the "scope" parameter should be absent (which means no
 * scope restriction), or the handle of a community or collection, otherwise
 * parameters exactly match those of the SearchServlet.
 * </p>
 * 
 * @author Richard Rodgers
 *
 */
public class OpenSearchServlet extends DSpaceServlet
{
	private static final long serialVersionUID = 1L;
	private static String msgKey = "org.dspace.app.webui.servlet.FeedServlet";
	/** log4j category */
    private static Logger log = Logger.getLogger(OpenSearchServlet.class);
    // locale-sensitive metadata labels
    private Map<String, Map<String, String>> localeLabels = null;
    
    public void init()
    {
    	localeLabels = new HashMap<String, Map<String, String>>();
    }
    
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {	     
        // dispense with simple service document requests
    	String scope = request.getParameter("scope");
    	if (scope !=null && "".equals(scope))
    	{
    		scope = null;
    	}
        String path = request.getPathInfo();
        if (path != null && path.endsWith("description.xml"))
        {
        	String svcDescrip = OpenSearch.getDescription(scope);
        	response.setContentType(OpenSearch.getContentType("opensearchdescription"));
        	response.setContentLength(svcDescrip.length());
        	response.getWriter().write(svcDescrip);
        	return;	
      	}
        
        // get enough request parameters to decide on action to take
        String format = request.getParameter("format");
        if (format == null || "".equals(format))
        {
        	// default to atom
        	format = "atom";
        }
        
        // do some sanity checking
        if (! OpenSearch.getFormats().contains(format))
        {
        	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        	return;
        }        
        
        // then the rest - we are processing the query
        String query = request.getParameter("query");
        int start = UIUtil.getIntParameter(request, "start");
        int rpp = UIUtil.getIntParameter(request, "rpp");
        int sort = UIUtil.getIntParameter(request, "sort_by");
        
        QueryArgs qArgs = new QueryArgs();       
        // can't start earlier than 0 in the results!
        if (start < 0)
        {
            start = 0;
        }
        qArgs.setStart(start);
        
        if (rpp > 0)
        {
            qArgs.setPageSize(rpp);
        }
        
        if (sort > 0)
        {
        	try
        	{
        		qArgs.setSortOption(SortOption.getSortOption(sort));
        	}
        	catch(Exception e)
        	{
        		// invalid sort id - do nothing
        	}
        }

        // Ensure the query is non-null
        if (query == null)
        {
            query = "";
        }

        // If there is a scope parameter, attempt to dereference it
        // failure will only result in its being ignored
        DSpaceObject container = (scope != null) ? HandleManager.resolveToObject(context, scope) : null;

        // Build log information
        String logInfo = "";

        // get the start of the query results page
        qArgs.setQuery(query);

        // Perform the search
        QueryResults qResults = null;
        if (container == null)
        {
        	qResults = DSQuery.doQuery(context, qArgs);
        }
        else if (container instanceof Collection)
        {
            logInfo = "collection_id=" + container.getID() + ",";
            qResults = DSQuery.doQuery(context, qArgs, (Collection)container);
        }
        else if (container instanceof Community)
        {
            logInfo = "community_id=" + container.getID() + ",";
            qResults = DSQuery.doQuery(context, qArgs, (Community)container);
        }
        
        // now instantiate the results
        DSpaceObject[] results = new DSpaceObject[qResults.getHitHandles().size()];
        for (int i = 0; i < qResults.getHitHandles().size(); i++)
        {
            String myHandle = (String)qResults.getHitHandles().get(i);
            DSpaceObject dso = HandleManager.resolveToObject(context, myHandle);
            if (dso == null)
            {
                throw new SQLException("Query \"" + query
                        + "\" returned unresolvable handle: " + myHandle);
            }
            results[i] = dso;
        }

        // Log
        log.info(LogManager.getHeader(context, "search", logInfo + "query=\""
                + query + "\",results=(" + results.length + ")"));
        
        // format and return results
        Map<String, String> labelMap = getLabels(request); 
        String resultStr = OpenSearch.getResultsString(format, query, qResults, container, results, labelMap);
        response.setContentType(OpenSearch.getContentType(format));
        response.setContentLength(resultStr.length());
        response.getWriter().write(resultStr);
    }
    
    private Map<String, String> getLabels(HttpServletRequest request)
    {
        // Get access to the localized resource bundle
        Locale locale = request.getLocale();
        Map<String, String> labelMap = localeLabels.get(locale.toString());
        if (labelMap == null)
        {
        	labelMap = getLocaleLabels(locale);
        	localeLabels.put(locale.toString(), labelMap);
        }
        return labelMap;
    }
    
    private Map<String, String> getLocaleLabels(Locale locale)
    {
    	Map<String, String> labelMap = new HashMap<String, String>();
        ResourceBundle labels = ResourceBundle.getBundle("Messages", locale);
        labelMap.put("notitle", labels.getString(msgKey + ".notitle"));
        labelMap.put("logo.title", labels.getString(msgKey + ".logo.title"));
        labelMap.put("general-feed.description", labels.getString(msgKey + ".general-feed.description"));
        for (String selector : OpenSearch.getDescriptionSelectors())
        {
        	labelMap.put("metadata." + selector, labels.getString(msgKey + ".metadata." + selector));
        }
        labelMap.put("uitype", "jspui");
        return labelMap;
    }
}
