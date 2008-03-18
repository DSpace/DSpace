/*
 * AdvancedSearch
 *
 * Version: $Revision: 1.14 $
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.oro.text.perl.Perl5Util;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

/**
 * Preform an advanced search of the repository. The user is presented with
 * three search parameters, that may be ORed, ANDed, NOTed together.
 * 
 * At the present time only three fields are displayed however if the theme
 * whishes to expand this they can by setting the num_search_fields to the
 * desired number of search fields. Also the theme can change the number of
 * results per the page by setting results_per_page
 * 
 * FIXME: The list of what fields are search should come from a configurable
 * place. Possibily the sitemap configuration.
 * 
 * @author Scott Phillips
 */
public class AdvancedSearch extends AbstractSearch implements CacheableProcessingComponent
{
    /** Language string used: */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.AdvancedSearch.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.head");
    
    private static final Message T_search_scope = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.search_scope");
    
    private static final Message T_search_scope_help = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.search_scope_help");
    
    private static final Message T_conjunction = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.conjunction");
    
    private static final Message T_search_type =
        message("xmlui.ArtifactBrowser.AdvancedSearch.search_type");
    
    private static final Message T_search_for = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.search_for");
    
    private static final Message T_go = 
        message("xmlui.general.go");

    private static final Message T_and = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.and");
    
    private static final Message T_or = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.or");
    
    private static final Message T_not = 
        message("xmlui.ArtifactBrowser.AdvancedSearch.not");
    
    
    
    /** How many conjunction fields to display */
    private static final int FIELD_DISPLAY_COUNT = 3;
    private static final int FIELD_MAX_COUNT = 12;
    
    /** A cache of extracted search fields */
    private ArrayList<SearchField> fields;
    
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
     * Add the body
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String numSearchField = request.getParameter("num_search_field");
        if (numSearchField == null || numSearchField.length() == 0)
        	numSearchField = "3";
    	
        // Build the DRI Body
        Division search = body.addDivision("advanced-search","primary");
        search.setHead(T_head);
        Division query = search.addInteractiveDivision("search-query",
                "advanced-search",Division.METHOD_POST,"secondary search");
        
        // Use these fields to change the number of search fields, or change the results per page.
        query.addHidden("num_search_field").setValue(numSearchField);
        query.addHidden("results_per_page").setValue(getParameterRpp());
        
        List queryList = query.addList("search-query",List.TYPE_FORM);
        
        if (variableScope())
        {
            Select scope = queryList.addItem().addSelect("scope");
            scope.setLabel(T_search_scope);
            scope.setHelp(T_search_scope_help);
            buildScopeList(scope);
        }
        
