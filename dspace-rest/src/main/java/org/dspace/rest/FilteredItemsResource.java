/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;


import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.SiteService;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.rest.common.ItemFilter;
import org.dspace.rest.common.ItemFilterQuery;
import org.dspace.rest.filter.ItemFilterSet;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/*
 * This class retrieves items by a constructed metadata query evaluated against a set of Item Filters.
 * 
 * @author Terry Brady, Georgetown University
 */
@Path("/filtered-items")
public class FilteredItemsResource extends Resource {
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    private static Logger log = Logger.getLogger(FilteredItemsResource.class);
    
    /**
     * Return instance of collection with passed id. You can add more properties
     * through expand parameter.
     * 
     * @param expand
     *            String in which is what you want to add to returned instance
     *            of collection. Options are: "all", "parentCommunityList",
     *            "parentCommunity", "items", "license" and "logo". If you want
     *            to use multiple options, it must be separated by commas.
     * @param limit
     *            Limit value for items in list in collection. Default value is
     *            100.
     * @param offset
     *            Offset of start index in list of items of collection. Default
     *            value is 0.
     * @param filters
     *            Comma separated list of Item Filters to use to evaluate against
     *            the items in a collection
	 * @param query_field
	 *            List of metadata fields to evaluate in a metadata query.
	 *            Each list value is used in conjunction with a query_op and query_field.
	 * @param query_op
	 *            List of metadata operators to use in a metadata query.
	 *            Each list value is used in conjunction with a query_field and query_field.
	 * @param query_val
	 *            List of metadata values to evaluate in a metadata query.
	 *            Each list value is used in conjunction with a query_value and query_op.
 	 * @param collSel
	 *            List of collections to query.
     * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return instance of collection. It can also return status code
     *         NOT_FOUND(404) if id of collection is incorrect or status code
     *         UNATHORIZED(401) if user has no permission to read collection.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading
     *             (SQLException) or problem with creating
     *             context(ContextException). It is thrown by NOT_FOUND and
     *             UNATHORIZED status codes, too.
     */
	@GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.ItemFilter getItemQuery(@QueryParam("expand") String expand, 
    		@QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@QueryParam("filters") @DefaultValue("is_item,all_filters") String filters,
            @QueryParam("query_field[]") @DefaultValue("dc.title") List<String> query_field,
            @QueryParam("query_op[]") @DefaultValue("exists") List<String> query_op,
            @QueryParam("query_val[]") @DefaultValue("") List<String> query_val,
            @QueryParam("collSel[]") @DefaultValue("") List<String> collSel,
    		@Context HttpHeaders headers, @Context HttpServletRequest request, @Context ServletContext servletContext) {
        org.dspace.core.Context context = null;
        ItemFilterSet itemFilterSet = new ItemFilterSet(filters, true);
        ItemFilter result = itemFilterSet.getAllFiltersFilter();
        try {
            context = createContext();
            if (!configurationService.getBooleanProperty("rest.reporting-authenticate", true)) {
                context.turnOffAuthorisationSystem();            	
            }
            
            int index = Math.min(query_field.size(), Math.min(query_op.size(), query_val.size()));
            List<ItemFilterQuery> itemFilterQueries = new ArrayList<ItemFilterQuery>();
            for(int i=0; i<index; i++){
            	itemFilterQueries.add(new ItemFilterQuery(query_field.get(i), query_op.get(i), query_val.get(i)));
            }

            String regexClause = configurationService.getProperty("rest.regex-clause");
            if (regexClause == null) {
            	regexClause = "";
            }

    		List<UUID> uuids = getUuidsFromStrings(collSel);
    		List<List<MetadataField>> listFieldList = getMetadataFieldsList(context, query_field);    		

            Iterator<org.dspace.content.Item> childItems = itemService.findByMetadataQuery(context, listFieldList, query_op, query_val, uuids, regexClause, offset, limit);
             
            int count = itemFilterSet.processSaveItems(context, servletContext, childItems, true, expand);
    	    writeStats(siteService.findSite(context), UsageEvent.Action.VIEW, user_ip, user_agent, xforwarderfor, headers, request, context);
    	    result.annotateQuery(query_field, query_op, query_val);
    	    result.setUnfilteredItemCount(count);
    	    context.complete();
        } catch (IOException e) {
            processException(e.getMessage(), context);
        } catch (SQLException e) {
        	processException(e.getMessage(), context);
        } catch (AuthorizeException e) {
        	processException(e.getMessage(), context);
        } catch (ContextException e) {
        	processException("Unauthorized filtered item query. " + e.getMessage(), context);
		} finally {
			processFinally(context);
        }
        return result;
    }
	
	private List<List<MetadataField>> getMetadataFieldsList(org.dspace.core.Context context, List<String> query_field) throws SQLException {
		List<List<MetadataField>> listFieldList = new ArrayList<List<MetadataField>>();
		for(String s: query_field) {
			ArrayList<MetadataField> fields = new ArrayList<MetadataField>();
			listFieldList.add(fields);
			if (s.equals("*")) {
				continue;
			}
        	String schema = "";
        	String element = "";
        	String qualifier = null;
        	String[] parts = s.split("\\.");
        	if (parts.length>0) {
        		schema = parts[0];
        	}
        	if (parts.length>1) {
        		element = parts[1];
        	}
        	if (parts.length>2) {
        		qualifier = parts[2];
        	}
        
        	if (Item.ANY.equals(qualifier)) {
    			for(MetadataField mf: metadataFieldService.findFieldsByElementNameUnqualified(context, schema, element)){
        			fields.add(mf);        		    				
    			}
        	} else {
    			MetadataField mf = metadataFieldService.findByElement(context, schema, element, qualifier);
    			if (mf != null) {
        			fields.add(mf);    				
    			}
        	}
		}
		return listFieldList;
	}
	
	private List<UUID> getUuidsFromStrings(List<String> collSel) {
		List<UUID> uuids = new ArrayList<UUID>();
		for(String s: collSel) {
			try {
				uuids.add(UUID.fromString(s));
			} catch (IllegalArgumentException e) {
				log.warn("Invalid collection UUID: " + s);
			}
		}
		return uuids;
	}
    
}
