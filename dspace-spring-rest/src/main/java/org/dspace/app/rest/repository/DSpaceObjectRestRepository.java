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

import org.dspace.app.rest.converter.DSpaceObjectConverter;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.DSpaceObjectPatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.DSpaceObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

public abstract class DSpaceObjectRestRepository<M extends DSpaceObject, R extends DSpaceObjectRest>
        extends DSpaceRestRepository<R, UUID> {

    final DSpaceObjectService<M> dsoService;
    final DSpaceObjectPatch<R> dsoPatch;
    final DSpaceObjectConverter<M, R> dsoConverter;

    @Autowired
    MetadataConverter metadataConverter;

    DSpaceObjectRestRepository(DSpaceObjectService<M> dsoService,
                               DSpaceObjectConverter<M, R> dsoConverter,
                               DSpaceObjectPatch<R> dsoPatch) {
        this.dsoService = dsoService;
        this.dsoPatch = dsoPatch;
        this.dsoConverter = dsoConverter;
    }

    protected void patchDSpaceObject(String apiCategory, String model, UUID id, Patch patch)
            throws AuthorizeException, PatchBadRequestException, ResourceNotFoundException,
            SQLException, UnprocessableEntityException {
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
        R origDsoRest = dsoConverter.fromModel(dso);
        if (!origDsoRest.getMetadata().equals(dsoRest.getMetadata())) {
            metadataConverter.setMetadata(obtainContext(), dso, dsoRest.getMetadata());
        }
    }
}
