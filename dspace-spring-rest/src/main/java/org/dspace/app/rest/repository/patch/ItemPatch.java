/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Item resource patches.
 *
 * @author Michael Spalti
 */
@Component
public class ItemPatch extends AbstractResourcePatch<ItemRest> {

    private static final String OPERATION_PATH_WITHDRAW = "/withdrawn";

    private static final String OPERATION_PATH_DISCOVERABLE = "/discoverable";

    private static final Logger log = Logger.getLogger(ItemPatch.class);

    @Autowired
    ItemService is;

    /**
     * Implementation of the PATCH replace operation.
     *
     * @param restModel
     * @param context
     * @param operation
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Override
    protected void replace(ItemRest restModel, Context context, Operation operation)
        throws UnprocessableEntityException, PatchBadRequestException, SQLException, AuthorizeException {

        switch (operation.getPath()) {
            case OPERATION_PATH_WITHDRAW:
                withdraw(restModel, context, (Boolean) operation.getValue());
                break;
            case OPERATION_PATH_DISCOVERABLE:
                discoverable(restModel, context, (Boolean) operation.getValue());
                break;
            default:
                throw new UnprocessableEntityException(
                    "Unrecognized patch operation path: " + operation.getPath()
                );
        }
    }

    /**
     * Withdraws or reinstates the item based on boolean value provided in the patch request.
     *
     * @param restModel
     * @param context
     * @param withdrawItem
     * @throws PatchBadRequestException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void withdraw(ItemRest restModel, Context context, Boolean withdrawItem)
        throws PatchBadRequestException, SQLException, AuthorizeException {

        try {
            if (withdrawItem == null) {
                throw new PatchBadRequestException("Boolean value not provided for withdrawal operation.");
            }
            if (withdrawItem) {
                // Item is not withdrawn but is also NOT archived. Is this a possible situation?
                if (!restModel.getWithdrawn() && !restModel.getInArchive()) {
                    throw new UnprocessableEntityException("Cannot withdraw item because it is not archived.");
                }
                // Item is already withdrawn. No-op, 200 response.
                // (The operation is not idempotent since it results in a provenance note in the record.)
                if (restModel.getWithdrawn()) {
                    return;
                }
                Item item = is.find(context, UUID.fromString(restModel.getUuid()));
                is.withdraw(context, item);
            } else {
                // No need to reinstate item if it has not previously been not withdrawn.
                // No-op, 200 response. (The operation is not idempotent since it results
                // in a provenance note in the record.)
                if (!restModel.getWithdrawn()) {
                    return;
                }
                Item item = is.find(context, UUID.fromString(restModel.getUuid()));
                is.reinstate(context, item);
            }

        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Sets discoverable field on the item.
     *
     * @param restModel
     * @param context
     * @param isDiscoverable
     * @throws PatchBadRequestException
     */
    private void discoverable(ItemRest restModel, Context context, Boolean isDiscoverable)
        throws PatchBadRequestException {

        if (isDiscoverable == null) {
            throw new PatchBadRequestException("Boolean value not provided for discoverable operation.");
        }
        try {
            Item item = is.find(context, UUID.fromString(restModel.getUuid()));
            item.setDiscoverable(isDiscoverable);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
