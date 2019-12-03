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
public class ItemDiscoverableReplaceOperation extends ReplacePatchOperation<ItemRest, Boolean> {

    private static final Logger log = Logger.getLogger(ItemDiscoverableReplaceOperation.class);


    @Override
    public ItemRest replace(ItemRest item, Operation operation) {

        Boolean discoverable = getBooleanOperationValue(operation.getValue());
        if (discoverable) {
            if (item.getTemplateItemOf() != null) {
                throw new UnprocessableEntityException("A template item cannot be discoverable.");
            }
        }
        item.setDiscoverable(discoverable);
        return item;

    }

    @Override
    void checkModelForExistingValue(ItemRest resource, Operation operation) {
        if ((Object) resource.getDiscoverable() == null) {
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
