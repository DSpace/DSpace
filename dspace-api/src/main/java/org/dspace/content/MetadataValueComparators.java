/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains only static members that can be used
 * to sort list of {@link MetadataValue}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public final class MetadataValueComparators {

    private MetadataValueComparators() {}

    /**
     * This is the default comparator that mimics the ordering
     * applied by the standard {@code @OrderBy} annotation inside
     * {@link DSpaceObject#getMetadata()}
     */
    public static final Comparator<MetadataValue> defaultComparator =
        Comparator.comparing(MetadataValue::getMetadataFieldId)
            .thenComparing(
                MetadataValue::getPlace,
                Comparator.nullsFirst(Comparator.naturalOrder())
            );

    /**
     * This method creates a new {@code List<MetadataValue>} ordered by the
     * {@code MetadataComparators#defaultComparator}.
     *
     * @param metadataValues
     * @return {@code List<MetadataValue>} ordered copy list using stream.
     */
    public static final List<MetadataValue> sort(List<MetadataValue> metadataValues) {
        return metadataValues
                .stream()
                .sorted(MetadataValueComparators.defaultComparator)
                .collect(Collectors.toList());
    }

}
