/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.service.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.InProgressSubmission;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;
import org.dspace.external.provider.metadata.service.MetadataSuggestionProviderService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation class for {@link MetadataSuggestionProviderService}
 */
public class MetadataSuggestionProviderServiceImpl implements MetadataSuggestionProviderService {

    @Autowired(required = false)
    private List<MetadataSuggestionProvider> metadataSuggestionProviders = new ArrayList<>();

    @Override
    public List<MetadataSuggestionProvider> getMetadataSuggestionProviders(InProgressSubmission inProgressSubmission) {

        return metadataSuggestionProviders.stream().filter(
            metadataSuggestionProvider -> metadataSuggestionProvider.supports(inProgressSubmission, null, null,
                                                                              false)).collect(
            Collectors.toList());
    }

    @Override
    public Optional<MetadataSuggestionProvider> getMetadataSuggestionProvider(String id) {

        for (MetadataSuggestionProvider metadataSuggestionProvider : metadataSuggestionProviders) {
            if (StringUtils.equals(metadataSuggestionProvider.getId(), id)) {
                return Optional.of(metadataSuggestionProvider);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<MetadataItemSuggestions> getMetadataItemSuggestions(String suggestionName, String entryId,
                                                                        InProgressSubmission inProgressSubmission) {
        Optional<MetadataSuggestionProvider> metadataSuggestionProviderOptional = getMetadataSuggestionProvider(
            suggestionName);
        if (metadataSuggestionProviderOptional.isPresent()) {
            MetadataSuggestionProvider metadataSuggestionProvider = metadataSuggestionProviderOptional.get();
            Optional<ExternalDataObject> externalDataObjectOptional = metadataSuggestionProvider
                .getExternalDataProvider().getExternalDataObject(entryId);
            ExternalDataObject externalDataObject = externalDataObjectOptional
                .orElseThrow(() -> new IllegalArgumentException("Couldn't construct an externalDataObject for the " +
                                                                    "given EntryId: " + entryId + " and " +
                                                                    "suggestionName: " + suggestionName));
            return Optional.of(new MetadataItemSuggestions(externalDataObject, inProgressSubmission));
        }

        return Optional.empty();
    }

    @Override
    public Optional<MetadataSuggestionDifferences> getMetadataSuggestionDifferences(String suggestionName,
                                                    String entryId, InProgressSubmission inProgressSubmission) {

        // Here we fetch the MetadataItemSuggestions object from which we'll start cause it contains
        // the changes and the map of metadata for the InProgressSubmissions so we don't have to
        // calculate this again
        Optional<MetadataItemSuggestions> optional = getMetadataItemSuggestions(suggestionName, entryId,
                                                                                inProgressSubmission);
        if (optional.isPresent()) {
            MetadataItemSuggestions metadataItemSuggestions = optional.get();
            List<MetadataChange> metadataChanges = metadataItemSuggestions.getMetadataChanges();
            Map<String, List<String>> inProgressSubmissionMetadataMap = metadataItemSuggestions
                .getInProgressSubmissionMetadata();
            // Create a new MetadataSuggestionDifferences object so we can start filling up the map
            MetadataSuggestionDifferences metadataSuggestionDifferences =
                new MetadataSuggestionDifferences(suggestionName, inProgressSubmission, entryId);
            // Traverse all our changes to see what we need to add to the map
            for (MetadataChange metadataChange : metadataChanges) {
                MetadataSuggestionDifference metadataSuggestionDifference = metadataSuggestionDifferences
                    .getDifference(metadataChange.getMetadataKey());
                // If the key already exists, we add it to the MetadataSuggestionDifference object in that value
                if (metadataSuggestionDifference != null) {
                    metadataSuggestionDifference.addMetadataChange(metadataChange);
                } else {
                    // If the key didn't exist, we have to make a new MetadataSuggestionDifference object
                    // and populate it with the changes (that contains only this one metadataChange object for now)
                    // the current values for the metadata field and add the MetadataSuggestionDifference to the big map
                    // with the appropriate key
                    metadataSuggestionDifference = new MetadataSuggestionDifference();
                    List<MetadataChange> list = new LinkedList<>();
                    list.add(metadataChange);
                    metadataSuggestionDifference.setMetadataChanges(list);
                    metadataSuggestionDifference.setCurrentValues(inProgressSubmissionMetadataMap
                                                                      .get(metadataChange.getMetadataKey()));
                    metadataSuggestionDifferences.addDifference(metadataChange.getMetadataKey(),
                                                                metadataSuggestionDifference);
                }
            }
            return Optional.of(metadataSuggestionDifferences);
        }
        return Optional.empty();

    }

    public List<MetadataItemSuggestions> getMetadataSuggestionEntryRests(
        MetadataSuggestionProvider metadataSuggestionProvider, InProgressSubmission inProgressSubmission, String query,
        Bitstream bitstream, boolean useMetadata,
        int start, int limit) {
        List<ExternalDataObject> list = new LinkedList<>();

        if (StringUtils.isNotBlank(query)) {
            list = metadataSuggestionProvider.query(query, start, limit);
        } else if (bitstream != null) {
            list = metadataSuggestionProvider.bitstreamQuery(bitstream);
        } else if (useMetadata) {
            list = metadataSuggestionProvider.metadataQuery(inProgressSubmission.getItem(), start, limit);
        }

        List<MetadataItemSuggestions> listToReturn = new LinkedList<>();
        for (ExternalDataObject externalDataObject : list) {
            listToReturn.add(new MetadataItemSuggestions(externalDataObject, inProgressSubmission));
        }
        return listToReturn;
    }


}
