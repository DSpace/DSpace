/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.BundleRest;
import org.dspace.content.Bundle;
import org.springframework.stereotype.Component;

@Component
public class BundleConverter
    extends DSpaceObjectConverter<Bundle, BundleRest> {

    @Override
    protected BundleRest newInstance() {
        return new BundleRest();
    }

    @Override
    public Class<Bundle> getModelClass() {
        return Bundle.class;
    }
}
