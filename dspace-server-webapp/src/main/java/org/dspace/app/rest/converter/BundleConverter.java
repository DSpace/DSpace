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
import org.dspace.content.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BundleConverter
    extends DSpaceObjectConverter<org.dspace.content.Bundle, org.dspace.app.rest.model.BundleRest> {

    @Autowired
    BitstreamConverter bitstreamConverter;

    protected BundleRest newInstance() {
        return new BundleRest();
    }

    protected Class<Bundle> getModelClass() {
        return Bundle.class;
    }

    public BundleRest fromModel(Bundle obj) {
        BundleRest bundle = (BundleRest) super.fromModel(obj);

        bundle.setBitstreams(obj.getBitstreams()
                                .stream()
                                .map(x -> bitstreamConverter.fromModel(x))
                                .collect(Collectors.toList()));

        if (obj.getPrimaryBitstream() != null) {
            BitstreamRest primaryBitstreamRest = bitstreamConverter.fromModel(obj.getPrimaryBitstream());
            bundle.setPrimaryBitstream(primaryBitstreamRest);
        }

        return bundle;
    }

    public Bundle toModel(BundleRest obj) {
        return null;
    }
}

