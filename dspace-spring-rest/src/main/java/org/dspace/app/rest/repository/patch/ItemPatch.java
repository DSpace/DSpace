/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.factories.ItemOperationFactory;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides PATCH operations for item updates.
 */
@Component
public class ItemPatch extends AbstractResourcePatch<ItemRest> {

    @Autowired
    ItemOperationFactory patchFactory;

    /**
     * Peforms the replace operation.
     * @param item the rest representation of the item
     * @param operation the replace operation
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     */
    protected ItemRest replace(ItemRest item, Operation operation)
            throws UnprocessableEntityException, PatchBadRequestException {

        ResourcePatchOperation<ItemRest> patchOperation =
                patchFactory.getReplaceOperationForPath(operation.getPath());

        return (ItemRest) patchOperation.perform(item, operation);

    }
}
