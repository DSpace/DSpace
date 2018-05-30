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

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

/**
 * The base class for resource PATCH operations.
 *
 * @author Michael Spalti
 */
public abstract class AbstractResourcePatch<R extends RestModel> {

    /**
     * Handles the patch operations, delegating actions to sub-class implementations. If no sub-class method
     * is provided, the default method throws a UnprocessableEntityException.
     *
     * @param restModel the REST resource to patch
     * @param context
     * @param patch
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void patch(R restModel, Context context, Patch patch)
        throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {

        List<Operation> operations = patch.getOperations();

        // Note: the list of possible operations is taken from JsonPatchConverter class. Does not implement
        // test https://tools.ietf.org/html/rfc6902#section-4.6
        ops: for (Operation op : operations) {
            switch (op.getOp()) {
                case "add":
                    add(restModel, context, op);
                    continue ops;
                case "replace":
                    replace(restModel, context, op);
                    continue ops;
                case "remove":
                    remove(restModel, context, op);
                    continue ops;
                case "copy":
                    copy(restModel, context, op);
                    continue ops;
                case "move":
                    move(restModel, context, op);
                    continue ops;
                default:
                    // JsonPatchConverter should have thrown error before this point.
                    throw new PatchBadRequestException("Missing or illegal patch operation: " + op.getOp());
            }
        }
    }
    // The default patch methods throw an error when no sub-class implementation is provided.

    protected void add(R restModel, Context context, Operation operation)
        throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {
        throw new UnprocessableEntityException(
            "The add operation is not supported."
        );
    }

    protected void replace(R restModel, Context context, Operation operation)
        throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {
        // The replace operation is functionally identical to a "remove" operation for
        // a value, followed immediately by an "add" operation at the same
        // location with the replacement value. https://tools.ietf.org/html/rfc6902#section-4.3
        remove(restModel, context, operation);
        add(restModel, context, operation);
    }

    protected void remove(R restModel, Context context, Operation operation)

        throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {
        throw new UnprocessableEntityException(
            "The remove operation is not supported."
        );
    }

    protected void copy(R restModel, Context context, Operation operation)
        throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {
        throw new UnprocessableEntityException(
            "The copy operation is not supported."
        );
    }

    protected void move(R restModel, Context context, Operation operation)
        throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {
        throw new UnprocessableEntityException(
            "The move operation is not supported."
        );
    }

}
