/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;

public class MockDataProvider implements ExternalDataProvider {

    private Map<String, ExternalDataObject> mockLookupMap;
    private String sourceIdentifier;

    /**
     * Generic getter for the sourceIdentifier
     * @return the sourceIdentifier value of this MockDataProvider
     */
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        ExternalDataObject externalDataObject = mockLookupMap.get(id);
        if (externalDataObject == null) {
            return Optional.empty();
        } else {
            return Optional.of(externalDataObject);
        }
    }

    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        List<ExternalDataObject> listToReturn = new LinkedList<>();
        for (Map.Entry<String, ExternalDataObject> entry : mockLookupMap.entrySet()) {
            if (StringUtils.containsIgnoreCase(entry.getKey(), query)) {
                listToReturn.add(entry.getValue());
            }

        }
        return listToReturn;
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        return searchExternalDataObjects(query, 0, 100).size();
    }

    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this MockDataProvider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public void init() throws IOException {
        mockLookupMap = new HashMap<>();
        List<String> externalDataObjectsToMake = new LinkedList<>();
        externalDataObjectsToMake.add("one");
        externalDataObjectsToMake.add("two");
        externalDataObjectsToMake.add("three");
        externalDataObjectsToMake.add("onetwo");

        for (String id : externalDataObjectsToMake) {
            ExternalDataObject externalDataObject = new ExternalDataObject("mock");
            externalDataObject.setId(id);
            externalDataObject.setValue(id);
            externalDataObject.setDisplayValue(id);
            List<MetadataValueDTO> list = new LinkedList<>();
            list.add(new MetadataValueDTO("dc", "contributor", "author", null, "Donald, Smith"));
            externalDataObject.setMetadata(list);

            mockLookupMap.put(id, externalDataObject);
        }
    }
}
