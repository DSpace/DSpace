/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

public class CSVExportDiscoveryNavigation  extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
	private static final Logger log = Logger.getLogger(CSVExportDiscoveryNavigation.class);
	
	private static final Message T_context_head = message("xmlui.administrative.Navigation.context_head");
	private static final Message T_export_metadata = message("xmlui.administrative.Navigation.context_search_export_metadata");
	
	private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
	
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
	 try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            String key = request.getScheme() + request.getServerName() + request.getServerPort() + request.getSitemapURI() + request.getQueryString();
            
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null)
            {
                key += "-" + dso.getHandle();
            }

            return HashUtil.hash(key);
        } 
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * 
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    /**
     * Add the basic navigational options:
     * 
     * Search - advanced search
     * 
     * browse - browse by Titles - browse by Authors - browse by Dates
     * 
     * language FIXME: add languages
     * 
     * context - export metadata if in discover
     * 
     * action no action options are added.
     */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	Context context = ContextUtil.obtainContext(objectModel);
    	Request request = ObjectModelHelper.getRequest(objectModel);
    	
        /* regulate the ordering */
        options.addList("discovery");
        options.addList("browse");
        options.addList("account");
        options.addList("administrative");
                
        // get uri to see if using discovery and if under a specific handle
        String uri = request.getSitemapURI();
        
        // check value in dspace.cfg
        String search_export_config = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("xmlui.search.metadata_export");
        
        // get query
        String query = decodeFromURL(request.getParameter("query"));
                
        // get scope, if not under handle returns null
        String scope= request.getParameter("scope");
        
        // used to serialize all query filters together
    	String filters = "";
    	
    	// get all query filters
    	String[] fqs = DiscoveryUIUtils.getFilterQueries(ObjectModelHelper.getRequest(objectModel), context);
        
    	if (fqs != null)
        {
        	for(int i = 0; i < fqs.length; i++) {
            	if(i < fqs.length - 1)
            		filters += fqs[i] + ",";
            	else
            		filters += fqs[i];
            }
        }
    	
    	// check scope
    	if(isEmpty(scope)) scope = "/";
    	
    	// check query
    	if(isEmpty(query)) query = "*";
    	
    	// check if under a handle, already in discovery        	
    	if(uri.contains("handle")) {
    		scope = uri.replace("handle/", "").replace("/discover", "");
        }
    	
    	// replace forward slash to make query parameter safe
    	try {
        	scope = scope.replace("/", "~");
        }
        catch(NullPointerException e) { }
    	
    	if(search_export_config != null) {
    		// some logging    		
			log.info("uri: " + uri);
			log.info("query: " + query);
			log.info("scope: " + scope);
			log.info("filters: " + filters);
			
			boolean show = false;
    		        		
    		if(search_export_config.equals("admin")) {
    			if(authorizeService.isAdmin(context)) {
    				show = true;
    			}
    		}
    		else if(search_export_config.equals("user") || search_export_config.equals("anonymous")){
    			show = true;
    		}
    		
    		if(show) {
    			List results = options.addList("context");    		
            	results.setHead(T_context_head);
            	String link = contextPath + "/discover/search/csv?query=" + query + "&scope=" + scope;
            	if(!isEmpty(filters)) {
            		link += "&filters=" + filters;
            	}
            	results.addItem().addXref(link, T_export_metadata);
    		}
    	}
    }
    
    private boolean isEmpty(String str) {
    	return str == null || str.length() == 0;
    }

    /**
     * Ensure that the context path is added to the page meta.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Add metadata for quick searches:
        pageMeta.addMetadata("search", "simpleURL").addContent("/discover");
        pageMeta.addMetadata("search", "advancedURL").addContent(contextPath + "/discover");
        pageMeta.addMetadata("search", "queryField").addContent("query");
    }
}
