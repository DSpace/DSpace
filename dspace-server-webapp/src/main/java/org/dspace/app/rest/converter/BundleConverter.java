package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.BundleRest;
import org.dspace.content.Bundle;
import org.springframework.stereotype.Component;

@Component
public class BundleConverter
    extends DSpaceObjectConverter<org.dspace.content.Bundle, org.dspace.app.rest.model.BundleRest> {

    protected BundleRest newInstance() {
        return new BundleRest();
    }

    protected Class<Bundle> getModelClass() {
        return Bundle.class;
    }

    public BundleRest fromModel(Bundle obj) {
        BundleRest bundle = (BundleRest) super.fromModel(obj);
        return bundle;
    }

    public Bundle toModel(BundleRest obj) {
        return null;
    }
}

