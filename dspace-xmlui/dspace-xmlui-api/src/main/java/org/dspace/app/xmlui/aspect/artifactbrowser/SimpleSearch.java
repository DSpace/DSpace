/*
 * SimpleSearch.java
 *
 * Version: $Revision: 1.12 $
 *
 * Date: $Date: 2006/07/27 18:24:34 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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
 * Preform a simple search of the repository. The user provides a simple one
 * field query (the url parameter is named query) and the results are processed.
 * 
 * @author Scott Phillips
 */
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
	        HandleUtil.buildHandleTrail(dso,pageMeta,contextPath);
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
        String query = URLDecode(request.getParameter("query"));
        if (query == null)
            return "";
        return query;
    }

    /**
     * Generate a url to the simple search url.
     */
    protected String generateURL(Map<String, String> parameters)
            throws UIException
    {
        String query = getQuery();
        if (!"".equals(query))
            parameters.put("query", URLEncode(query));
        
        if (parameters.get("page") == null)
        	parameters.put("page", String.valueOf(getParameterPage()));
        
        if (parameters.get("rpp") == null)
        	parameters.put("rpp", String.valueOf(getParameterRpp()));
        
        if (parameters.get("sort_by") == null)
        	parameters.put("sort_by", String.valueOf(getParameterSortBy()));
        
        if (parameters.get("order") == null)
        	parameters.put("order",getParameterOrder());
        
        if (parameters.get("etal") == null)
        	parameters.put("etal",String.valueOf(getParameterEtAl()));
        
        return super.generateURL("search", parameters);
    }
}
