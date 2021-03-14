/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Item 'withdrawn' patches.
 * <p>
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/items/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /withdrawn", "value": true|false]'
 * </code>
 */
@Component
public class ItemWithdrawReplaceOperation<R> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_WITHDRAW = "/withdrawn";
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());

        boolean withdraw = getBooleanOperationValue(operation.getValue());

        if (supports(object, operation)) {
            Item item = (Item) object;
            // This is a request to withdraw the item.
            try {
                if (withdraw) {
                    if (item.getTemplateItemOf() != null) {
                        throw new UnprocessableEntityException("A template item cannot be withdrawn.");
                    }

                    // The item is currently not withdrawn and also not archived. Is this a possible situation?
                    if (!item.isWithdrawn() && !item.isArchived()) {
                        throw new UnprocessableEntityException("Cannot withdraw item when it is not in archive.");
                    }
                    // Item is already withdrawn. No-op, 200 response.
                    // (The operation is not idempotent since it results in a provenance note in the record.)
                    if (item.isWithdrawn()) {
                        return object;
                    }
                    itemService.withdraw(context, item);
                    return object;

                } else {
                    // No need to reinstate item if it has not previously been not withdrawn.
                    // No-op, 200 response. (The operation is not idempotent since it results
                    // in a provenance note in the record.)
                    if (!item.isWithdrawn()) {
                        return object;
                    }
                    itemService.reinstate(context, item);
                    return object;
                }
            } catch (AuthorizeException e) {
                throw new RESTAuthorizationException("Unauthorized user for item withdrawal/reinstation");
            } catch (SQLException e) {
                throw new DSpaceBadRequestException("SQL exception during item withdrawal/reinstation");
            }
        } else {
            throw new DSpaceBadRequestException("ItemWithdrawReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Item && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_WITHDRAW));
    }

}
