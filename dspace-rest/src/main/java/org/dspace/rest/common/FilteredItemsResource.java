/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 * GUCODE[[twb27:custom module]]
 */
package org.dspace.rest.common;


import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.rest.common.MetadataEntry;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.rest.common.ItemFilter;
import org.dspace.rest.common.ItemFilterQuery;
import org.dspace.rest.filter.ItemFilterSet;
import org.dspace.rest.filter.OP;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.usage.UsageEvent;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * This class retrieves items by a constructed metadata query evaluated against a set of Item Filters.
 * 
 * @author Terry Brady, Georgetown University
 */
@Path("/filtered-items")
public class FilteredItemsResource extends Resource {
    private static Logger log = Logger.getLogger(FilteredItemsResource.class);
    
    /**
     * Construct a SQL query for item metadata fields
     * @param context
     *     Current DSpace context
     * @param sb
     *     String builder object containing current query
     * @param params
     *     Object containing SQL prepared statement parameters
     * @param field
     *     Field to query
     * @param op
     *     Operation to perform on a field
     * @param val
     *     Value to use in field operation
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void addOp(org.dspace.core.Context context, StringBuilder sb, List<Object> params, String field, OP op, String val) throws SQLException, AuthorizeException {
	    sb.append(" and ");
	    sb.append(op.existsQuery() ? "exists " : "not exists ");
	    sb.append("(select 1 from metadatavalue mv where item.item_id = mv.resource_id ");
	    sb.append("and mv.resource_type_id=2 and mv.metadata_field_id=?");

	    String[] parts = field.split("\\.");
	    String schema =  (parts.length > 0) ? parts[0] : ""; 
        String element =  (parts.length > 1) ? parts[1] : ""; 
        String qualifier =  (parts.length > 2) ? parts[2] : null; 
	    MetadataSchema mds = MetadataSchema.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
        if (mdf == null)
        {
            throw new IllegalArgumentException(
                    "No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
        }
        params.add(mdf.getFieldID());
        
        if (op.getValueOperation() != null) {
            sb.append(" and text_value ");
            sb.append(op.getValueOperation());
            sb.append(" ?");
            params.add(op.prepareVal(val));
        }
        sb.append(")");
	}
	
    /**
     * Construct a SQL query for item unqualified metadata fields
     * @param context
     *     Current DSpace context
     * @param sb
     *     String builder object containing current query
     * @param params
     *     Object containing SQL prepared statement parameters
     * @param field
     *     Unqualified field to query
     * @param op
     *     Operation to perform on a field
     * @param val
     *     Value to use in field operation
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void addUnqualifiedOp(org.dspace.core.Context context, StringBuilder sb, List<Object> params, String field, OP op, String val) throws SQLException, AuthorizeException {
	    sb.append(" and ");
	    sb.append(op.existsQuery() ? "exists " : "not exists ");
	    sb.append("(select 1 from metadatavalue mv " +
	    		"inner join metadatafieldregistry mfr " +
	    		"on mfr.metadata_field_id = mv.metadata_field_id " +
	    		"and mfr.metadata_schema_id = ? " +
	    		"and mfr.element = ? " +
	    		"where item.item_id = mv.resource_id " + 
	    		"and mv.resource_type_id=2 ");

	    String[] parts = field.split("\\.");
	    String schema =  (parts.length > 0) ? parts[0] : ""; 
        String element =  (parts.length > 1) ? parts[1] : ""; 
	    MetadataSchema mds = MetadataSchema.find(context, schema);
        if (mds == null)
        {
            throw new IllegalArgumentException("No such metadata schema: " + schema);
        }
        
        params.add(mds.getSchemaID());
        params.add(element);
        
        if (op.getValueOperation() != null) {
            sb.append(" and text_value ");
            sb.append(op.getValueOperation());
            sb.append(" ?");
            params.add(op.prepareVal(val));
        }
        sb.append(")");
	}

    /**
     * Construct a SQL query for item unqualified metadata fields
     * @param context
     *     Current DSpace context
     * @param sb
     *     String builder object containing current query
     * @param params
     *     Object containing SQL prepared statement parameters
     * @param field
     *     Unqualified field to query
     * @param op
     *     Operation to perform on a field
     * @param val
     *     Value to use in field operation
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void addAnyFieldOp(org.dspace.core.Context context, StringBuilder sb, List<Object> params, OP op, String val) throws SQLException, AuthorizeException {
	    sb.append(" and ");
	    sb.append(op.existsQuery() ? "exists " : "not exists ");
	    sb.append("(select 1 from metadatavalue mv " +
	    		"where item.item_id = mv.resource_id " + 
	    		"and mv.resource_type_id=2 ");
        
        if (op.getValueOperation() != null) {
            sb.append(" and text_value ");
            sb.append(op.getValueOperation());
            sb.append(" ?");
            params.add(op.prepareVal(val));
        }
        sb.append(")");
	}

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
	 * @param show_fields
	 *            List of metadata fields to return with the items retrieved in the metadata query.
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
            @QueryParam("show_fields[]") @DefaultValue("") List<String> show_fields,
            @QueryParam("collSel[]") @DefaultValue("") List<String> collSel,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) {
        org.dspace.core.Context context = null;
        try {
            context = createContext(getUser(headers));
            if (!ConfigurationManager.getBooleanProperty("rest", "rest-reporting-authenticate", false)) {
                context.turnOffAuthorisationSystem();            	
            }

            ItemFilterSet itemFilterSet = new ItemFilterSet(filters, true);
            ItemFilter result = itemFilterSet.getAllFiltersFilter();
            
            StringBuilder sb = new StringBuilder();
            StringBuilder sql = new StringBuilder("select item.* from item where 1=1 ");
            ArrayList<Object> params = new ArrayList<Object>();
            
            List<ItemFilterQuery> itemFilterQueries = new ArrayList<ItemFilterQuery>();
            
            try {                
                for(int i=0; i<query_field.size(); i++) {
                    if (i >= query_op.size()) break;
                    String field = query_field.get(i);
                    OP op = OP.valueOf(query_op.get(i));
                    String val = (i >= query_val.size()) ? "" : query_val.get(i);
                    if (sb.length() > 0) {
                        sb.append(" AND ");
                    }
                    sb.append(op.existsQuery() ? "(" : "!(");
                    sb.append(query_field.get(i));
                    sb.append(" ");
                    sb.append(op.getValueOperation() == null ? "exists" : op.getValueOperation());
                    sb.append(" ");
                    sb.append(op.prepareVal(val));
                    sb.append(")");
                    
                    if (field.equals("*")) {
                    	addAnyFieldOp(context, sql, params, op, val);
                    } else if (field.endsWith(".*")) {
                        addUnqualifiedOp(context, sql, params, field, op, val);
                    } else {
                        addOp(context, sql, params, field, op, val);                    	
                    }
                    itemFilterQueries.add(new ItemFilterQuery(field, op.getDisplay(), op.prepareVal(val)));
                }
                
                result.setQueryAnnotation(sb.toString());
                result.setItemFilterQueries(itemFilterQueries);
                result.initMetadataList(show_fields);
                
                StringBuilder queryList = new StringBuilder();
                for(String coll: collSel) {
                	if (coll.isEmpty()) break;
                	try {
						int collId = Integer.parseInt(coll);
						params.add(collId);
	                	if (queryList.length() == 0) {
	                		queryList.append(" and item.owning_collection in (?");
	                	} else {
	                		queryList.append(",?");
	                	}
					} catch (Exception e) {
						log.warn(e.toString());
						break;
					}
                }
                if (queryList.length() > 0) {
                	queryList.append(")");
                	sql.append(queryList.toString());
                }
                
                sql.append(" limit ? offset ?");
                params.add(limit);
                params.add(offset);
                
                TableRowIterator rows = DatabaseManager.queryTable(context, "item", sql.toString(), params.toArray(new Object[0]));
                
                log.debug(sb.toString());
                
                ItemIterator childItems = new ItemIterator(context, rows);
                
                itemFilterSet.processSaveItems(context, childItems, true, expand);
            } catch(IllegalArgumentException e) {
                result.setQueryAnnotation(e.getMessage());                
            }
            
    	    writeStats(Site.find(context, Site.SITE_ID), UsageEvent.Action.VIEW, user_ip, user_agent, xforwarderfor, headers, request, context);
            return result;
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (AuthorizeException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ContextException e) {
            log.error("Unauthorized filtered item query. " + e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
            if(context != null) {
                try {
                    context.complete();
                } catch (SQLException e) {
                    log.error(e.getMessage() + " occurred while trying to close");
                }
            }
        }
    }
    
}
