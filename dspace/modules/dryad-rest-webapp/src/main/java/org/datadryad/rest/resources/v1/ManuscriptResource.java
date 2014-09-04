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
import org.apache.log4j.Logger;
import org.datadryad.rest.handler.ManuscriptHandlerGroup;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.AbstractManuscriptStorage;
import org.datadryad.rest.storage.AbstractOrganizationStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Path("organizations/{organizationCode}/manuscripts")

public class ManuscriptResource {
    private static final Logger log = Logger.getLogger(ManuscriptResource.class);
    @Context AbstractManuscriptStorage manuscriptStorage;
    @Context AbstractOrganizationStorage organizationStorage;
    @Context UriInfo uriInfo;
    @Context SecurityContext securityContext;
    @Context ManuscriptHandlerGroup handlers;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManuscripts(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode) {
        try {
            // Returning a list requires POJO turned on
            StoragePath path = new StoragePath();
            path.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
            return Response.ok(manuscriptStorage.getAll(path)).build();
        } catch (StorageException ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @Path("/{manuscriptId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManuscript(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode, @PathParam(Manuscript.MANUSCRIPT_ID) String manuscriptId) {
        try {
            StoragePath manuscriptPath = new StoragePath();
            manuscriptPath.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
            manuscriptPath.addPathElement(Manuscript.MANUSCRIPT_ID, manuscriptId);
            Manuscript manuscript = manuscriptStorage.findByPath(manuscriptPath);
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
    public Response createManuscript(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode, Manuscript manuscript) {
        StoragePath organizationPath = new StoragePath();
        organizationPath.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        if(manuscript.isValid()) {
            try {
                // Find the organization in database first.
                manuscript.organization = organizationStorage.findByPath(organizationPath);
                manuscriptStorage.create(organizationPath, manuscript);
            } catch (StorageException ex) {
                return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
            }
            // call handlers - must set organization first
            handlers.handleObjectCreated(organizationPath, manuscript);
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
    public Response updateManuscript(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode, @PathParam(Manuscript.MANUSCRIPT_ID) String manuscriptId, Manuscript manuscript) {
        StoragePath path = new StoragePath();
        path.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        path.addPathElement(Manuscript.MANUSCRIPT_ID, manuscriptId);
        if(manuscript.isValid()) {
            try {
                StoragePath organizationPath = new StoragePath();
                organizationPath.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
                manuscript.organization = organizationStorage.findByPath(organizationPath);
                manuscriptStorage.update(path, manuscript);
            } catch (StorageException ex) {
                return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
            }
            // call handlers - must set organization first.
            handlers.handleObjectUpdated(path, manuscript);
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Manuscript ID not found").build();
        }
    }

    @Path("/{manuscriptId}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteManuscript(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode, @PathParam(Manuscript.MANUSCRIPT_ID) String manuscriptId) {
        StoragePath path = new StoragePath();
        path.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        path.addPathElement(Manuscript.MANUSCRIPT_ID, manuscriptId);
        try {
            manuscriptStorage.deleteByPath(path);
        } catch (StorageException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
        // TODO: handle the deleted object - if we need to
        return Response.noContent().build();
    }
}
