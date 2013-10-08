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
import org.dspace.core.Context;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

    private static org.dspace.core.Context context;

    /*
    The "GET" annotation indicates this method will respond to HTTP Get requests.
    The "Produces" annotation indicates the MIME response the method will return.
     */
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public String listHTML() {
        StringBuilder everything = new StringBuilder();
        try {
            org.dspace.core.Context context = new org.dspace.core.Context();

            org.dspace.content.Collection[] collections = org.dspace.content.Collection.findAll(context);
            for(org.dspace.content.Collection collection : collections) {
                //TODO check auth...
                everything.append("<li><a href='" + servletContext.getContextPath() + "/collections/" + collection.getID() + "'>" + collection.getID() + " - " + collection.getName() + "</a></li>\n");
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
            return "ERROR: " + e.getMessage();
        }

        return "<html><title>Hello!</title><body>Collections<br/><ul>" + everything.toString() + "</ul>.</body></html> ";
    }

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Collection[] list(@QueryParam("expand") String expand) {
        try {
            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
            }

            org.dspace.content.Collection[] collections = org.dspace.content.Collection.findAll(context);
            ArrayList<org.dspace.rest.common.Collection> collectionArrayList = new ArrayList<org.dspace.rest.common.Collection>();
            for(org.dspace.content.Collection collection : collections) {
                if(AuthorizeManager.authorizeActionBoolean(context, collection, org.dspace.core.Constants.READ)) {
                    org.dspace.rest.common.Collection restCollection = new org.dspace.rest.common.Collection(collection, expand, context);
                    collectionArrayList.add(restCollection);
                } // Not showing restricted-access collections
            }

            return collectionArrayList.toArray(new org.dspace.rest.common.Collection[0]);

        } catch (SQLException e) {
            e.getMessage();
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/{collection_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Collection getCollection(@PathParam("collection_id") Integer collection_id, @QueryParam("expand") String expand) {
        try {
            if(context == null || !context.isValid() ) {
                context = new Context();
            }

            org.dspace.content.Collection collection = org.dspace.content.Collection.find(context, collection_id);
            if(AuthorizeManager.authorizeActionBoolean(context, collection, org.dspace.core.Constants.READ)) {
                return new org.dspace.rest.common.Collection(collection, expand, context);
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
