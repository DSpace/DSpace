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
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.patch.DSpaceObjectPatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.DSpaceObjectService;
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
    final DSpaceObjectPatch<R> dsoPatch;

    @Autowired
    MetadataConverter metadataConverter;

    DSpaceObjectRestRepository(DSpaceObjectService<M> dsoService,
                               DSpaceObjectPatch<R> dsoPatch) {
        this.dsoService = dsoService;
        this.dsoPatch = dsoPatch;
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
        M dso = dsoService.find(obtainContext(), id);
        if (dso == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        R dsoRest = dsoPatch.patch(findOne(id), patch.getOperations());
        updateDSpaceObject(dso, dsoRest);
    }

    /**
     * Applies the changes in the given rest DSpace object to the model DSpace object.
     * The default implementation updates metadata if needed. Subclasses should extend
     * to support updates of additional properties.
     *
     * @param dso the dso to apply changes to.
     * @param dsoRest the rest representation of the new desired state.
     */
    protected void updateDSpaceObject(M dso, R dsoRest)
            throws AuthorizeException, SQLException {
        R origDsoRest = converter.toRest(dso, Projection.DEFAULT);
        if (!origDsoRest.getMetadata().equals(dsoRest.getMetadata())) {
            metadataConverter.setMetadata(obtainContext(), dso, dsoRest.getMetadata());
        }
    }
}
