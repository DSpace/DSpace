/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.stream.Collectors;

import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bundle;
import org.springframework.stereotype.Component;

@Component
public class BundleConverter
    extends DSpaceObjectConverter<Bundle, BundleRest> {

    @Override
    public BundleRest convert(Bundle bundle, Projection projection) {
        BundleRest bundleRest = super.convert(bundle, projection);

        bundleRest.setBitstreams(bundle.getBitstreams()
                                .stream()
                                .map(x -> (BitstreamRest) converter.toRest(x, projection))
                                .collect(Collectors.toList()));

        if (bundle.getPrimaryBitstream() != null) {
            BitstreamRest primaryBitstreamRest = converter.toRest(bundle.getPrimaryBitstream(), projection);
            bundleRest.setPrimaryBitstream(primaryBitstreamRest);
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
