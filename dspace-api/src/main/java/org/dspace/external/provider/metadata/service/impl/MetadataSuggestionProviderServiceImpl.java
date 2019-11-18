/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.InProgressSubmission;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;
import org.dspace.external.provider.metadata.service.MetadataSuggestionProviderService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation class for {@link MetadataSuggestionProviderService}
 */
public class MetadataSuggestionProviderServiceImpl implements MetadataSuggestionProviderService {

    @Autowired
    private List<MetadataSuggestionProvider> metadataSuggestionProviders;

    @Override
    public List<MetadataSuggestionProvider> getMetadataSuggestionProviders(InProgressSubmission inProgressSubmission) {

        return metadataSuggestionProviders.stream().filter(
            metadataSuggestionProvider -> metadataSuggestionProvider.supports(inProgressSubmission)).collect(
            Collectors.toList());
    }

    @Override
    public MetadataSuggestionProvider getMetadataSuggestionProvider(String id) {

        for (MetadataSuggestionProvider metadataSuggestionProvider : metadataSuggestionProviders) {
            if (StringUtils.equals(metadataSuggestionProvider.getId(), id)) {
                return metadataSuggestionProvider;
            }
        }
        return null;
    }

    @Override
    public MetadataItemSuggestions getMetadataItemSuggestions(String suggestionName, String entryId,
                                           InProgressSubmission inProgressSubmission) {
        MetadataSuggestionProvider metadataSuggestionProvider = getMetadataSuggestionProvider(suggestionName);
        if (metadataSuggestionProvider != null) {
            Optional<ExternalDataObject> externalDataObjectOptional = metadataSuggestionProvider.getExternalDataProvider().getExternalDataObject(entryId);
            ExternalDataObject externalDataObject = externalDataObjectOptional.orElseThrow(() -> new IllegalArgumentException("Couldn't construct an externalDataObject for the given EntryId: " + entryId + " and suggestionName: " + suggestionName));
            return new MetadataItemSuggestions(externalDataObject, inProgressSubmission);
        }

        return null;
    }
}
