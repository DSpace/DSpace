/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;

import org.apache.log4j.Logger;
import org.dspace.app.util.OpenSearch;
import org.dspace.app.util.SyndicationFeed;
import org.dspace.app.util.Util;
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
        int start = Util.getIntParameter(request, "start");
        int rpp = Util.getIntParameter(request, "rpp");
        int sort = Util.getIntParameter(request, "sort_by");
        String order = request.getParameter("order");
        String sortOrder = (order == null || order.length() == 0 || order.toLowerCase().startsWith("asc")) ?
                         SortOption.ASCENDING : SortOption.DESCENDING;
        
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
        qArgs.setSortOrder(sortOrder);
        
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
        qArgs.setSortOrder(sortOrder);

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
        else
        {
            throw new IllegalStateException("Invalid container for search context");
        }
        
        // now instantiate the results
        DSpaceObject[] results = new DSpaceObject[qResults.getHitHandles().size()];
        for (int i = 0; i < qResults.getHitHandles().size(); i++)
        {
            String myHandle = qResults.getHitHandles().get(i);
            DSpaceObject dso = HandleManager.resolveToObject(context, myHandle);
            if (dso == null)
            {
                throw new SQLException("Query \"" + query
                        + "\" returned unresolvable handle: " + myHandle);
            }
            results[i] = dso;
        }

        // Log
        log.info(LogManager.getHeader(context, "search", logInfo + "query=\"" + query + "\",results=(" + results.length + ")"));

        // format and return results
        Map<String, String> labelMap = getLabels(request);
        Document resultsDoc = OpenSearch.getResultsDoc(format, query, qResults, container, results, labelMap);
        try
        {
            Transformer xf = TransformerFactory.newInstance().newTransformer();
            response.setContentType(OpenSearch.getContentType(format));
            xf.transform(new DOMSource(resultsDoc), new StreamResult(response.getWriter()));
        }
        catch (TransformerException e)
        {
            log.error(e);
            throw new ServletException(e.toString(), e);
        }
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

        labelMap.put(SyndicationFeed.MSG_UNTITLED, labels.getString(msgKey + ".notitle"));
        labelMap.put(SyndicationFeed.MSG_LOGO_TITLE, labels.getString(msgKey + ".logo.title"));
        labelMap.put(SyndicationFeed.MSG_FEED_DESCRIPTION, labels.getString(msgKey + ".general-feed.description"));
        labelMap.put(SyndicationFeed.MSG_UITYPE, SyndicationFeed.UITYPE_JSPUI);
        for (String selector : SyndicationFeed.getDescriptionSelectors())
        {
            labelMap.put("metadata." + selector, labels.getString(SyndicationFeed.MSG_METADATA + selector));
        }
        return labelMap;
    }
}
