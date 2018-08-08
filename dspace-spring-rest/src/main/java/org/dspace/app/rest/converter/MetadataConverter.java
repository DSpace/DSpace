/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.MetadataValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class MetadataConverter implements Converter<List<MetadataValue>, MetadataRest> {

    private final MetadataValueConverter valueConverter;

    @Autowired
    public MetadataConverter(MetadataValueConverter valueConverter) {
        this.valueConverter = valueConverter;
    }

    @Override
    public MetadataRest convert(List<MetadataValue> metadataValueList) {
        // Convert each value to a DTO while retaining place order in a map of key -> SortedSet
        Map<String, SortedSet<MetadataValueRest>> mapOfSortedSets = new HashMap<>();
        for (MetadataValue metadataValue : metadataValueList) {
            String key = metadataValue.getMetadataField().toString('.');
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
