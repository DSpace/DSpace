/*
 */
package org.datadryad.rest.resources.v1;

import org.apache.log4j.Logger;
import org.datadryad.rest.responses.ErrorsResponse;
import org.datadryad.rest.responses.ResponseFactory;
import org.datadryad.rest.storage.AbstractJournalConceptStorage;
import org.datadryad.rest.storage.AbstractPackageStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Path("journals/{journalRef}/packages")

public class PackageResource {
    private static final Logger log = Logger.getLogger(PackageResource.class);
    @Context
    AbstractJournalConceptStorage journalStorage;
    @Context
    AbstractPackageStorage packageStorage;
    @Context UriInfo uriInfo;
    @Context SecurityContext securityContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPackages(@PathParam(StoragePath.JOURNAL_PATH) String journalRef, @QueryParam("search") String searchParam, @QueryParam("count") Integer resultParam) {
        try {
            // Returning a list requires POJO turned on
            StoragePath path = StoragePath.createPackagesPath(journalRef);
            return Response.ok(packageStorage.getResults(path,"",100)).build();
        } catch (StorageException ex) {
            log.error("Exception getting packages", ex);
            ErrorsResponse error = ResponseFactory.makeError(ex.getMessage(), "Unable to list packages", uriInfo, Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return error.toResponse().build();
        }
    }

}
