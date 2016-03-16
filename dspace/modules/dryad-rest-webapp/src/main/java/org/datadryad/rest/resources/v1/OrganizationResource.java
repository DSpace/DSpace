/*
 */
package org.datadryad.rest.resources.v1;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
import javax.ws.rs.QueryParam;
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
import org.datadryad.api.DryadJournalConcept;
import org.dspace.JournalUtils;

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
    public Response getOrganizations(@QueryParam("status") String status) {
        try {
            List<DryadJournalConcept> allJournalConceptList = organizationStorage.getAll(new StoragePath());
            ArrayList<DryadJournalConcept> journalConceptList = new ArrayList<DryadJournalConcept>();
            if (status != null) {
                for (DryadJournalConcept journalConcept : allJournalConceptList) {
                    if (journalConcept != null) {
                        if (status.equals(journalConcept.getStatus())) {
                            journalConceptList.add(journalConcept);
                        }
                    }
                }
            } else {
                journalConceptList.addAll(allJournalConceptList);
            }
            // Returning a list requires POJO turned on
            return Response.ok(journalConceptList).build();
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to list organizations", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{organizationCode}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrganization(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode) {
        StoragePath path = StoragePath.createOrganizationPath(organizationCode);
        try {
            DryadJournalConcept journalConcept = organizationStorage.findByPath(path);
            if (journalConcept == null) {
                ErrorsResponse error = ResponseFactory.makeError("Organization with code " + organizationCode + " does not exist", "Organization not found", uriInfo, Status.NOT_FOUND.getStatusCode());
                return Response.status(Status.NOT_FOUND).entity(error).build();
            } else {
                return Response.ok(journalConcept).build();
            }
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to get organization", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

    private DryadJournalConcept createOrganization(DryadJournalConcept journalConcept) {
        DryadJournalConcept storedJournalConcept = null;

        // Check required fields
        if (journalConcept.isValid()) {
            try {
                organizationStorage.create(new StoragePath(), journalConcept);
            } catch (StorageException ex) {
                ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to create organization", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return null;
            }
            storedJournalConcept = JournalUtils.getJournalConceptByJournalName(journalConcept.getFullName());
        }
        return storedJournalConcept;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrganization(DryadJournalConcept[] journalConcepts) {
        Response response = null;
        ArrayList<DryadJournalConcept> concepts = new ArrayList<DryadJournalConcept>();
        DryadJournalConcept storedJournalConcept = null;
        for (int i=0; i<journalConcepts.length; i++) {
            storedJournalConcept = createOrganization(journalConcepts[i]);
            if (storedJournalConcept != null) {
                concepts.add(storedJournalConcept);
            }
        }
        if (concepts.size() > 0) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI uri = ub.path(storedJournalConcept.getJournalID()).build();
            return Response.created(uri).entity(concepts).build();
        } else {
            ErrorsResponse error = ResponseFactory.makeError("Please check the structure of your object", "Invalid organization object or organization already exists", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
    }

    private DryadJournalConcept updateOrganization(DryadJournalConcept journalConcept) {
        StoragePath path = StoragePath.createOrganizationPath(journalConcept.getJournalID());
        DryadJournalConcept storedJournalConcept = null;
        // Check required fields
        if (journalConcept.isValid()) {
            try {
                organizationStorage.update(path, journalConcept);
            } catch (StorageException ex) {
                ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to update organization", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return null;
            }
            storedJournalConcept = JournalUtils.getJournalConceptByJournalName(journalConcept.getFullName());
        }
        return storedJournalConcept;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrganization(DryadJournalConcept[] journalConcepts) {
        Response response = null;
        ArrayList<DryadJournalConcept> concepts = new ArrayList<DryadJournalConcept>();
        DryadJournalConcept storedJournalConcept = null;
        for (int i=0; i<journalConcepts.length; i++) {
            storedJournalConcept = updateOrganization(journalConcepts[i]);
            if (storedJournalConcept != null) {
                concepts.add(storedJournalConcept);
            }
        }
        if (concepts.size() > 0) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI uri = ub.path(storedJournalConcept.getJournalID()).build();
            return Response.created(uri).entity(concepts).build();
        } else {
            ErrorsResponse error = ResponseFactory.makeError("Please check the structure of your object", "Invalid organization object or organization doesn't exist", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{organizationCode}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteOrganization(@PathParam(Organization.ORGANIZATION_CODE) String organizationCode) {
        StoragePath path = StoragePath.createOrganizationPath(organizationCode);
        try {
            organizationStorage.deleteByPath(path);
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to delete organization", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
        return Response.noContent().build();
    }
}

