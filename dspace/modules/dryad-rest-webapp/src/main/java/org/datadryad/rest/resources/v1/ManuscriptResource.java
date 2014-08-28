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

// TODO: shares a lot of code with OrganizationResource
// TODO: include nested organizationCode

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Path("organizations/{organizationCode}/manuscripts")

public class ManuscriptResource {

    @Context AbstractManuscriptStorage storage;
    @Context UriInfo uriInfo;
    @Context SecurityContext securityContext;

    private static final String[] codeFieldArray = {"organizationCode"};
    private static String[] codeValueArray(String code) {
        String[] array = new String[1];
        array[0] = code;
        return array;
    }
    
    private static final String[] manuscriptFieldArray = {"organizationCode", "manuscriptId"};
    private static String[] manuscriptValueArray(String code, String manuscriptId) {
        String[] array = new String[2];
        array[0] = code;
        array[1] = manuscriptId;
        return array;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManuscripts() {
        try {
            // Returning a list requires POJO turned on
            // TODO: get manuscripts from code
            return Response.ok(storage.getAll()).build();
        } catch (StorageException ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @Path("/{manuscriptId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManuscript(@PathParam("organizationCode") String organizationId, @PathParam("manuscriptId") String manuscriptId) {
        try {
            Manuscript manuscript = storage.findByValue(manuscriptFieldArray, manuscriptValueArray(organizationId, manuscriptId));
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
    public Response createManuscript(Manuscript manuscript) {
        if(manuscript.isValid()) {
            try {
                storage.create(manuscript);
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
    public Response updateManuscript(Manuscript manuscript) {
        if(manuscript.isValid()) {
            try {
                storage.update(manuscript);
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
    public Response deleteManuscript(@PathParam("organizationCode") String organizationId, @PathParam("manuscriptId") String manuscriptId) {
        try {
            storage.deleteByValue(manuscriptFieldArray, manuscriptValueArray(organizationId, manuscriptId));
        } catch (StorageException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
        return Response.noContent().build();
    }
}
