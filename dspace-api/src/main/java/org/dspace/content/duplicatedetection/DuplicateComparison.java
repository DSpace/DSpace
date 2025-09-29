/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.duplicatedetection;

import org.dspace.services.ConfigurationService;

public record DuplicateComparison(String fieldName, String value, int distance) {
    public static String getSolrFieldPrefix(ConfigurationService configurationService,
                                            DuplicateComparison duplicateComparison) {
        return configurationService.getProperty("duplicate.comparison.solr.field.prefix", "deduplication") + "_" +
            duplicateComparison.fieldName.replace(".", "_").toLowerCase() + "_" +
            configurationService.getProperty("duplicate.comparison.solr.field.suffix", "keyword");
    }
}
