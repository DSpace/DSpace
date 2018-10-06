/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import java.util.List;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;

/**
 * The base class for resource PATCH operations.
 *
 * @author Michael Spalti
 */
public abstract class AbstractResourcePatch<R extends RestModel> {

    /**
     * Handles the patch operations. Patch implementations are provided by subclasses.
     * The default methods throw an UnprocessableEntityException.
     *
     * @param restModel the rest resource to patch
     * @param operations list of patch operations
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     */
    public RestModel patch(R restModel, List<Operation> operations)
            throws UnprocessableEntityException, PatchBadRequestException {

        // Note: the list of possible operations is taken from JsonPatchConverter class. Does not implement
        // test https://tools.ietf.org/html/rfc6902#section-4.6
        ops: for (Operation op : operations) {
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
                    throw new PatchBadRequestException("Missing or illegal patch operation: " + op.getOp());
            }
        }

        return restModel;

    }
    // The default patch methods throw an error when no sub-class implementation is provided.

    protected R add(R restModel, Operation operation)
            throws UnprocessableEntityException, PatchBadRequestException {
        throw new UnprocessableEntityException(
                "The add operation is not supported."
        );
    }

    protected R replace(R restModel, Operation operation)
            throws UnprocessableEntityException, PatchBadRequestException {
        throw new UnprocessableEntityException(
                "The remove operation is not supported."
        );
    }

    protected R remove(R restModel, Operation operation)

            throws UnprocessableEntityException, PatchBadRequestException {
        throw new UnprocessableEntityException(
                "The remove operation is not supported."
        );
    }

    protected R copy(R restModel, Operation operation)
            throws UnprocessableEntityException, PatchBadRequestException {
        throw new UnprocessableEntityException(
                "The copy operation is not supported."
        );
    }

    protected R move(R restModel, Operation operation)
            throws UnprocessableEntityException, PatchBadRequestException {
        throw new UnprocessableEntityException(
                "The move operation is not supported."
        );
    }

}