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
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.aspect.discovery.SimpleSearch;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.content.*;
import org.dspace.content.DSpaceObject;
import org.dspace.discovery.*;

/**
 * SearchMetadataExportReader that generates a CSV of search
 * result metadata using MetadataExport.
 */
public class SearchMetadataExportReader extends AbstractReader implements Recyclable
{	
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
    
    
    private static Logger log = Logger.getLogger(SearchMetadataExportReader.class);
    
    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();    
    private HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    private DSpaceCSV csv = null;
    private String filename = null;
    
    private SimpleSearch simpleSearch = null;
    
    /**
     * Set up the export reader.
     * See the class description for information on configuration options.
     *
     * @param resolver source resolver.
     * @param objectModel Cocoon object model.
     * @param src source to read.
     * @param par Reader parameters.
     * @throws org.apache.cocoon.ProcessingException on error.
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.io.IOException passed through.
     */
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par)
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, par);

        try
        {
        	this.request = ObjectModelHelper.getRequest(objectModel);
        	this.response = ObjectModelHelper.getResponse(objectModel);
        	
        	String query = request.getParameter("query");
        	String scope = request.getParameter("scope");
        	String filters = request.getParameter("filters");
        	       	 
            Context context = ContextUtil.obtainContext(objectModel);
            
            String search_export_config = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("xmlui.search.metadata_export");
            
            
            if(search_export_config.equals("admin")) {            	
            	if(!authorizeService.isAdmin(context)) {
                    /*
                     * Auth should be done by MetadataExport -- pass context through
                     * we should just be catching exceptions and displaying errors here
                     */
                    if(AuthenticationUtil.isLoggedIn(request)) {
                    	String redictURL = request.getContextPath() + "/restricted-resource";
                        HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                		httpResponse.sendRedirect(redictURL);
                		return;
                    }
                    else {
                    	String redictURL = request.getContextPath() + "/login";
                        AuthenticationUtil.interruptRequest(objectModel, AUTH_REQUIRED_HEADER, AUTH_REQUIRED_MESSAGE, null);
                        HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                        httpResponse.sendRedirect(redictURL);
                        return;
                    }
                }
            }
            else if(search_export_config.equals("user")) {
            	if(!AuthenticationUtil.isLoggedIn(request)) {            		
                	String redictURL = request.getContextPath() + "/login";
                    AuthenticationUtil.interruptRequest(objectModel, AUTH_REQUIRED_HEADER, AUTH_REQUIRED_MESSAGE, null);
                    HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                    httpResponse.sendRedirect(redictURL);
                    return;
                }
            }
            
            simpleSearch = new SimpleSearch();
            
            csv = exportMetadata(context, objectModel, query, scope, filters);
            filename = "search-results.csv";
            
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
     * @throws java.io.IOException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.apache.cocoon.ProcessingException passed through.
	 */
    @Override
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
    @Override
    public void recycle() {        
        this.response = null;
        this.request = null;
    }
    
    /**
     * Save and return the search results as a CSV file.
     * 
     * @param context session context.
     * @param objectModel Cocoon object model.
     * @param query search parameters.
     * @param scopeString scope of the search.
     * @param filters search filters.
     * @return wrapper for the CSV file.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws org.dspace.discovery.SearchServiceException passed through.
     * @throws java.sql.SQLException passed through.
     */
    public DSpaceCSV exportMetadata(Context context, Map objectModel, String query, String scopeString, String filters)
            throws IOException, UIException, SearchServiceException, SQLException
    {
    	DiscoverResult qResults = new DiscoverResult();
    	
    	DiscoverQuery qArgs = new DiscoverQuery();
    	    	
    	 // Are we in a community or collection?
        DSpaceObject scope;
    	
    	if(scopeString != null && scopeString.length() > 0) {
    		scopeString = scopeString.replace("~", "/");
    		// Get the search scope from the location parameter
    		scope = handleService.resolveToObject(context, scopeString);
    	}
    	else {
    		// get the search scope from the url handle
        	scope = HandleUtil.obtainHandle(objectModel);
    	}
    	
    	
        // set the object model on the simple search object
        simpleSearch.objectModel = objectModel;
        
        String[] fqs = filters != null ? filters.split(",") : new String[0];
        
        // prepare query from SimpleSearch object
        qArgs = simpleSearch.prepareQuery(scope, query, fqs);
                
        // no paging required
        qArgs.setStart(0);
                
        // some arbitrary value for first search
        qArgs.setMaxResults(10);
                
        // search once to get total search results
        qResults = SearchUtils.getSearchService().search(context, scope, qArgs);
                	        	
        // set max results to total search results
        qArgs.setMaxResults(safeLongToInt(qResults.getTotalSearchResults()));        	        	
        
        // search again to return all search results
        qResults = SearchUtils.getSearchService().search(context, scope, qArgs);
        
    	// Get a list of found items
        ArrayList<Item> items = new ArrayList<Item>();        
        for (DSpaceObject resultDSO : qResults.getDspaceObjects()) {
            if (resultDSO instanceof Item) {
                items.add((Item) resultDSO);
            }
        } 
        
        // Log the attempt
        log.info(LogManager.getHeader(context, "metadataexport", "exporting_search"));

        MetadataExport exporter = new MetadataExport(context, items.iterator(), false);
        
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
    
}
