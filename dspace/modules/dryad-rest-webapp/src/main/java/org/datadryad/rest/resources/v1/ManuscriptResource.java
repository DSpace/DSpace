/*
 */
package org.datadryad.rest.resources.v1;

import java.net.URI;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.storage.AbstractManuscriptStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Path("organizations/{organizationCode}/manuscripts")

public class ManuscriptResource {

    @Context AbstractManuscriptStorage storage;
    @Context UriInfo uriInfo;
    @Context SecurityContext securityContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManuscripts(@PathParam("organizationCode") String organizationCode) {
        try {
            // Returning a list requires POJO turned on
            StoragePath path = new StoragePath();
            path.addPathElement("organizationCode", organizationCode);
            return Response.ok(storage.getAll(path)).build();
        } catch (StorageException ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @Path("/{manuscriptId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManuscript(@PathParam("organizationCode") String organizationCode, @PathParam("manuscriptId") String manuscriptId) {
        try {
            StoragePath path = new StoragePath();
            path.addPathElement("organizationCode", organizationCode);
            path.addPathElement("manuscriptId", manuscriptId);
            Manuscript manuscript = storage.findByPath(path);
            if(manuscript == null) {
                return Response.status(Status.NOT_FOUND).build();
            } else {
                return Response.ok(manuscript).build();
            }
        } catch (StorageException ex) {
            // what to do here?
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createManuscript(@PathParam("organizationCode") String organizationCode, Manuscript manuscript) {
        StoragePath path = new StoragePath();
        path.addPathElement("organizationCode", organizationCode);
        if(manuscript.isValid()) {
            try {
                storage.create(path, manuscript);
            } catch (StorageException ex) {
                return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
            }
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI uri = ub.path(manuscript.manuscriptId).build();
            return Response.created(uri).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Manuscript ID not found").build();
        }
    }

    @Path("/{manuscriptId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateManuscript(@PathParam("organizationCode") String organizationCode, @PathParam("manuscriptId") String manuscriptId, Manuscript manuscript) {
        StoragePath path = new StoragePath();
        path.addPathElement("organizationCode", organizationCode);
        path.addPathElement("manuscriptId", manuscriptId);
        if(manuscript.isValid()) {
            try {
                storage.update(path, manuscript);
            } catch (StorageException ex) {
                return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
            }
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Manuscript ID not found").build();
        }
    }

    @Path("/{manuscriptId}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteManuscript(@PathParam("organizationCode") String organizationCode, @PathParam("manuscriptId") String manuscriptId) {
        StoragePath path = new StoragePath();
        path.addPathElement("organizationCode", organizationCode);
        path.addPathElement("manuscriptId", manuscriptId);
        try {
            storage.deleteByPath(path);
        } catch (StorageException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
        return Response.noContent().build();
    }
}
