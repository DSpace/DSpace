/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataSuggestionsSourceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;
import org.springframework.stereotype.Component;

/**
 * This converter deals with the conversion between MetadataSuggestionProvider objects and
 * MetadataSuggestionsSourceRest objects
 */
@Component
public class MetadataSuggestionsSourceRestConverter implements
    DSpaceConverter<MetadataSuggestionProvider, MetadataSuggestionsSourceRest> {

    @Override
    public MetadataSuggestionsSourceRest convert(MetadataSuggestionProvider obj, Projection projection) {
        MetadataSuggestionsSourceRest metadataSuggestionsSourceRest = new MetadataSuggestionsSourceRest();
        metadataSuggestionsSourceRest.setId(obj.getId());
        metadataSuggestionsSourceRest.setName(obj.getId());
        metadataSuggestionsSourceRest.setFileBased(obj.isFileBased());
        metadataSuggestionsSourceRest.setMetadataBased(obj.isMetadataBased());
        metadataSuggestionsSourceRest.setQueryBased(obj.isQueryBased());
        return metadataSuggestionsSourceRest;
    }

    @Override
    public Class<MetadataSuggestionProvider> getModelClass() {
        return MetadataSuggestionProvider.class;
    }
}
