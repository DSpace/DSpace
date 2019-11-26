/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to translate between lists of domain {@link MetadataValue}s and {@link MetadataRest} representations.
 */
@Component
public class MetadataValueDTOListConverter implements Converter<List<MetadataValueDTO>, MetadataRest> {

    @Autowired
    private MetadataValueDTOConverter valueConverter;

    /**
     * Gets a rest representation of the given list of domain metadata values.
     *
     * @param metadataValueList the domain values.
     * @return the rest representation.
     */
    @Override
    public MetadataRest convert(List<MetadataValueDTO> metadataValueList) {
        // Convert each value to a DTO while retaining place order in a map of key -> SortedSet
        Map<String, List<MetadataValueRest>> mapOfLists = new HashMap<>();
        for (MetadataValueDTO metadataValue : metadataValueList) {
            String key = metadataValue.getSchema() + "." + metadataValue.getElement();
            if (StringUtils.isNotBlank(metadataValue.getQualifier())) {
                key += "." + metadataValue.getQualifier();
            }
            List<MetadataValueRest> list = mapOfLists.get(key);
            if (list == null) {
                list = new LinkedList();
                mapOfLists.put(key, list);
            }
            list.add(valueConverter.convert(metadataValue));
        }

        MetadataRest metadataRest = new MetadataRest();

        // Populate MetadataRest's map of key -> List while respecting SortedSet's order
        Map<String, List<MetadataValueRest>> metadataRestMap = metadataRest.getMap();
        for (Map.Entry<String, List<MetadataValueRest>> entry : mapOfLists.entrySet()) {
            metadataRestMap.put(entry.getKey(), entry.getValue().stream().collect(Collectors.toList()));
        }

        return metadataRest;
    }

}
