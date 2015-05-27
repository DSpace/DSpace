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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.rest.common.Collection;
import org.dspace.rest.common.MetadataEntry;
import org.dspace.rest.common.ItemFilter;
import org.dspace.rest.common.ItemFilterQuery;
import org.dspace.rest.filter.ItemFilterSet;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
The "Path" annotation indicates the URI this class will be available at relative to your base URL.  For
example, if this web-app is launched at localhost using a context of "hello" and no URL pattern is defined
in the web.xml servlet mapping section, then the web service will be available at:

http://localhost:8080/<webapp>/collections
 */
@Path("/filtered-items")
public class FilteredItemsResource {
    private static Logger log = Logger.getLogger(FilteredItemsResource.class);
    
    private enum OP {
        equals("==",true,"="),
        not_equals("!=",false,"="),
        like("like",true," like "),
        not_like("not like",false," like "),
        contains("like",true," like ") {
            public String prepareVal(String val) {
                return "%" + val + "%";
            }
        },
        doesnt_contain("not like",false," like "){
            public String prepareVal(String val) {
                return "%" + val + "%";
            }
        },
        exists(true),
        doesnt_exist(false),
        matches("~",true, "~"),
        doesnt_match("!~",false,"~");
        String disp;
        boolean bexists;
        String valop;
        
        private OP(String disp, boolean exists, String valop) {
            this.disp = disp;
            this.bexists = exists;
            this.valop = valop;
        }
        private OP(boolean exists) {
            this.disp = name();
            this.bexists = exists;
            this.valop = null;
        }
        public String prepareVal(String val) {
            return val;
        }
    }

    @javax.ws.rs.core.Context ServletContext servletContext;
    
    private static final boolean writeStatistics;
    
	static{
		writeStatistics=ConfigurationManager.getBooleanProperty("rest","stats",false);
	}
	
	private void addOp(org.dspace.core.Context context, StringBuilder sb, List<Object> params, String field, OP op, String val) throws SQLException, AuthorizeException {
	    sb.append(" and ");
	    sb.append(op.bexists ? "exists " : "not exists ");
	    sb.append("(select 1 from metadatavalue mv where item.item_id = mv.item_id and mv.metadata_field_id=?");

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
        
        if (op.valop != null) {
            sb.append(" and text_value ");
            sb.append(op.valop);
            sb.append(" ?");
            params.add(op.prepareVal(val));
        }
        sb.append(")");
	}
	
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.ItemFilter getItemQuery(@PathParam("collection_id") Integer collection_id, @QueryParam("expand") String expand, 
    		@QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@QueryParam("filters") @DefaultValue("all_filters") String filters,
            @QueryParam("query_field[]") @DefaultValue("dc.title") List<String> query_field,
            @QueryParam("query_op[]") @DefaultValue("exists") List<String> query_op,
            @QueryParam("query_val[]") @DefaultValue("") List<String> query_val,
            @QueryParam("show_fields[]") @DefaultValue("") List<String> show_fields,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) {
        org.dspace.core.Context context = null;
        try {
            context = new org.dspace.core.Context();

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
                    sb.append(op.bexists ? "(" : "!(");
                    sb.append(query_field.get(i));
                    sb.append(" ");
                    sb.append(op.valop == null ? "exists" : op.valop);
                    sb.append(" ");
                    sb.append(op.prepareVal(val));
                    sb.append(")");
                    
                    addOp(context, sql, params, field, op, val);
                    itemFilterQueries.add(new ItemFilterQuery(field, op.disp, op.prepareVal(val)));
                }
                
                sql.append(" limit ? offset ?");
                params.add(limit);
                params.add(offset);
                
                result.setQueryAnnotation(sb.toString());
                result.setItemFilterQueries(itemFilterQueries);
                if (show_fields != null) {
                    List<MetadataEntry> returnFields = new ArrayList<MetadataEntry>();
                    for(String field: show_fields) {
                        returnFields.add(new MetadataEntry(field, null));
                    }
                    result.setMetadata(returnFields);                    
                }
                TableRowIterator rows = DatabaseManager.queryTable(context, "item", sql.toString(), params.toArray(new Object[0]));
                
                ItemIterator childItems = new ItemIterator(context, rows);
                itemFilterSet.processSaveItems(context, childItems, true, expand);
            } catch(IllegalArgumentException e) {
                result.setQueryAnnotation(e.getMessage());                
            }
            
            return result;
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (AuthorizeException e) {
            log.error(e.getMessage());
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
    
    private void writeStats(org.dspace.core.Context context, Integer collection_id, String user_ip, String user_agent,
			String xforwarderfor, HttpHeaders headers,
			HttpServletRequest request) {
		
    	try{
    		DSpaceObject collection = DSpaceObject.find(context, Constants.COLLECTION, collection_id);
    		
    		if(user_ip==null || user_ip.length()==0){
    			new DSpace().getEventService().fireEvent(
	                     new UsageEvent(
	                                     UsageEvent.Action.VIEW,
	                                     request,
	                                     context,
	                                     collection));
    		} else{
	    		new DSpace().getEventService().fireEvent(
	                     new UsageEvent(
	                                     UsageEvent.Action.VIEW,
	                                     user_ip,
	                                     user_agent,
	                                     xforwarderfor,
	                                     context,
	                                     collection));
    		}
    		log.debug("fired event");
    		
		} catch(SQLException ex){
			log.error("SQL exception can't write usageEvent \n" + ex);
		}
    		
	}
}
