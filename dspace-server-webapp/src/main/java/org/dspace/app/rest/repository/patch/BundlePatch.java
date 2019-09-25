/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.factories.BundleOperationFactory;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides PATCH operations for bundle updates.
 */
@Component
public class BundlePatch extends DSpaceObjectPatch<BundleRest> {

    @Autowired
    BundleOperationFactory patchFactory;

    /**
     * Performs the move operation.
     * @param restModel the rest representation of the bundle
     * @param operation the move operation
     * @throws UnprocessableEntityException
     * @throws DSpaceBadRequestException
     */
    protected BundleRest move(BundleRest restModel, Operation operation) {
        ResourcePatchOperation<BundleRest> patchOperation = patchFactory.getMoveOperation();
        return patchOperation.perform(restModel, operation);
    }
}
