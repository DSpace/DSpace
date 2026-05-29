/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.duplicatedetection;

import org.dspace.services.ConfigurationService;

/**
 * Represents a duplicate comparison record with a field name, value, and distance.
 * Encapsulates the data required for identifying and comparing duplicates in a system.
 * This record provides utility for constructing Solr-specific field names for deduplication purposes.
 *
 * @param fieldName The name of the field involved in the comparison.
 * @param value The value of the field being compared.
 * @param distance The distance used for determining similarity.
 */
public record DuplicateComparison(String fieldName, String value, int distance) {
    /**
     * Constructs a Solr field prefix for use in deduplication processes by combining configurable
     * prefix and suffix values with a transformed field name from a duplicate comparison record.
     *
     * @param configurationService the configuration service used to fetch prefix and suffix property values.
     * @param duplicateComparison the duplicate comparison record containing the field name to be transformed.
     * @return the constructed Solr field prefix in the format:
     *         [prefix]_[transformed field name]_[suffix].
     */
    public static String getSolrFieldPrefix(ConfigurationService configurationService,
                                            DuplicateComparison duplicateComparison) {
        return configurationService.getProperty("duplicate.comparison.solr.field.prefix", "deduplication") + "_" +
            duplicateComparison.fieldName.replace(".", "_").toLowerCase() + "_" +
            configurationService.getProperty("duplicate.comparison.solr.field.suffix", "keyword");
    }
}
