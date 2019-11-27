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
import org.dspace.content.dto.MetadataValueDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to translate between domain {@link MetadataValue}s and {@link MetadataValueRest} representations.
 */
@Component
public class MetadataValueDTOConverter implements Converter<MetadataValueDTO, MetadataValueRest> {

    /**
     * Gets a rest representation of the given domain metadata value.
     *
     * @param metadataValue the domain value.
     * @return the rest representation.
     */
    @Override
    public MetadataValueRest convert(MetadataValueDTO metadataValue) {
        MetadataValueRest metadataValueRest = new MetadataValueRest();
        metadataValueRest.setValue(metadataValue.getValue());
        metadataValueRest.setLanguage(metadataValue.getLanguage());
        metadataValueRest.setAuthority(metadataValue.getAuthority());
        metadataValueRest.setConfidence(metadataValue.getConfidence());
        return metadataValueRest;
    }
}
