/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

/**
 * Perform a simple search of the repository. The user provides a simple one
 * field query (the url parameter is named query) and the results are processed.
 * 
 * @author Scott Phillips
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search provider. The
 *             legacy system build upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in use Lucene as backend
 *             for the DSpace search system please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public class SimpleSearch extends AbstractSearch implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.SimpleSearch.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.SimpleSearch.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.SimpleSearch.head");
    
    private static final Message T_search_scope =
        message("xmlui.ArtifactBrowser.SimpleSearch.search_scope");
    
    private static final Message T_full_text_search =
        message("xmlui.ArtifactBrowser.SimpleSearch.full_text_search");
    
    private static final Message T_go =
        message("xmlui.general.go");
    
    /**
     * Add Page metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		
		DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof Collection) || (dso instanceof Community))
        {
	        HandleUtil.buildHandleTrail(dso,pageMeta,contextPath, true);
		} 
		
        pageMeta.addTrail().addContent(T_trail);
    }
    
    /**
     * build the DRI page representing the body of the search query. This
     * provides a widget to generate a new query and list of search results if
     * present.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        String queryString = getQuery();

        // Build the DRI Body
        Division search = body.addDivision("search","primary");
        search.setHead(T_head);
        Division query = search.addInteractiveDivision("general-query",
                "search",Division.METHOD_GET,"secondary search");

        List queryList = query.addList("search-query",List.TYPE_FORM);
        
        if (variableScope())
        {
            Select scope = queryList.addItem().addSelect("scope");
            scope.setLabel(T_search_scope);
            buildScopeList(scope);
        }
        
        Text text = queryList.addItem().addText("query");
        text.setLabel(T_full_text_search);
        text.setValue(queryString);
        
        buildSearchControls(query);
        query.addPara(null, "button-list").addButton("submit").setValue(T_go);

        // Add the result division
        buildSearchResultsDivision(search);

    }

    /**
     * Get the search query from the URL parameter, if none is found the empty
     * string is returned.
     */
    protected String getQuery() throws UIException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String query = decodeFromURL(request.getParameter("query"));
        if (query == null)
        {
            return "";
        }
        return query;
    }

    /**
     * Generate a url to the simple search url.
     */
    protected String generateURL(Map<String, String> parameters)
            throws UIException
    {
	Request request = ObjectModelHelper.getRequest(objectModel);
        
        String scope = request.getParameter("scope");
        if (scope != null)
        {
            parameters.put("scope", scope);
        }
	
        String query = getQuery();
        if (!"".equals(query))
        {
            parameters.put("query", encodeForURL(query));
        }
        
        if (parameters.get("page") == null)
        {
            parameters.put("page", String.valueOf(getParameterPage()));
        }
        
        if (parameters.get("rpp") == null)
        {
            parameters.put("rpp", String.valueOf(getParameterRpp()));
        }
        
        if (parameters.get("sort_by") == null)
        {
            parameters.put("sort_by", String.valueOf(getParameterSortBy()));
        }
        
        if (parameters.get("order") == null)
        {
            parameters.put("order", getParameterOrder());
        }
        
        if (parameters.get("etal") == null)
        {
            parameters.put("etal", String.valueOf(getParameterEtAl()));
        }
        
        return super.generateURL("search", parameters);
    }
}
