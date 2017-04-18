/*
 */
package org.datadryad.rest.resources.v1;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Journal;
import org.datadryad.rest.models.Package;
import org.datadryad.rest.responses.ErrorsResponse;
import org.datadryad.rest.responses.ResponseFactory;
import org.datadryad.rest.storage.AbstractOrganizationConceptStorage;
import org.datadryad.rest.storage.AbstractPackageStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.datadryad.rest.storage.rdbms.PackageDatabaseStorageImpl;
import org.dspace.content.authority.Concept;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Path("journals")
public class PackageResource {
    private static final Logger log = Logger.getLogger(PackageResource.class);
    @Inject AbstractOrganizationConceptStorage journalStorage;
    @Inject AbstractPackageStorage packageStorage;
    @Context UriInfo uriInfo;
    @Context SecurityContext securityContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJournals() {
        try {
            List<DryadJournalConcept> allJournalConceptList = journalStorage.getAll(new StoragePath());
            ArrayList<Journal> journals = new ArrayList<Journal>();
            for (DryadJournalConcept journalConcept : allJournalConceptList) {
                if (Concept.Status.ACCEPTED.name().equals(journalConcept.getStatus())) {
                    journals.add(new Journal(journalConcept));
                }
            }

            return Response.ok(journals).build();
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
                return Response.ok(new Journal(journalConcept)).build();
            }
        } catch (StorageException ex) {
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to get journal", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

    @Path("/{journalRef}/packages")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPackages(@PathParam(StoragePath.JOURNAL_PATH) String journalRef,
                                @DefaultValue("") @QueryParam("date_from") String dateFromString,
                                @DefaultValue("") @QueryParam("date_to") String dateToString,
                                @DefaultValue("") @QueryParam("search") String searchParam,
                                @DefaultValue("20") @QueryParam("count") Integer countParam,
                                @DefaultValue("0") @QueryParam("cursor") Integer cursorParam) {
        try {
            // Returning a list requires POJO turned on
            StoragePath path = StoragePath.createPackagesPath(journalRef);
            ArrayList<Package> packages = new ArrayList<Package>();
            Date dateFrom = null;
            Date dateTo = null;
            SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");
            try {
                if ("".equals(dateToString)) {
                    dateTo = new Date();
                } else {
                    dateTo = dateIso.parse(dateToString);
                }
                if ("".equals(dateFromString)) {
                    dateFrom = new Date(0);
                } else {
                    dateFrom = dateIso.parse(dateFromString);
                }
            } catch (ParseException e) {
                log.error("couldn't parse date: " + e.getMessage());
                ErrorsResponse error = ResponseFactory.makeError(e.getMessage(), "Unable to parse date", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return error.toResponse().build();
            }
            if (dateFrom != null && dateTo != null) {
                packageStorage.addResultsInDateRange(path, packages, dateFrom, dateTo, countParam, cursorParam);
            } else {
                packages.addAll(packageStorage.getResults(path, searchParam, countParam, cursorParam));
            }
            Response response = Response.ok(packages).build();
            return response;
        } catch (StorageException ex) {
            log.error("Exception getting packages", ex);
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to list packages", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

}
