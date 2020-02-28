/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

/**
 * Base class for DSpaceObject-based Rest Repositories, providing common functionality.
 *
 * @param <M> the specific type of DSpaceObject.
 * @param <R> the corresponding DSpaceObjectRest.
 */
public abstract class DSpaceObjectRestRepository<M extends DSpaceObject, R extends DSpaceObjectRest>
        extends DSpaceRestRepository<R, UUID> {

    final DSpaceObjectService<M> dsoService;

    @Autowired
    ResourcePatch<M> resourcePatch;
    @Autowired
    MetadataConverter metadataConverter;

    DSpaceObjectRestRepository(DSpaceObjectService<M> dsoService) {
        this.dsoService = dsoService;
    }

    /**
     * Updates the DSpaceObject according to the given Patch.
     *
     * @param apiCategory the api category.
     * @param model the api model.
     * @param id the id of the DSpaceObject.
     * @param patch the patch to apply.
     * @throws AuthorizeException if the action is unauthorized.
     * @throws ResourceNotFoundException if the DSpace object was not found.
     * @throws SQLException if a database error occurs.
     * @throws UnprocessableEntityException if the patch attempts to modify an unmodifiable attribute of the object.
     */
    protected void patchDSpaceObject(String apiCategory, String model, UUID id, Patch patch)
            throws AuthorizeException, ResourceNotFoundException, SQLException, UnprocessableEntityException {
        Context context = obtainContext();
        M dso = dsoService.find(context, id);
        if (dso == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        resourcePatch.patch(obtainContext(), dso, patch.getOperations());
        dsoService.update(obtainContext(), dso);
    }
}
