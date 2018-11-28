/*
 */
package org.datadryad.rest.resources.v1;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Journal;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Package;
import org.datadryad.rest.models.ResultSet;
import org.datadryad.rest.responses.ErrorsResponse;
import org.datadryad.rest.responses.ResponseFactory;
import org.datadryad.rest.storage.AbstractOrganizationConceptStorage;
import org.datadryad.rest.storage.AbstractPackageStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.JournalUtils;
import org.dspace.content.authority.Concept;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
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
    public Response getJournals(@DefaultValue("20") @QueryParam("count") Integer countParam,
                                @DefaultValue("0") @QueryParam("cursor") Integer cursorParam) {
        try {
            ArrayList<DryadJournalConcept> journalConcepts = new ArrayList<DryadJournalConcept>();
            ResultSet resultSet = null;
            resultSet = journalStorage.getResults(new StoragePath(), journalConcepts, Concept.Status.ACCEPTED.name(), countParam, cursorParam);
            ArrayList<Journal> journals = new ArrayList<Journal>();
            for (DryadJournalConcept journalConcept : journalConcepts) {
                journals.add(new Journal(journalConcept));
            }

            URI firstLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor",resultSet.getFirstCursor()).build();
            URI lastLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor",resultSet.getLastCursor()).build();
            int total = resultSet.itemList.size();
            Response.ResponseBuilder responseBuilder = Response.ok(journals).link(firstLink, "first").link(lastLink, "last").header("X-Total-Count", total);
            if (resultSet.getNextCursor() > 0) {
                URI nextLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor", resultSet.getNextCursor()).build();
                responseBuilder.link(nextLink, "next");
            }
            if (resultSet.getPreviousCursor() > 0) {
                URI prevLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor", resultSet.getPreviousCursor()).build();
                responseBuilder.link(prevLink, "prev");
            }
            return responseBuilder.build();
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
                ErrorsResponse error = ResponseFactory.makeError("Journal with code " + journalRef + " does not exist", "Journal not found", uriInfo, Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(error).build();
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
                if (!"".equals(dateFromString) || !"".equals(dateToString)) {
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
                }
            } catch (ParseException e) {
                log.error("couldn't parse date: " + e.getMessage());
                ErrorsResponse error = ResponseFactory.makeError(e.getMessage(), "Unable to parse date", uriInfo, Status.BAD_REQUEST.getStatusCode());
                return error.toResponse().build();
            }
            ResultSet resultSet = null;
            if (dateFrom != null && dateTo != null) {
                resultSet = packageStorage.addResultsInDateRange(path, packages, dateFrom, dateTo, countParam, cursorParam);
            } else {
                resultSet = packageStorage.getResults(path, packages, searchParam, countParam, cursorParam);
            }

            URI firstLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor",resultSet.getFirstCursor()).build();
            URI lastLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor",resultSet.getLastCursor()).build();
            int total = resultSet.itemList.size();
            Response.ResponseBuilder responseBuilder = Response.ok(packages).link(firstLink, "first").link(lastLink, "last").header("X-Total-Count", total);
            if (resultSet.getNextCursor() > 0) {
                URI nextLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor", resultSet.getNextCursor()).build();
                responseBuilder.link(nextLink, "next");
            }
            if (resultSet.getPreviousCursor() > 0) {
                URI prevLink = uriInfo.getRequestUriBuilder().replaceQueryParam("cursor", resultSet.getPreviousCursor()).build();
                responseBuilder.link(prevLink, "prev");
            }
            return responseBuilder.build();
        } catch (Exception ex) {
            log.error("Exception getting packages", ex);
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to list packages", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
    }
    @Path("/{journalRef}/packages")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPackage(@PathParam(StoragePath.JOURNAL_PATH) String journalRef, Package pkg) {
        String msID = pkg.getManuscriptNumber();
        StoragePath journalPath = StoragePath.createJournalPath(journalRef);
        journalPath.setManuscriptId(msID);
        DryadJournalConcept dryadJournalConcept = JournalUtils.getJournalConceptByISSN(journalRef);
        if (dryadJournalConcept == null) {
            dryadJournalConcept = JournalUtils.getJournalConceptByJournalID(journalRef);
        }
        if (dryadJournalConcept == null) {
            ErrorsResponse error = ResponseFactory.makeError("Invalid journal reference: should be either a Dryad journal code or ISSN", "Invalid journal reference", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
        Manuscript manuscript = JournalUtils.getManuscriptFromManuscriptStorage(msID, dryadJournalConcept);
        if (manuscript == null) {
            ErrorsResponse error = ResponseFactory.makeError("Invalid manuscript number: Dryad does not have metadata for this manuscript", "Invalid manuscript number", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
        if (manuscript.isValid()) {
            try {
                DryadDataPackage dryadDataPackage = new DryadDataPackage(manuscript);
                dryadDataPackage.setIdentifier(pkg.getDryadDOI());
                packageStorage.create(journalPath, new Package(dryadDataPackage));
            } catch (Exception ex) {
                log.error("Exception creating manuscript", ex);
                ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to create manuscript", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
                return error.toResponse().build();
            }
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI uri = ub.path(manuscript.getManuscriptId()).build();
            return Response.created(uri).entity(manuscript).build();
        } else {
            ErrorsResponse error = ResponseFactory.makeError("Please check the structure of your object", "Invalid manuscript object", uriInfo, Status.BAD_REQUEST.getStatusCode());
            return error.toResponse().build();
        }
    }

}
