/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map;
import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.reading.AbstractReader;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.MetadataExport;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.aspect.discovery.AbstractSearch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.handle.HandleManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.ItemIterator;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;


/**
 *
 * SearchMetadataExportReader that generates a CSV of search
 * result metadata using MetadataExport
 *
 */

public class SearchMetadataExportReader extends AbstractReader implements Recyclable
{
	private static Logger log = Logger.getLogger(MetadataExportReader.class);
	
     /**
     * Messages to be sent when the user is not authorized to view 
     * a particular bitstream. They will be redirected to the login
     * where this message will be displayed.
     */
	private static final String AUTH_REQUIRED_HEADER = "xmlui.ItemExportDownloadReader.auth_header";
	private static final String AUTH_REQUIRED_MESSAGE = "xmlui.ItemExportDownloadReader.auth_message";
	
    /**
     * How big a buffer should we use when reading from the bitstream before
     * writing to the HTTP response?
     */
    protected static final int BUFFER_SIZE = 8192;

    /**
     * When should a download expire in milliseconds. This should be set to
     * some low value just to prevent someone hitting DSpace repeatedly from
     * killing the server. Note: there are 60000 milliseconds in a minute.
     * 
     * Format: minutes * seconds * milliseconds
     */
    protected static final int expires = 60 * 60 * 60000;

    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;

    DSpaceCSV csv = null;
    String filename = null;
    
