package org.dspace.rest;

import org.dspace.core.Context;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
    private static Context context;

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
                context = new Context();
            }
            org.dspace.content.Community[] communities = org.dspace.content.Community.findAllTop(context);
            for(org.dspace.content.Community community : communities) {
                everything.append(community.getName() + "<br/>\n");
            }

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }

        return "<html><title>Hello!</title><body>Communities:<br/>" + everything.toString() + ".</body></html> ";
    }

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Community[] list(@QueryParam("expand") String expand) {
        try {
            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
            }

            org.dspace.content.Community[] topCommunities = org.dspace.content.Community.findAllTop(context);
            ArrayList<org.dspace.rest.common.Community> communityArrayList = new ArrayList<org.dspace.rest.common.Community>();
            for(org.dspace.content.Community community : topCommunities) {
                org.dspace.rest.common.Community restCommunity = new org.dspace.rest.common.Community(community, expand);
                communityArrayList.add(restCommunity);
            }

            return communityArrayList.toArray(new org.dspace.rest.common.Community[0]);

        } catch (SQLException e) {
            return null;
        }
    }

    @GET
    @Path("/{community_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Community getCommunity(@PathParam("community_id") Integer community_id, @QueryParam("expand") String expand) {
        return new org.dspace.rest.common.Community(community_id, expand);
    }
}
