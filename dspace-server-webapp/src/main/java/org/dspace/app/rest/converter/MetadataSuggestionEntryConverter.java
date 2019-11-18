/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataSuggestionEntryRest;
import org.dspace.external.provider.metadata.service.impl.MetadataItemSuggestions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class acts as a converter from the {@link MetadataItemSuggestions} to their REST representation in
 * {@link MetadataSuggestionEntryRest} objects
 */
@Component
public class MetadataSuggestionEntryConverter implements
                                              DSpaceConverter<MetadataItemSuggestions, MetadataSuggestionEntryRest> {

    @Autowired
    private MockMetadataConverter metadataConverter;

    @Autowired
    private MetadataChangeConverter metadataChangeConverter;

    @Override
    public MetadataSuggestionEntryRest fromModel(MetadataItemSuggestions obj) {
        MetadataSuggestionEntryRest metadataSuggestionEntryRest = new MetadataSuggestionEntryRest();
        metadataSuggestionEntryRest.setDisplay(obj.getExternalDataObject().getDisplayValue());
        metadataSuggestionEntryRest.setId(obj.getExternalDataObject().getId());
        metadataSuggestionEntryRest.setValue(obj.getExternalDataObject().getValue());
        metadataSuggestionEntryRest.setMetadataRest(metadataConverter.convert(obj.getExternalDataObject().getMetadata()));
        metadataSuggestionEntryRest.setMetadataSuggestion(obj.getExternalDataObject().getSource());
        metadataSuggestionEntryRest.setMetadataChangeRest(metadataChangeConverter.convert(obj.getMetadataChanges()));
        return metadataSuggestionEntryRest;
    }

    @Override
    public MetadataItemSuggestions toModel(MetadataSuggestionEntryRest obj) {
        return null;
    }
}