    /**
     * Set up the export reader.
     * 
     * See the class description for information on configuration options.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, par);

        try
        {
        	this.request = ObjectModelHelper.getRequest(objectModel);
        	this.response = ObjectModelHelper.getResponse(objectModel);
        	
        	String query = par.getParameter("query");
        	String scope = par.getParameter("scope");
        	String filters = par.getParameter("filters");
        	        	 
            Context context = ContextUtil.obtainContext(objectModel);
            
            String search_export_config = ConfigurationManager.getProperty("xmlui.search.metadata_export");
            
            if(search_export_config.equals("admin")) {            	
            	if(AuthorizeManager.isAdmin(context)) {
                	csv = exportMetadata(context, objectModel, query, scope, filters);
                    filename = "search-results.csv";
                }
                else {                	
                    /*
                     * Auth should be done by MetadataExport -- pass context through
                     * we should just be catching exceptions and displaying errors here
                     */
                    if(AuthenticationUtil.isLoggedIn(request)) {
                    	String redictURL = request.getContextPath() + "/restricted-resource";
                        HttpServletResponse httpResponse = (HttpServletResponse)
                		objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                		httpResponse.sendRedirect(redictURL);
                		return;
                    }
                    else {
                    	String redictURL = request.getContextPath() + "/login";
                        AuthenticationUtil.interruptRequest(objectModel, AUTH_REQUIRED_HEADER, AUTH_REQUIRED_MESSAGE, null);
                        HttpServletResponse httpResponse = (HttpServletResponse)objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                        httpResponse.sendRedirect(redictURL);
                        return;
                    }
                }
            }
            else if(search_export_config.equals("user")) {
            	if(AuthenticationUtil.isLoggedIn(request)) {
            		csv = exportMetadata(context, objectModel, query, scope, filters);
                    filename = "search-results.csv";
                }
                else {
                	String redictURL = request.getContextPath() + "/login";
                    AuthenticationUtil.interruptRequest(objectModel, AUTH_REQUIRED_HEADER, AUTH_REQUIRED_MESSAGE, null);
                    HttpServletResponse httpResponse = (HttpServletResponse)objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                    httpResponse.sendRedirect(redictURL);
                    return;
                }
            }
            else if(search_export_config.equals("anonymous")) {
            	csv = exportMetadata(context, objectModel, query, scope, filters);
                filename = "search-results.csv";
            }
        }
        catch (RuntimeException e) {
            throw e;    
        }
        catch (IOException e) {
        	throw new ProcessingException("Unable to export metadata.",e);
        }
        catch (Exception e) {
            throw new ProcessingException("Unable to read bitstream.",e);
        } 
    }
    
    /**
	 * Write the CSV.
	 */
    public void generate() throws IOException, SAXException, ProcessingException
    {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=" + filename);
        out.write(csv.toString().getBytes("UTF-8"));
        out.flush();
        out.close();
    }
    
    /**
	 * Recycle
	 */
    public void recycle() {        
        this.response = null;
        this.request = null;
    }
    
    /**
     * Save and return the search results as a csv file
     * 
     * @params context, objectModel, query, scopeString, filters
     * 
     * @throws IOException, UIException, SearchServiceException, SQLException
     */
    public DSpaceCSV exportMetadata(Context context, Map objectModel, String query, String scopeString, String filters) throws IOException, UIException, SearchServiceException, SQLException
    {
    	DiscoverResult qResults = new DiscoverResult();
    	DiscoverQuery qArgs = new DiscoverQuery();
        
    	try {
    		scopeString = scopeString.replace("~", "/");
        }
        catch(NullPointerException e) { }
    	
        // Are we in a community or collection?
        DSpaceObject scope;
        if (scopeString == null || "".equals(scopeString)) {
            // get the search scope from the url handle
        	scope = HandleUtil.obtainHandle(objectModel);
        }
        else {
            // Get the search scope from the location parameter
        	scope = HandleManager.resolveToObject(context, scopeString);
        }
        
    	List<String> filterQueries = new ArrayList<String>();

        String[] fqs = filters.split(",");
        
        if (fqs != null) {
            filterQueries.addAll(Arrays.asList(fqs));   
        }
        
        // some arbitrary value for first search
        qArgs.setMaxResults(10);

        //Add the configured default filter queries
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(scope);
        List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
        qArgs.addFilterQueries(defaultFilterQueries.toArray(new String[defaultFilterQueries.size()]));

        if (filterQueries.size() > 0) {
        	qArgs.addFilterQueries(filterQueries.toArray(new String[filterQueries.size()]));
        }

        String sortBy = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
        DiscoverySortConfiguration searchSortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
        if(sortBy == null) {
            //Attempt to find the default one, if none found we use SCORE
            sortBy = "score";
            if(searchSortConfiguration != null) {
                for (DiscoverySortFieldConfiguration sortFieldConfiguration : searchSortConfiguration.getSortFields()) {
                    if(sortFieldConfiguration.equals(searchSortConfiguration.getDefaultSort())) {
                        sortBy = SearchUtils.getSearchService().toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());
                    }
                }
            }
        }
        
        String sortOrder = ObjectModelHelper.getRequest(objectModel).getParameter("order");
        if(sortOrder == null && searchSortConfiguration != null) {
            sortOrder = searchSortConfiguration.getDefaultSortOrder().toString();
        }

        if (sortOrder == null || sortOrder.equalsIgnoreCase("DESC")) {
        	qArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.desc);
        }
        else {
        	qArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.asc);
        }

        String groupBy = ObjectModelHelper.getRequest(objectModel).getParameter("group_by");

        // Enable groupBy collapsing if designated
        if (groupBy != null && !groupBy.equalsIgnoreCase("none")) {
            /** Construct a Collapse Field Query */
        	qArgs.addProperty("collapse.field", groupBy);
        	qArgs.addProperty("collapse.threshold", "1");
        	qArgs.addProperty("collapse.includeCollapsedDocs.fl", "handle");
        	qArgs.addProperty("collapse.facet", "before");

            //queryArgs.a  type:Article^2

            // TODO: This is a hack to get Publications (Articles) to always be at the top of Groups.
            // TODO: I think that can be more transparently done in the solr solrconfig.xml with DISMAX and boosting
            /** sort in groups to get publications to top */
        	qArgs.setSortField("dc.type", DiscoverQuery.SORT_ORDER.asc);
        }
        
        qArgs.setQuery(query != null && !query.trim().equals("") ? query : null);

        // no paging required
        qArgs.setStart(0);

        if(discoveryConfiguration.getHitHighlightingConfiguration() != null) {
            List<DiscoveryHitHighlightFieldConfiguration> metadataFields = discoveryConfiguration.getHitHighlightingConfiguration().getMetadataFields();
            for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : metadataFields) {
            	qArgs.addHitHighlightingField(new DiscoverHitHighlightingField(fieldConfiguration.getField(), fieldConfiguration.getMaxSize(), fieldConfiguration.getSnippets()));
            }
        }
        
        qArgs.setSpellCheck(discoveryConfiguration.isSpellCheckEnabled());
        
        // search once to get total search results
        qResults = SearchUtils.getSearchService().search(context, scope, qArgs);
                	        	
        // set max results to total search results
        qArgs.setMaxResults(safeLongToInt(qResults.getTotalSearchResults()));        	        	
        
        // search again to return all search results
        qResults = SearchUtils.getSearchService().search(context, scope, qArgs);
        
    	Item[] resultsItems;
        
    	// Get a list of found items
        ArrayList<Item> items = new ArrayList<Item>();        
        for (DSpaceObject resultDSO : qResults.getDspaceObjects()) {
            if (resultDSO instanceof Item) {
                items.add((Item) resultDSO);
            }
        }        
        resultsItems = new Item[items.size()];
        resultsItems = items.toArray(resultsItems);        
        
        // Log the attempt
        log.info(LogManager.getHeader(context, "metadataexport", "exporting_search"));
        
        // Export a search view
        ArrayList iids = new ArrayList();
        for (Item item : items) {
            iids.add(item.getID());
        }
        ItemIterator ii = new ItemIterator(context, iids);
        MetadataExport exporter = new MetadataExport(context, ii, false);        
        
        // Perform the export
        DSpaceCSV csv = exporter.export();        
        log.info(LogManager.getHeader(context, "metadataexport", "exported_file:search-results.csv"));
        
        return csv;
    }
    
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int.");
        }
        return (int) l;
    }
        
    /**
     * Returns a list of the filter queries for use in rendering pages, creating page more urls, ....
     * @return an array containing the filter queries
     */
    protected Map<String, String[]> getParameterFilterQueries()
    {
        try {
            Map<String, String[]> result = new HashMap<String, String[]>();
            result.put("fq", ObjectModelHelper.getRequest(objectModel).getParameterValues("fq"));
            return result;
        }
        catch (Exception e) {
            return null;
        }
    }
}
