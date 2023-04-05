/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The base class for resource PATCH operations.
 */
@Component
public class ResourcePatch<M> {

    @Autowired
    private List<PatchOperation> patchOperations;

    /**
     * Handles the patch operations. Patch implementations are provided by subclasses.
     * The default methods throw an UnprocessableEntityException.
     *
     * @param context       Context of patch operation
     * @param dso           the dso resource to patch
     * @param operations    list of patch operations
     * @throws UnprocessableEntityException
     * @throws DSpaceBadRequestException
     */
    public void patch(Context context, M dso, List<Operation> operations) throws SQLException {
        for (Operation operation: operations) {
            performPatchOperation(context, dso, operation);
        }
    }

    /**
     * Checks with all possible patch operations whether they support this operation
     *      (based on instanceof dso and operation.path)
     * @param context       Context of patch operation
     * @param object        the resource to patch
     * @param operation     the patch operation
     * @throws DSpaceBadRequestException
     */
    protected void performPatchOperation(Context context, M object, Operation operation)
            throws DSpaceBadRequestException, SQLException {
        for (PatchOperation patchOperation: patchOperations) {
            if (patchOperation.supports(object, operation)) {
                patchOperation.perform(context,object, operation);
                return;
            }
        }
        throw new DSpaceBadRequestException(
                "This operation is not supported."
        );
    }

}
