/*
 */
package org.datadryad.rest.resources.v1;

import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.ResultSet;
import org.datadryad.rest.responses.ErrorsResponse;
import org.datadryad.rest.responses.ResponseFactory;
import org.datadryad.rest.storage.AbstractOrganizationConceptStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.JournalUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */

@Path("organizations")
public class OrganizationConceptResource {
    @Inject AbstractOrganizationConceptStorage journalStorage;
    @Context UriInfo uriInfo;
    @Context HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJournals(@QueryParam("status") String status,
                                @DefaultValue("20") @QueryParam("count") Integer resultParam,
                                @DefaultValue("0") @QueryParam("cursor") Integer cursorParam) {
        try {
            ArrayList<DryadJournalConcept> journalConceptList = new ArrayList<DryadJournalConcept>();

            ResultSet resultSet = journalStorage.getResults(new StoragePath(), journalConceptList, status, resultParam, cursorParam);

            URI nextLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor",resultSet.nextCursor).build();
            URI prevLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor",resultSet.previousCursor).build();
            URI firstLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor",resultSet.firstCursor).build();
            URI lastLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor",resultSet.lastCursor).build();
            int total = resultSet.itemList.size();
            Response response = Response.ok(journalConceptList).link(nextLink, "next").link(prevLink, "prev").link(firstLink, "first").link(lastLink, "last").header("X-Total-Count", total).build();
            return response;
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to list journals", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{journalRef}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJournal(@PathParam(StoragePath.JOURNAL_PATH) String journalRef) {
        StoragePath path = StoragePath.createJournalPath(journalRef);
        try {
            DryadJournalConcept journalConcept = journalStorage.findByPath(path);
            if (journalConcept == null) {
                ErrorsResponse error = ResponseFactory.makeError("Journal with code " + journalRef + " does not exist", "Journal not found", uriInfo, Status.NOT_FOUND.getStatusCode());
                return Response.status(Status.NOT_FOUND).entity(error).build();
            } else {
                return Response.ok(journalConcept).build();
            }
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to get journal", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

    private DryadJournalConcept createJournal(DryadJournalConcept journalConcept) {
        DryadJournalConcept storedJournalConcept = null;

        // Check required fields
        if (journalConcept.isValid()) {
            try {
                journalStorage.create(new StoragePath(), journalConcept);
            } catch (StorageException ex) {
                ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to create journal", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return null;
            }
            storedJournalConcept = JournalUtils.getJournalConceptByJournalName(journalConcept.getFullName());
        }
        return storedJournalConcept;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJournal(DryadJournalConcept[] journalConcepts) {
        ArrayList<DryadJournalConcept> concepts = new ArrayList<DryadJournalConcept>();
        DryadJournalConcept storedJournalConcept = null;
        for (int i=0; i<journalConcepts.length; i++) {
            storedJournalConcept = createJournal(journalConcepts[i]);
            if (storedJournalConcept != null) {
                concepts.add(storedJournalConcept);
            }
        }
        if (concepts.size() > 0) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI uri = ub.path(storedJournalConcept.getJournalID()).build();
            return Response.created(uri).entity(concepts).build();
        } else {
            ErrorsResponse error = ResponseFactory.makeError("Please check the structure of your object", "Invalid journal object or journal already exists", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
    }

    private DryadJournalConcept updateJournal(DryadJournalConcept journalConcept) {
        StoragePath path = StoragePath.createJournalPath(journalConcept.getJournalID());
        DryadJournalConcept storedJournalConcept = null;
        // Check required fields
        if (journalConcept.isValid()) {
            try {
                journalStorage.update(path, journalConcept);
            } catch (StorageException ex) {
                ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to update journal", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return null;
            }
            storedJournalConcept = JournalUtils.getJournalConceptByJournalName(journalConcept.getFullName());
        }
        return storedJournalConcept;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateJournal(DryadJournalConcept[] journalConcepts) {
        ArrayList<DryadJournalConcept> concepts = new ArrayList<DryadJournalConcept>();
        DryadJournalConcept storedJournalConcept = null;
        for (int i=0; i<journalConcepts.length; i++) {
            storedJournalConcept = updateJournal(journalConcepts[i]);
            if (storedJournalConcept != null) {
                concepts.add(storedJournalConcept);
            }
        }
        if (concepts.size() > 0) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI uri = ub.path(storedJournalConcept.getJournalID()).build();
            return Response.created(uri).entity(concepts).build();
        } else {
            ErrorsResponse error = ResponseFactory.makeError("Please check the structure of your object", "Invalid journal object or journal doesn't exist", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{journalRef}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteJournal(@PathParam(StoragePath.JOURNAL_PATH) String journalRef) {
        StoragePath path = StoragePath.createJournalPath(journalRef);
        try {
            journalStorage.deleteByPath(path);
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to delete journal", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
        return Response.noContent().build();
    }
}

