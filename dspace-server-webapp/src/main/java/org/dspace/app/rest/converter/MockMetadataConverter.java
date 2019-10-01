/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.MetadataValue;
import org.dspace.mock.MockMetadataValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to translate between lists of domain {@link MetadataValue}s and {@link MetadataRest} representations.
 */
@Component
public class MockMetadataConverter implements Converter<List<MockMetadataValue>, MetadataRest> {

    @Autowired
    private MockMetadataValueConverter valueConverter;

    /**
     * Gets a rest representation of the given list of domain metadata values.
     *
     * @param metadataValueList the domain values.
     * @return the rest representation.
     */
    @Override
    public MetadataRest convert(List<MockMetadataValue> metadataValueList) {
        // Convert each value to a DTO while retaining place order in a map of key -> SortedSet
        Map<String, SortedSet<MetadataValueRest>> mapOfSortedSets = new HashMap<>();
        for (MockMetadataValue metadataValue : metadataValueList) {
            String key = metadataValue.getSchema() + "." + metadataValue.getElement();
            if (StringUtils.isNotBlank(metadataValue.getQualifier())) {
                key += "." + metadataValue.getQualifier();
            }
            SortedSet<MetadataValueRest> set = mapOfSortedSets.get(key);
            if (set == null) {
                set = new TreeSet<>(Comparator.comparingInt(MetadataValueRest::getPlace));
                mapOfSortedSets.put(key, set);
            }
            set.add(valueConverter.convert(metadataValue));
        }

        MetadataRest metadataRest = new MetadataRest();

        // Populate MetadataRest's map of key -> List while respecting SortedSet's order
        Map<String, List<MetadataValueRest>> mapOfLists = metadataRest.getMap();
        for (Map.Entry<String, SortedSet<MetadataValueRest>> entry : mapOfSortedSets.entrySet()) {
            mapOfLists.put(entry.getKey(), entry.getValue().stream().collect(Collectors.toList()));
        }

        return metadataRest;
    }

}
