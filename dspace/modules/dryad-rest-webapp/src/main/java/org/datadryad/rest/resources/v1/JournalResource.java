/*
 */
package org.datadryad.rest.resources.v1;

import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Journal;
import org.datadryad.rest.responses.ErrorsResponse;
import org.datadryad.rest.responses.ResponseFactory;
import org.datadryad.rest.storage.AbstractJournalStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.JournalUtils;

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
public class JournalResource {
    @Context
    AbstractJournalStorage journalStorage;
    @Context UriInfo uriInfo;
    @Context HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJournals(@QueryParam("status") String status) {
        try {
            List<DryadJournalConcept> allJournalConceptList = journalStorage.getAll(new StoragePath());
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
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to list journals", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{journalCode}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJournal(@PathParam(Journal.JOURNAL_CODE) String journalCode) {
        StoragePath path = StoragePath.createJournalPath(journalCode);
        try {
            DryadJournalConcept journalConcept = journalStorage.findByPath(path);
            if (journalConcept == null) {
                ErrorsResponse error = ResponseFactory.makeError("Journal with code " + journalCode + " does not exist", "Journal not found", uriInfo, Status.NOT_FOUND.getStatusCode());
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
        Response response = null;
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
        Response response = null;
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

    @Path("/{journalCode}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteJournal(@PathParam(Journal.JOURNAL_CODE) String journalCode) {
        StoragePath path = StoragePath.createJournalPath(journalCode);
        try {
            journalStorage.deleteByPath(path);
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to delete journal", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
        return Response.noContent().build();
    }
}

