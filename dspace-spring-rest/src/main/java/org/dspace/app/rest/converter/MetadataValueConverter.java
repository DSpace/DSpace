/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.MetadataValue;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class MetadataValueConverter implements Converter<MetadataValue, MetadataValueRest> {

    @Override
    public MetadataValueRest convert(MetadataValue model) {
        MetadataValueRest metadataValueRest = new MetadataValueRest();
        metadataValueRest.setValue(model.getValue());
        metadataValueRest.setLanguage(model.getLanguage());
        metadataValueRest.setAuthority(model.getAuthority());
        metadataValueRest.setConfidence(model.getConfidence());
        metadataValueRest.setPlace(model.getPlace());
        return metadataValueRest;
    }
}
