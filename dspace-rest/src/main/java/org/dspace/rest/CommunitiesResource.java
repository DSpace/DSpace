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

http://localhost:8080/<webapp>/communities
 */
@Path("/communities")
public class CommunitiesResource {
    private static Logger log = Logger.getLogger(CommunitiesResource.class);

    private static final boolean writeStatistics;
	
	static{
		writeStatistics=ConfigurationManager.getBooleanProperty("rest","stats",false);
	}

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Community[] list(@QueryParam("expand") String expand) {
        org.dspace.core.Context context = null;
        try {
            context = new org.dspace.core.Context();

            org.dspace.content.Community[] topCommunities = org.dspace.content.Community.findAllTop(context);
            ArrayList<org.dspace.rest.common.Community> communityArrayList = new ArrayList<org.dspace.rest.common.Community>();
            for(org.dspace.content.Community community : topCommunities) {
                if(AuthorizeManager.authorizeActionBoolean(context, community, org.dspace.core.Constants.READ)) {
                    //Only list communities that this user has access to.
                    org.dspace.rest.common.Community restCommunity = new org.dspace.rest.common.Community(community, expand, context);
                    communityArrayList.add(restCommunity);
                }
            }

            return communityArrayList.toArray(new org.dspace.rest.common.Community[0]);

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
    @Path("/{community_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Community getCommunity(@PathParam("community_id") Integer community_id, @QueryParam("expand") String expand,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) {
        org.dspace.core.Context context = null;
        try {
            context = new org.dspace.core.Context();

            org.dspace.content.Community community = org.dspace.content.Community.find(context, community_id);
            if(AuthorizeManager.authorizeActionBoolean(context, community, org.dspace.core.Constants.READ)) {
            	if(writeStatistics){
    				writeStats(context, community_id, user_ip, user_agent, xforwarderfor, headers, request);
    			}
                return new org.dspace.rest.common.Community(community, expand, context);
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
    
    private void writeStats(org.dspace.core.Context context, Integer community_id, String user_ip, String user_agent,
 			String xforwarderfor, HttpHeaders headers,
 			HttpServletRequest request) {
 		
     	try{
     		DSpaceObject community = DSpaceObject.find(context, Constants.COMMUNITY, community_id);
     		
     		if(user_ip==null || user_ip.length()==0){
     			new DSpace().getEventService().fireEvent(
 	                     new UsageEvent(
 	                                     UsageEvent.Action.VIEW,
 	                                     request,
 	                                     context,
 	                                    community));
     		} else{
 	    		new DSpace().getEventService().fireEvent(
 	                     new UsageEvent(
 	                                     UsageEvent.Action.VIEW,
 	                                     user_ip,
 	                                     user_agent,
 	                                     xforwarderfor,
 	                                     context,
 	                                    community));
     		}
     		log.debug("fired event");
     		
 		} catch(SQLException ex){
 			log.error("SQL exception can't write usageEvent \n" + ex);
 		}
     		
 	}
}
