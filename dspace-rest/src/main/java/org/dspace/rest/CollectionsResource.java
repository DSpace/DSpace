/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;


import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.rest.common.Collection;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.sql.SQLException;
import java.util.ArrayList;

/*
The "Path" annotation indicates the URI this class will be available at relative to your base URL.  For
example, if this web-app is launched at localhost using a context of "hello" and no URL pattern is defined
in the web.xml servlet mapping section, then the web service will be available at:

http://localhost:8080/<webapp>/collections
 */
@Path("/collections")
public class CollectionsResource {
    private static Logger log = Logger.getLogger(CollectionsResource.class);

    @javax.ws.rs.core.Context ServletContext servletContext;
    
    private static final boolean writeStatistics;
	
	static{
		writeStatistics=ConfigurationManager.getBooleanProperty("rest","stats",false);
	}

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Collection[] list(@QueryParam("expand") String expand, @QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset) {
        org.dspace.core.Context context = null;
        try {
            context = new org.dspace.core.Context();

            org.dspace.content.Collection[] collections;

            //Only support paging if limit/offset are 0 or positive values.
            if(limit != null && limit >= 0 && offset != null && offset >= 0) {
                collections = org.dspace.content.Collection.findAll(context, limit, offset);
            } else {
                collections = org.dspace.content.Collection.findAll(context);
            }

            ArrayList<org.dspace.rest.common.Collection> collectionArrayList = new ArrayList<org.dspace.rest.common.Collection>();
            for(org.dspace.content.Collection collection : collections) {
                if(AuthorizeManager.authorizeActionBoolean(context, collection, org.dspace.core.Constants.READ)) {
                    org.dspace.rest.common.Collection restCollection = new org.dspace.rest.common.Collection(collection, null, context, limit, offset);
                    collectionArrayList.add(restCollection);
                } // Not showing restricted-access collections
            }

            return collectionArrayList.toArray(new org.dspace.rest.common.Collection[0]);

        } catch (SQLException e) {
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

    @GET
    @Path("/{collection_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Collection getCollection(@PathParam("collection_id") Integer collection_id, @QueryParam("expand") String expand, 
    		@QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) {
        org.dspace.core.Context context = null;
        try {
            context = new org.dspace.core.Context();

            org.dspace.content.Collection collection = org.dspace.content.Collection.find(context, collection_id);
            if(AuthorizeManager.authorizeActionBoolean(context, collection, org.dspace.core.Constants.READ)) {
            	if(writeStatistics){
    				writeStats(context, collection_id, user_ip, user_agent, xforwarderfor, headers, request);
    			}
                return new org.dspace.rest.common.Collection(collection, expand, context, limit, offset);
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
        } catch (SQLException e) {
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
