package org.dspace.app.rest.repository.patch.factories;

import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.repository.patch.factories.impl.BundleMoveOperation;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by kristof on 24/09/2019
 */
@Component
public class BundleOperationFactory {

    @Autowired
    BundleMoveOperation bundleMoveOperation;

    public ResourcePatchOperation<BundleRest> getMoveOperation() {
        return bundleMoveOperation;
    }
}
