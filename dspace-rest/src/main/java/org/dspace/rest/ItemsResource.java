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
import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/19/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/items")
public class ItemsResource {
	
	private static final boolean writeStatistics;
	
	static{
		writeStatistics=ConfigurationManager.getBooleanProperty("rest","stats",false);
	}
	
	 /** log4j category */
    private static final Logger log = Logger.getLogger(ItemsResource.class);
    //ItemList - Not Implemented

    private static org.dspace.core.Context context;

    @GET
    @Path("/{item_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Item getItem(@PathParam("item_id") Integer item_id, @QueryParam("expand") String expand,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException {
    	
    	
        try {
            if(context == null || !context.isValid()) {
                context = new org.dspace.core.Context();
                //Failed SQL is ignored as a failed SQL statement, prevent: current transaction is aborted, commands ignored until end of transaction block
                context.getDBConnection().setAutoCommit(true);
            }

            org.dspace.content.Item item = org.dspace.content.Item.find(context, item_id);

            if(AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
            	if(writeStatistics){
    				writeStats(item_id, user_ip, user_agent, xforwarderfor, headers, request);
    			}
                return new org.dspace.rest.common.Item(item, expand, context);
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

        } catch (SQLException e)  {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    private void writeStats(Integer item_id, String user_ip, String user_agent,
			String xforwarderfor, HttpHeaders headers,
			HttpServletRequest request) {
		
    	try{
    		DSpaceObject item = DSpaceObject.find(context, Constants.ITEM, item_id);
    		
    		if(user_ip==null || user_ip.length()==0){
    			new DSpace().getEventService().fireEvent(
	                     new UsageEvent(
	                                     UsageEvent.Action.VIEW,
	                                     request,
	                                     context,
	                                     item));
    		} else{
	    		new DSpace().getEventService().fireEvent(
	                     new UsageEvent(
	                                     UsageEvent.Action.VIEW,
	                                     user_ip,
	                                     user_agent,
	                                     xforwarderfor,
	                                     context,
	                                     item));
    		}
    		log.debug("fired event");
    		
		} catch(SQLException ex){
			log.error("SQL exception can't write usageEvent \n" + ex);
		}
    		
	}

}
