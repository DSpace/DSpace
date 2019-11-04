/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import java.util.List;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.factories.impl.PatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The base class for resource PATCH operations.
 *
 * @author Michael Spalti
 */
@Component
public class ResourcePatch<R extends RestModel> {

    @Autowired
    private List<PatchOperation> patchOperations;

    /**
     * Handles the patch operations. Patch implementations are provided by subclasses.
     * The default methods throw an UnprocessableEntityException.
     *
     * @param restModel the rest resource to patch
     * @param operations list of patch operations
     * @throws UnprocessableEntityException
     * @throws DSpaceBadRequestException
     */
    public R patch(R restModel, List<Operation> operations) {
        for (Operation operation: operations) {
            performPatchOperation(restModel, operation);
        }
        return restModel;

    }

    protected void performPatchOperation(R restModel, Operation operation)
            throws DSpaceBadRequestException {
        for (PatchOperation patchOperation: patchOperations) {
            if (patchOperation.supports(restModel, operation.getPath())) {
                System.out.println(patchOperation.getClass());
                patchOperation.perform(restModel, operation);
                return;
            }
        }
        throw new DSpaceBadRequestException(
                "This operation is not supported."
        );
    }

}
