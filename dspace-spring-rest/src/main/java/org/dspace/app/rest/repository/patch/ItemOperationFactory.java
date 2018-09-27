/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.repository.patch.impl.ItemDiscoverableReplaceOperation;
import org.dspace.app.rest.repository.patch.impl.ItemWithdrawReplaceOperation;
import org.dspace.app.rest.repository.patch.impl.ResourcePatchOperation;
import org.dspace.content.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides factory method for instances of item PatchOperations.
 *
 * @author Michael Spalti
 */
@Component
public class ItemOperationFactory {

    @Autowired
    ItemDiscoverableReplaceOperation itemDiscoverableReplaceOperation;

    @Autowired
    ItemWithdrawReplaceOperation itemWithdrawReplaceOperation;

    private static final String OPERATION_PATH_WITHDRAW = "/withdrawn";
    private static final String OPERATION_PATH_DISCOVERABLE = "/discoverable";

    /**
     * Returns the PatchOperation for the operation.
     * @param path
     * @return
     */
    public ResourcePatchOperation<Item> getPatchOperationForPath(String path) {

        switch (path) {
            case OPERATION_PATH_DISCOVERABLE:
                return itemDiscoverableReplaceOperation;
            case OPERATION_PATH_WITHDRAW:
                return itemWithdrawReplaceOperation;
            default:
                throw new PatchBadRequestException("Missing or illegal patch operation for: " + path);
        }
    }
}
