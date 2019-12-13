/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Item resource patches.
 * <p>
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/item/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /withdrawn", "value": true|false]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class ItemWithdrawReplaceOperation extends ReplacePatchOperation<ItemRest, Boolean> {

    private static final Logger log = Logger.getLogger(ItemWithdrawReplaceOperation.class);

    @Override
    public ItemRest replace(ItemRest item, Operation operation) {

        Boolean withdraw = getBooleanOperationValue(operation.getValue());

        // This is a request to withdraw the item.
        if (withdraw) {
            if (item.getTemplateItemOf() != null) {
                throw new UnprocessableEntityException("A template item cannot be withdrawn.");
            }
            // The item is currently not withdrawn and also not archived. Is this a possible situation?
            if (!item.getWithdrawn() && !item.getInArchive()) {
                throw new UnprocessableEntityException("Cannot withdraw item when it is not in archive.");
            }
            // Item is already withdrawn. No-op, 200 response.
            // (The operation is not idempotent since it results in a provenance note in the record.)
            if (item.getWithdrawn()) {
                return item;
            }
            item.setWithdrawn(true);
            return item;

        } else {
            // No need to reinstate item if it has not previously been not withdrawn.
            // No-op, 200 response. (The operation is not idempotent since it results
            // in a provenance note in the record.)
            if (!item.getWithdrawn()) {
                return item;
            }
            item.setWithdrawn(false);
            return item;
        }

    }

    @Override
    void checkModelForExistingValue(ItemRest resource, Operation operation) {
        if ((Object) resource.getWithdrawn() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
    }

    protected Class<Boolean[]> getArrayClassForEvaluation() {
        return Boolean[].class;
    }

    protected Class<Boolean> getClassForEvaluation() {
        return Boolean.class;
    }

}
