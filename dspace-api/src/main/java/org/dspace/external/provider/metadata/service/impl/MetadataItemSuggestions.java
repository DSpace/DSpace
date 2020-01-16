/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.service.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.content.InProgressSubmission;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;

/**
 *  This class will take an {@link ExternalDataObject} and an {@link InProgressSubmission} and calculate the changes
 *  that the
 *  ExternalDataObject suggests
 */
public class MetadataItemSuggestions {

    private ExternalDataObject externalDataObject;
    private InProgressSubmission inProgressSubmission;
    private List<MetadataChange> metadataChanges;

    protected MetadataItemSuggestions(ExternalDataObject externalDataObject,
                                      InProgressSubmission inProgressSubmission) {
        this.externalDataObject = externalDataObject;
        this.inProgressSubmission = inProgressSubmission;
    }

    /**
     * Generic getter for the externalDataObject
     * @return the externalDataObject value of this MetadataItemSuggestions
     */
    public ExternalDataObject getExternalDataObject() {
        return externalDataObject;
    }

    /**
     * Generic setter for the externalDataObject
     * @param externalDataObject   The externalDataObject to be set on this MetadataItemSuggestions
     */
    public void setExternalDataObject(ExternalDataObject externalDataObject) {
        this.externalDataObject = externalDataObject;
    }

    /**
     * Generic getter for the metadataChanges
     * @return the metadataChanges value of this MetadataItemSuggestions
     */
    public List<MetadataChange> getMetadataChanges() {
        if (metadataChanges == null) {
            constructMetadataChanges();
        }
        return metadataChanges;
    }

    /**
     * This method will loop over the metadata of the InProgressSubmission object and it'll compare that metadata
     * to the metadata for the ExternalDataObject and it'll calculate the changes that can be made based on that
     */
    private void constructMetadataChanges() {
        metadataChanges = new LinkedList<>();
        Map<String, List<String>> inProgressSubmissionMetadataMap = getInProgressSubmissionMetadata();

        for (MetadataValueDTO mockMetadataValue : externalDataObject.getMetadata()) {
            if (!inProgressSubmissionMetadataMap.containsKey(mockMetadataValue.getKey())) {
                metadataChanges
                    .add(new MetadataChange("add/metadata/" + mockMetadataValue.getKey() + "/-",
                                            mockMetadataValue.getKey(), mockMetadataValue.getValue()));
            }
        }
    }

    /**
     * This method will produce a map with the metadatafield String representation as key and the list of its values
     * as value of the map. This metadata is metadata that is currently present on the InProgressSubmission's item
     * @return  The map with the metadatafield string key and list of values
     */
    public Map<String, List<String>> getInProgressSubmissionMetadata() {
        Map<String, List<String>> map = new HashMap<>();
        List<MetadataValueDTO> mockMetadataFromInProgressSubmission =
            inProgressSubmission.getItem().getMetadata().stream().map(
                metadataValue -> new MetadataValueDTO(metadataValue)).collect(Collectors.toList());
        mockMetadataFromInProgressSubmission.stream().forEach(mockMetadataValue -> {
            map.computeIfAbsent(mockMetadataValue.getKey(), k -> new LinkedList<>())
                .add(mockMetadataValue.getValue());
        });

        return map;
    }

    /**
     * Generic setter for the metadataChanges
     * @param metadataChanges   The metadataChanges to be set on this MetadataItemSuggestions
     */
    public void setMetadataChanges(List<MetadataChange> metadataChanges) {
        this.metadataChanges = metadataChanges;
    }

    /**
     * Generic getter for the inProgressSubmission
     * @return the inProgressSubmission value of this MetadataItemSuggestions
     */
    public InProgressSubmission getInProgressSubmission() {
        return inProgressSubmission;
    }

    /**
     * Generic setter for the inProgressSubmission
     * @param inProgressSubmission   The inProgressSubmission to be set on this MetadataItemSuggestions
     */
    public void setInProgressSubmission(InProgressSubmission inProgressSubmission) {
        this.inProgressSubmission = inProgressSubmission;
    }
}
