/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.repository.patch.factories.impl.ItemDiscoverableReplaceOperation;
import org.dspace.app.rest.repository.patch.factories.impl.ItemWithdrawReplaceOperation;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides factory methods for obtaining instances of item patch operations.
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
     * Returns the patch instance for the replace operation (based on the operation path).
     *
     * @param path the operation path
     * @return the patch operation implementation
     * @throws DSpaceBadRequestException
     */
    public ResourcePatchOperation<ItemRest> getReplaceOperationForPath(String path) {

        switch (path) {
            case OPERATION_PATH_DISCOVERABLE:
                return itemDiscoverableReplaceOperation;
            case OPERATION_PATH_WITHDRAW:
                return itemWithdrawReplaceOperation;
            default:
                throw new DSpaceBadRequestException("Missing patch operation for: " + path);
        }
    }
}
