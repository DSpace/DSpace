/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Item resource patches.
 * <p>
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/item/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /withdrawn", "value": true|false]'
 * </code>
 */
@Component
public class ItemWithdrawReplaceOperation extends PatchOperation<Item> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_WITHDRAW = "/withdrawn";
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
    public Item perform(Context context, Item item, Operation operation) {
        checkOperationValue(operation.getValue());
        checkModelForExistingValue(item);

        Boolean withdraw = getBooleanOperationValue(operation.getValue());

        // This is a request to withdraw the item.
        if (withdraw) {
            // The item is currently not withdrawn and also not archived. Is this a possible situation?
            if (!item.isWithdrawn() && !item.isArchived()) {
                throw new UnprocessableEntityException("Cannot withdraw item when it is not in archive.");
            }
            // Item is already withdrawn. No-op, 200 response.
            // (The operation is not idempotent since it results in a provenance note in the record.)
            if (item.isWithdrawn()) {
                return item;
            }
            try {
                itemService.withdraw(context, item);
            } catch (SQLException e) {
                // TODO
                e.printStackTrace();
            } catch (AuthorizeException e) {
                // TODO
                e.printStackTrace();
            }
            return item;

        } else {
            // No need to reinstate item if it has not previously been not withdrawn.
            // No-op, 200 response. (The operation is not idempotent since it results
            // in a provenance note in the record.)
            if (!item.isWithdrawn()) {
                return item;
            }
            try {
                itemService.reinstate(context, item);
            } catch (SQLException e) {
                // TODO
                e.printStackTrace();
            } catch (AuthorizeException e) {
                // TODO
                e.printStackTrace();
            }
            return item;
        }

    }

    void checkModelForExistingValue(Item resource) {
        if ((Object) resource.isWithdrawn() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
    }

    @Override
    public boolean supports(DSpaceObject dso, String path) {
        return (dso instanceof Item && path.trim().equalsIgnoreCase(OPERATION_PATH_WITHDRAW));
    }

}
