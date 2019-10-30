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
import org.dspace.app.rest.repository.patch.factories.impl.AddPatchOperation;
import org.dspace.app.rest.repository.patch.factories.impl.DspaceObjectMetadataOperation;
import org.dspace.app.rest.repository.patch.factories.impl.PatchOperation;
import org.dspace.app.rest.repository.patch.factories.impl.ReplacePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The base class for resource PATCH operations.
 *
 * @author Michael Spalti
 */
@Component
public class ResourcePatch<R extends RestModel> {

//    @Autowired
//    private List<AddPatchOperation> addOperations;
    @Autowired
    private List<ReplacePatchOperation> replaceOperations;
//    @Autowired
//    private List<AddPatchOperation> removeOperations;
    @Autowired
    private DspaceObjectMetadataOperation metadataOperation;

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

        // Note: the list of possible operations is taken from JsonPatchConverter class. Does not implement
        // test https://tools.ietf.org/html/rfc6902#section-4.6
        System.out.println("restModel: " + restModel);
        System.out.println("ops: " + operations);
        ops: for (Operation op : operations) {
            if (metadataOperation.supports(restModel, op.getPath())) {
                metadataOperation.perform(restModel, op);
                continue ops;
            }
            switch (op.getOp()) {
                case "add":
                    restModel = add(restModel, op);
                    continue ops;
                case "replace":
                    restModel = replace(restModel, op);
                    continue ops;
                case "remove":
                    restModel = remove(restModel, op);
                    continue ops;
                case "copy":
                    restModel = copy(restModel, op);
                    continue ops;
                case "move":
                    restModel = move(restModel, op);
                    continue ops;
                default:
                    // JsonPatchConverter should have thrown error before this point.
                    throw new DSpaceBadRequestException("Missing or illegal patch operation: " + op.getOp());
            }
        }

        return restModel;

    }
    // The default patch methods throw an error when no sub-class implementation is provided.

    protected R add(R restModel, Operation operation)
            throws UnprocessableEntityException, DSpaceBadRequestException {
//        for (PatchOperation patchOperation: addOperations) {
//            if (patchOperation.supports(restModel, operation.getPath())) {
//                return (R) patchOperation.perform(restModel, operation);
//            }
//        }
        throw new UnprocessableEntityException(
                "The add operation for " + restModel.getClass() + " is not supported."
        );
    }

    protected R replace(R restModel, Operation operation)
            throws UnprocessableEntityException, DSpaceBadRequestException {
        for (PatchOperation patchOperation: replaceOperations) {
            if (patchOperation.supports(restModel, operation.getPath())) {
                return (R) patchOperation.perform(restModel, operation);
            }
        }
        throw new UnprocessableEntityException(
                "The replace operation for " + restModel.getClass() + " is not supported."
        );
    }

    protected R remove(R restModel, Operation operation)

            throws UnprocessableEntityException, DSpaceBadRequestException {
//        for (PatchOperation patchOperation: removeOperations) {
//            if (patchOperation.supports(restModel, operation.getPath())) {
//                return (R) patchOperation.perform(restModel, operation);
//            }
//        }
        throw new UnprocessableEntityException(
                "The remove operation for " + restModel.getClass() + " is not supported."
        );
    }

    protected R copy(R restModel, Operation operation)
            throws UnprocessableEntityException, DSpaceBadRequestException {
        throw new UnprocessableEntityException(
                "The copy operation for " + restModel.getClass() + " is not supported."
        );
    }

    protected R move(R restModel, Operation operation)
            throws UnprocessableEntityException, DSpaceBadRequestException {
        throw new UnprocessableEntityException(
                "The copy operation is not supported."
        );
    }

}
