/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 10/7/13
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/handle")
public class HandleResource {
    private static Logger log = Logger.getLogger(HandleResource.class);
    private static org.dspace.core.Context context;

    @GET
    @Path("/{prefix}/{suffix}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.DSpaceObject getObject(@PathParam("prefix") String prefix, @PathParam("suffix") String suffix) {
        try {
            if(context == null || !context.isValid() ) {
                context = new Context();
            }

            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            log.info("DSO Lookup by handle: [" + prefix + "] / [" + suffix + "] got result of: " + dso.getTypeText() + "_" + dso.getID());

            if(dso != null) {
                return new org.dspace.rest.common.DSpaceObject(dso);
            } else {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
