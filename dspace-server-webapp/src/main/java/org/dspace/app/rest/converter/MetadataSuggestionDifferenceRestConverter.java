/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataDifferenceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.external.provider.metadata.service.impl.MetadataSuggestionDifference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetadataSuggestionDifferenceRestConverter
    implements DSpaceConverter<MetadataSuggestionDifference, MetadataDifferenceRest> {
    @Autowired
    private MetadataChangeConverter metadataChangeConverter;

    public MetadataDifferenceRest convert(MetadataSuggestionDifference modelObject, Projection projection) {
        MetadataDifferenceRest metadataDifferenceRest = new MetadataDifferenceRest();
        if (modelObject.getCurrentValues() != null) {
            metadataDifferenceRest.setCurrentValues(modelObject.getCurrentValues());
        }
        metadataDifferenceRest.setSuggestions(metadataChangeConverter.convert(modelObject.getMetadataChanges()));
        return metadataDifferenceRest;
    }

    public Class<MetadataSuggestionDifference> getModelClass() {
        return MetadataSuggestionDifference.class;
    }
}
