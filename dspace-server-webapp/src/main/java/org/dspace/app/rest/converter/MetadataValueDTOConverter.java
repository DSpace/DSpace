/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.springframework.stereotype.Component;

/**
 * Converter to translate between domain {@link MetadataValue}s and {@link MetadataValueRest} representations.
 */
@Component
public class MetadataValueDTOConverter implements DSpaceConverter<MetadataValueDTO, MetadataValueRest> {

    @Override
    public MetadataValueRest convert(MetadataValueDTO modelObject, Projection projection) {
        MetadataValueRest metadataValueRest = new MetadataValueRest();
        metadataValueRest.setValue(modelObject.getValue());
        metadataValueRest.setLanguage(modelObject.getLanguage());
        metadataValueRest.setAuthority(modelObject.getAuthority());
        metadataValueRest.setConfidence(modelObject.getConfidence());
        return metadataValueRest;    }

    public Class<MetadataValueDTO> getModelClass() {
        return MetadataValueDTO.class;
    }
}
