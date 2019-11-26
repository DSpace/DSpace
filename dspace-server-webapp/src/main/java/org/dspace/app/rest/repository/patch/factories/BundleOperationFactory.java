/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories;

import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.repository.patch.factories.impl.BundleMoveOperation;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides factory methods for obtaining instances of bundle patch operations.
 */
@Component
public class BundleOperationFactory {

    @Autowired
    BundleMoveOperation bundleMoveOperation;

    /**
     * Returns the patch instance for the move operation
     */
    public ResourcePatchOperation<BundleRest> getMoveOperation() {
        return bundleMoveOperation;
    }
}
