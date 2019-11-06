/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Item resource patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/item/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /discoverable", "value": true|false]'
 * </code>
 *
 *  @author Michael Spalti
 */
@Component
public class ItemDiscoverableReplaceOperation extends PatchOperation<ItemRest> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_DISCOVERABLE = "/discoverable";

    @Override
    public ItemRest perform(ItemRest item, Operation operation) {
        checkOperationValue(operation.getValue());
        checkModelForExistingValue(item);
        Boolean discoverable = getBooleanOperationValue(operation.getValue());
        item.setDiscoverable(discoverable);
        return item;

    }

    void checkModelForExistingValue(ItemRest resource) {
        if ((Object) resource.getDiscoverable() == null) {
            throw new DSpaceBadRequestException("Attempting to replace a non-existent value.");
        }
    }

    @Override
    public boolean supports(RestModel R, String path) {
        return (R instanceof ItemRest && path.trim().equalsIgnoreCase(OPERATION_PATH_DISCOVERABLE));
    }

}