        Table queryTable = query.addTable("search-query", 4, 3);
        Row header = queryTable.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_conjunction);
        header.addCellContent(T_search_type);
        header.addCellContent(T_search_for);
        
        for (int i = 1; i <= FIELD_DISPLAY_COUNT; i++)
        {
            Row row = queryTable.addRow(Row.ROLE_DATA);
            buildConjunctionField(i, row.addCell());
            buildTypeField(i, row.addCell());
            buildQueryField(i, row.addCell());
        }

        for (SearchField field : fields)
        {
        	// Skip over all the fields we've displayed.
        	int i = field.getIndex();
        	if (i <= FIELD_DISPLAY_COUNT)
        		continue;
        	
        	query.addHidden("conjunction"+i).setValue(field.getConjunction());
        	query.addHidden("field"+i).setValue(field.getField());
        	query.addHidden("query"+i).setValue(field.getQuery());
        }

        buildSearchControls(query);
        query.addPara(null, "button-list").addButton("submit").setValue(T_go);
        
        // Add the result division
        buildSearchResultsDivision(search);

    }

    /**
     * Build a conjunction field in the given for the given cell. A 
     * conjunction consists of logical the operators AND, OR, NOT.
     *
     * @param row The current row
     * @param cell The current cell
     */
    private void buildConjunctionField(int row, Cell cell) throws WingException
    {
        // No conjunction for the first row.
        if (row == 1)
            return;

        Request request = ObjectModelHelper.getRequest(objectModel);
        String current = request.getParameter("conjunction" + row);

        // default to AND if nothing specified.
        if (current == null || current.length() == 0)
            current = "AND";
        
        Select select = cell.addSelect("conjunction" + row);

        select.addOption("AND".equals(current), "AND").addContent(T_and);
        select.addOption("OR".equals(current), "OR").addContent(T_or);
        select.addOption("NOT".equals(current), "NOT").addContent(T_not);
    }

    /**
     * Build a list of all the indexable fields in the given cell.
     * 
     * FIXME: This needs to use the dspace.cfg data
     * 
     * @param row The current row
     * @param cell The current cell
     */
    private void buildTypeField(int row, Cell cell) throws WingException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String current = request.getParameter("field" + row);
        
        Select select = cell.addSelect("field" + row);

        // FIXME: this needs to come from a configurable source.
        Map<String, Message> searchTypes = new HashMap<String, Message>();
        searchTypes.put("author", 
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_author"));
        searchTypes.put("title", 
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_title"));
        searchTypes.put("keyword", 
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_subject"));
        searchTypes.put("abstract", 
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_abstract"));
        searchTypes.put("series", 
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_series"));
        searchTypes.put("sponsor", 
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_sponsor"));
        searchTypes.put("identifier", 
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_identifier"));
        searchTypes.put("language", 
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_language"));

        // Special case ANY
        select.addOption((current == null), "ANY").addContent(
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_keyword"));

        for (String key : searchTypes.keySet())
        {

            select.addOption(key.equals(current), key).addContent(
                    searchTypes.get(key));
        }
    }

    
    /**
     * Recycle
     */
    public void recycle() 
    {
        this.fields = null;
        super.recycle();
    }
    
    
    
    /**
     * Build the query field for the given cell.
     * 
     * @param row The current row.
     * @param cell The current cell.
     */
    private void buildQueryField(int row, Cell cell) throws WingException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String current = URLDecode(request.getParameter("query" + row));

        Text text = cell.addText("query" + row);
        if (current != null)
            text.setValue(current);
    }

    /**
     * Generate a URL for this search page which includes all the 
     * search parameters along with the added parameters.
     * 
     * @param parameters URL parameters to be included in the generated url.
     */
    protected String generateURL(Map<String, String> parameters)
            throws UIException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        String numSearchField = request.getParameter("num_search_field");
        if (numSearchField != null)
        	parameters.put("num_search_field", numSearchField);

        String resultsPerPage = request.getParameter("results_per_page");
        if (resultsPerPage != null)
        	parameters.put("results_per_page", resultsPerPage);
        
        String scope = request.getParameter("scope");
        if (scope != null)
        	parameters.put("scope", scope);
        
        for (SearchField searchField : getSearchFields(request))
        {
        	int index = searchField.getIndex();
        	String field = searchField.getField();
        	String query = searchField.getQuery();
        	String conjunction = searchField.getConjunction();
        	
            parameters.put("conjunction" + index, conjunction);
            parameters.put("field" + index, field);
            parameters.put("query" + index, query);
        }
        
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
        
        return super.generateURL("advanced-search", parameters);
    }

    /**
     * Determine the search query for this search page.
     * 
     * @return the query.
     */
    protected String getQuery() throws UIException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);   
        return buildQuery(getSearchFields(request));
    }
    
    
    /**
     * Given a list of search fields buld a lucene search query string.
     * 
     * @param fields The search fields
     * @return A string
     */
    private String buildQuery(ArrayList<SearchField> fields)
    {
    	Perl5Util util = new Perl5Util();
    	
    	String query = "";
    	
    	// Loop through the fields building the search query as we go.
    	for (SearchField field : fields)
    	{	
    		// if the field is empty, then skip it and try a later one.
    		if (field.getQuery() == null)
    			continue;
    		
    		// Add the conjunction for everything but the first field.
    		if (fields.indexOf(field) > 0)
    			query += " " + field.getConjunction() + " ";
            
    		// Two cases, one if a specific search field is specified or if 
    		// ANY is given then just a general search is performed.
            if ("ANY".equals(field.getField()))
            {
            	// No field specified, 
            	query += "(" + field.getQuery() + ")";
            }
            else
            {   
            	// Specific search field specified, add the field specific field.
            	
            	// Replace singe quote's with double quotes (only if they match)
            	String subquery = util.substitute("s/\'(.*)\'/\"$1\"/g", field.getQuery());
            	
            	// If the field is not quoted ...
            	if (!util.match("/\".*\"/", subquery))
                {
            		// ... then seperate each word and re-specify the search field.
                    subquery = util.substitute("s/ / " + field.getField() + ":/g", subquery);
                }
            	
            	// Put the subquery into the general query
            	query += "("+field.getField()+":"+subquery+")";
            }
    	}
    	
    	if (query.length() == 0)
    		return "";
    	else
    		return "("+query+")";
    }

   
    /**
     * Get a list of search fields from the request object
     * and parse them into a linear array of fileds. The field's
     * index is preserved, so if it comes in as index 17 it will 
     * be outputted as field 17.
     * 
     * @param request The http request object
     * @return Array of search fields
     * @throws UIException 
     */
    public ArrayList<SearchField> getSearchFields(Request request) throws UIException
	{
    	if (this.fields != null)
    		return this.fields;
    	
    	// Get how many fields to search
	    int numSearchField;
	    try {
	    	String numSearchFieldStr = request.getParameter("num_search_field");
	    	numSearchField = Integer.valueOf(numSearchFieldStr);
	    } 
	    catch (NumberFormatException nfe)
	    {
	    	numSearchField = FIELD_MAX_COUNT;
	    }
	    	
    	// Iterate over all the possible fields and add each one to the list of fields.
		ArrayList<SearchField> fields = new ArrayList<SearchField>();
		for (int i = 1; i <= numSearchField; i++)
		{
			String field = request.getParameter("field"+i);
			String query = URLDecode(request.getParameter("query"+i));
			String conjunction = request.getParameter("conjunction"+i);
			
			if (field != null)
			{
				field = field.trim();
				if (field.length() == 0)
					field = null;
			}
			
			
			if (query != null)
			{
				query = query.trim();
				if (query.length() == 0)
					query = null;
			}
			
			if (conjunction != null)
			{
				conjunction = conjunction.trim();
				if (conjunction.length() == 0)
					conjunction = null;
			}
			
			if (field == null)
				field = "ANY";
			if (conjunction == null)
				conjunction = "AND";
			
			if (query != null)
				fields.add(new SearchField(i,field,query,conjunction));
		}
		
		this.fields = fields;
		
		return this.fields;
	}
    
    /**
     * A private record keeping class to relate the various 
     * components of a search field together.
     */
    private static class SearchField {
    	
    	/** What index the search field is, typicaly there are just three - but the theme may exand this number */
    	private int index;
    	
    	/** The field to search, ANY if none specified */
    	private String field;
    	
    	/** The query string to search for */
    	private String query;
    	
    	/** the conjunction: either "AND" or "OR" */
    	private String conjuction;
    	
    	public SearchField(int index, String field, String query, String conjunction)
    	{
    		this.index = index;
    		this.field = field;
    		this.query = query;
    		this.conjuction = conjunction;
    	}
    	
    	public int    getIndex() { return this.index;}
    	public String getField() { return this.field;}
    	public String getQuery() { return this.query;}
    	public String getConjunction() { return this.conjuction;} 
    }
    
}
