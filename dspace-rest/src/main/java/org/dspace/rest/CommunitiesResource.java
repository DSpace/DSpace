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

    private static org.dspace.core.Context context;

    private static final boolean writeStatistics;
    private static final int maxPagination;
	
	static{
		writeStatistics=ConfigurationManager.getBooleanProperty("rest","stats",false);
		maxPagination=ConfigurationManager.getIntProperty("rest", "max_pagination");
	}
    
    /*
    The "GET" annotation indicates this method will respond to HTTP Get requests.
    The "Produces" annotation indicates the MIME response the method will return.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String list() {
        StringBuilder everything = new StringBuilder();
        try {
            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
                //Failed SQL is ignored as a failed SQL statement, prevent: current transaction is aborted, commands ignored until end of transaction block
                context.getDBConnection().setAutoCommit(true);
            }
            org.dspace.content.Community[] communities = org.dspace.content.Community.findAllTop(context);
            for(org.dspace.content.Community community : communities) {
                everything.append(community.getName() + "<br/>\n");
            }
            return "<html><title>Hello!</title><body>Communities:<br/>" + everything.toString() + ".</body></html> ";

        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    //TODO Respond to html for communities/:id

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.CommunityReturn list(@QueryParam("expand") String expand,
    		@QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
    		@Context HttpServletRequest request) {
        try {
            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
                //Failed SQL is ignored as a failed SQL statement, prevent: current transaction is aborted, commands ignored until end of transaction block
                context.getDBConnection().setAutoCommit(true);
            }
            
            if(limit==null || limit<0 || limit>maxPagination){
            	limit=maxPagination;
            }
            int count=0;
            int added=0;
            org.dspace.content.Community[] topCommunities = org.dspace.content.Community.findAllTop(context);
            ArrayList<org.dspace.rest.common.Community> communityArrayList = new ArrayList<org.dspace.rest.common.Community>();
            for(org.dspace.content.Community community : topCommunities) {
            	if(count>=offset && added<(offset+limit)){
	                if(AuthorizeManager.authorizeActionBoolean(context, community, org.dspace.core.Constants.READ)) {
	                    //Only list communities that this user has access to.
	                    org.dspace.rest.common.Community restCommunity = new org.dspace.rest.common.Community(community, expand, context);
	                    communityArrayList.add(restCommunity);
	                    added++;
	                }
            	}else if(added>=limit){
            		break;
            	}
            	count++;
            }
            
            org.dspace.rest.common.Context community_context = new org.dspace.rest.common.Context();
            org.dspace.rest.common.CommunityReturn community_return = new org.dspace.rest.common.CommunityReturn();
            

            StringBuffer requestURL = request.getRequestURL();
            String queryString = request.getQueryString();
            
            if (queryString == null) {
            	community_context.setQuery(requestURL.toString());
            } else {
            	community_context.setQuery(requestURL.append('?').append(queryString).toString());
            }
            community_context.setLimit(limit);
            community_context.setOffset(offset);
            community_context.setTotal_count(topCommunities.length);
            community_return.setContext(community_context);
            community_return.setCommunity(communityArrayList);
            
            return community_return;

        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } //finally?
    }

    @GET
    @Path("/{community_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Community getCommunity(@PathParam("community_id") Integer community_id, @QueryParam("expand") String expand,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) {
        try {
            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
                //Failed SQL is ignored as a failed SQL statement, prevent: current transaction is aborted, commands ignored until end of transaction block
                context.getDBConnection().setAutoCommit(true);
            }

            org.dspace.content.Community community = org.dspace.content.Community.find(context, community_id);
            if(AuthorizeManager.authorizeActionBoolean(context, community, org.dspace.core.Constants.READ)) {
            	if(writeStatistics){
    				writeStats(community_id, user_ip, user_agent, xforwarderfor, headers, request);
    			}
                return new org.dspace.rest.common.Community(community, expand, context);
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } //finally?
    }
    
    private void writeStats(Integer community_id, String user_ip, String user_agent,
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
