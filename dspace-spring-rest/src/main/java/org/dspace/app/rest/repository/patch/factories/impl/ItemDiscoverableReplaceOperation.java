/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * This is the implementation for Item resource patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/item/<:id-item> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /discoverable", "value": "true|false"]'
 * </code>
 *
 *  @author Michael Spalti
 */
@Component
public class ItemDiscoverableReplaceOperation extends PatchOperation<ItemRest, String>
        implements ResourcePatchOperation<ItemRest> {

    private static final Logger log = Logger.getLogger(ItemDiscoverableReplaceOperation.class);

    /**
     * Updates the discoverable in the item rest model.
     * @param item the rest model
     * @param operation
     * @return updated rest model
     * @throws PatchBadRequestException
     */
    public ItemRest perform(ItemRest item, Operation operation)
            throws PatchBadRequestException {

        return replace(item, operation);

    }

    private ItemRest replace(ItemRest item, Operation operation) {

        checkOperationValue(operation.getValue());
        Boolean discoverable = getBooleanOperationValue(operation.getValue());
        item.setDiscoverable(discoverable);
        return item;

    }

    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    protected Class<String> getClassForEvaluation() {
        return String.class;
    }

}
