/*
 */
package org.datadryad.rest.resources.v1;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.datadryad.rest.responses.ErrorsResponse;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.AbstractOrganizationStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.rest.responses.ResponseFactory;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */

@Path("organizations")
public class OrganizationResource {
    @Context AbstractOrganizationStorage organizationStorage;
    @Context UriInfo uriInfo;
    @Context HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrganizations() {
        try {
            // Returning a list requires POJO turned on
            return Response.ok(organizationStorage.getAll(new StoragePath())).build();
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to list organizations", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{organizationCode}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrganization(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode) {
        StoragePath path = new StoragePath();
        path.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        try {
            Organization organization = organizationStorage.findByPath(path);
            if(organization == null) {
                ErrorsResponse error = ResponseFactory.makeError("Organization with code " + organizationCode + " does not exist", "Organization not found", uriInfo, Status.NOT_FOUND.getStatusCode());
                return Response.status(Status.NOT_FOUND).entity(error).build();
            } else {
                return Response.ok(organization).build();
            }
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to get organization", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrganization(Organization organization) {
        // Check required fields
        if(organization.isValid()) {
            try {
                organizationStorage.create(new StoragePath(), organization);
            } catch (StorageException ex) {
                ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to create organization", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return error.toResponse().build();
            }
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI uri = ub.path(organization.organizationCode).build();
            return Response.created(uri).entity(organization).build();
        } else {
            ErrorsResponse error = ResponseFactory.makeError("Please check the structure of your object", "Invalid organization object", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{organizationCode}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrganization(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode, Organization organization) {
        StoragePath path = new StoragePath();
        path.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        // Check required fields
        if(organization.isValid()) {
            try {
                organizationStorage.update(path, organization);
            } catch (StorageException ex) {
                ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to update organization", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return error.toResponse().build();
            }
            return Response.ok(organization).build();
        } else {
            ErrorsResponse error = ResponseFactory.makeError("Please check the structure of your object",  "Invalid organization object", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{organizationCode}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteOrganization(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode) {
        StoragePath path = new StoragePath();
        path.addPathElement(Organization.ORGANIZATION_CODE, organizationCode);
        try {
            organizationStorage.deleteByPath(path);
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to delete organization", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
        return Response.noContent().build();
    }
}

