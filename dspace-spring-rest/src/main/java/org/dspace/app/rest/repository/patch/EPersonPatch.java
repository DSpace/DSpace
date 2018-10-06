/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.factories.EPersonOperationFactory;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides patch operations for eperson updates.
 */
@Component
public class EPersonPatch extends AbstractResourcePatch<EPersonRest> {

    @Autowired
    EPersonOperationFactory patchFactory;

    /**
     * Performs the replace operation.
     * @param eperson the eperson rest representation
     * @param operation the replace operation
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     */
    protected EPersonRest replace(EPersonRest eperson, Operation operation)
            throws UnprocessableEntityException, PatchBadRequestException {

        ResourcePatchOperation<EPersonRest> patchOperation =
                patchFactory.getReplaceOperationForPath(operation.getPath());

        return (EPersonRest) patchOperation.perform(eperson, operation);

    }
}
