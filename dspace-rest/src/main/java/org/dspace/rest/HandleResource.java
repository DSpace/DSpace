/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.rest.common.Collection;
import org.dspace.rest.common.Community;
import org.dspace.rest.common.DSpaceObject;
import org.dspace.rest.common.Item;
import org.dspace.rest.exceptions.ContextException;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
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
public class HandleResource extends Resource {
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private static Logger log = Logger.getLogger(HandleResource.class);

    @GET
    @Path("/{prefix}/{suffix}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.DSpaceObject getObject(@PathParam("prefix") String prefix, @PathParam("suffix") String suffix, @QueryParam("expand") String expand, @javax.ws.rs.core.Context HttpHeaders headers) {
        DSpaceObject dSpaceObject = new DSpaceObject();
        org.dspace.core.Context context = null;

        try {
            context = createContext();

            org.dspace.content.DSpaceObject dso = handleService.resolveToObject(context, prefix + "/" + suffix);

            if(dso == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            DSpaceObjectService dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
            log.info("DSO Lookup by handle: [" + prefix + "] / [" + suffix + "] got result of: " + dSpaceObjectService.getTypeText(dso) + "_" + dso.getID());

            if(authorizeService.authorizeActionBoolean(context, dso, org.dspace.core.Constants.READ)) {
                switch(dso.getType()) {
                    case Constants.COMMUNITY:
                        dSpaceObject = new Community((org.dspace.content.Community) dso, servletContext, expand, context);
                        break;
                    case Constants.COLLECTION:
                        dSpaceObject = new Collection((org.dspace.content.Collection) dso, servletContext, expand, context, null, null);
                        break;
                    case Constants.ITEM:
                        dSpaceObject = new Item((org.dspace.content.Item) dso, servletContext, expand, context);
                        break;
                    default:
                        dSpaceObject = new DSpaceObject(dso, servletContext);
                        break;
                }
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            context.complete();

        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ContextException e)
        {
            processException("Could not read handle(prefix=" + prefix + "), (suffix=" + suffix + ") ContextException. Message:" + e.getMessage(),
                    context);
        } finally
        {
            processFinally(context);
        }

        return dSpaceObject;

    }
}
