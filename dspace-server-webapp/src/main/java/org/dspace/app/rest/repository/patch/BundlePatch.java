package org.dspace.app.rest.repository.patch;

import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.repository.patch.factories.BundleOperationFactory;
import org.dspace.app.rest.repository.patch.factories.impl.ResourcePatchOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by kristof on 24/09/2019
 */
@Component
public class BundlePatch extends DSpaceObjectPatch<BundleRest> {

    @Autowired
    BundleOperationFactory patchFactory;

    protected BundleRest move(BundleRest restModel, Operation operation) {
        ResourcePatchOperation<BundleRest> patchOperation = patchFactory.getMoveOperation();
        return patchOperation.perform(restModel, operation);
    }
}
