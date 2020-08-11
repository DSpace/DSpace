/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Item 'discoverable' patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/items/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /discoverable", "value": true|false]'
 * </code>
 */
@Component
public class ItemDiscoverableReplaceOperation<R> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_DISCOVERABLE = "/discoverable";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        Boolean discoverable = getBooleanOperationValue(operation.getValue());
        if (supports(object, operation)) {
            Item item = (Item) object;
            if (discoverable && item.getTemplateItemOf() != null) {
                throw new UnprocessableEntityException("A template item cannot be discoverable.");
            }
            item.setDiscoverable(discoverable);
            return object;
        } else {
            throw new DSpaceBadRequestException("ItemDiscoverableReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Item && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
                && operation.getPath().trim().equalsIgnoreCase(OPERATION_PATH_DISCOVERABLE));
    }

}
