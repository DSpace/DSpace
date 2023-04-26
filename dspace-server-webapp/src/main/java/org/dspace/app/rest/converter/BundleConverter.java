/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Bundle in the DSpace API data model and the
 * REST data model
 */
@Component
public class BundleConverter
    extends DSpaceObjectConverter<Bundle, BundleRest> {

    @Override
    public BundleRest convert(Bundle obj, Projection projection) {
        BundleRest bundleRest = super.convert(obj, projection);
        Bitstream primaryBitstream = obj.getPrimaryBitstream();
        if (primaryBitstream != null) {
            bundleRest.setPrimaryBitstreamUUID(primaryBitstream.getID());
        }
        return bundleRest;
    }

    @Override
    protected BundleRest newInstance() {
        return new BundleRest();
    }

    @Override
    public Class<Bundle> getModelClass() {
        return Bundle.class;
    }
}
