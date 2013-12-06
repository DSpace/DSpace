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
import java.util.ArrayList;
import java.util.Map;



import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
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
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Perform an advanced search of the repository. The user is presented with
 * three search parameters, that may be ORed, ANDed, NOTed together.
 * 
 * At the present time only three fields are displayed however if the theme
 * whishes to expand this they can do so by setting the num_search_fields to the
 * desired number of search fields. Also, the theme can change the number of
 * results per the page by setting results_per_page
 * 
 * FIXME: The list of what fields are search should come from a configurable
 * place. Possibly the sitemap configuration.
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

    /** A cache of extracted search fields */
    private java.util.List<AdvancedSearchUtils.SearchField> fields;
    
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
     * Add the body
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String numSearchField = request.getParameter("num_search_field");
        if (numSearchField == null || numSearchField.length() == 0)
        {
            numSearchField = "3";
        }
    	
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

        for (AdvancedSearchUtils.SearchField field : fields)
        {
        	// Skip over all the fields we've displayed.
        	int i = field.getIndex();
        	if (i <= FIELD_DISPLAY_COUNT)
            {
                continue;
            }
        	
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
     * conjunction consists of logical operators AND, OR, NOT.
     *
     * @param row The current row
     * @param cell The current cell
     */
    private void buildConjunctionField(int row, Cell cell) throws WingException
    {
        // No conjunction for the first row.
        if (row == 1)
        {
            return;
        }

        Request request = ObjectModelHelper.getRequest(objectModel);
        String current = request.getParameter("conjunction" + row);

        // default to AND if nothing specified.
        if (current == null || current.length() == 0)
        {
            current = "AND";
        }
        
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

        // Special case ANY
        select.addOption((current == null), "ANY").addContent(
                message("xmlui.ArtifactBrowser.AdvancedSearch.type_ANY"));
        
        ArrayList<String> usedSearchTypes = new ArrayList<String>();
        int i = 1;
        String sindex = ConfigurationManager.getProperty("search.index." + i);
        while(sindex != null)
        {
            String field = sindex.split(":")[0];               
            
            if(! usedSearchTypes.contains(field))
            {
                usedSearchTypes.add(field);
                select.addOption(field.equals(current), field).addContent(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + field));
        }
            
            sindex = ConfigurationManager.getProperty("search.index." + ++i);
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
        String current = decodeFromURL(request.getParameter("query" + row));

        Text text = cell.addText("query" + row);
        if (current != null)
        {
            text.setValue(current);
        }
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
        {
            parameters.put("num_search_field", numSearchField);
        }

        String resultsPerPage = request.getParameter("results_per_page");
        if (resultsPerPage != null)
        {
            parameters.put("results_per_page", resultsPerPage);
        }
        
        String scope = request.getParameter("scope");
        if (scope != null)
        {
            parameters.put("scope", scope);
        }
        
        for (AdvancedSearchUtils.SearchField searchField : getSearchFields(request))
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
        return AdvancedSearchUtils.buildQuery(getSearchFields(request));
    }

    private java.util.List<AdvancedSearchUtils.SearchField> getSearchFields(Request request) throws UIException {
        if (fields == null){
            fields = AdvancedSearchUtils.getSearchFields(request);
        }
        return fields;
    }
}
