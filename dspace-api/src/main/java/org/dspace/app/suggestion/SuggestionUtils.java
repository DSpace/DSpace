/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.external.model.ExternalDataObject;

/**
 * This utility class provides convenient methods to deal with the
 * {@link ExternalDataObject} for the purpose of the Suggestion framework
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionUtils {
    private SuggestionUtils() {
    }
    /**
     * This method receive an ExternalDataObject and a metadatum key.
     * It return only the values of the Metadata associated with the key.
     * 
     * @param record the ExternalDataObject to extract metadata from
     * @param schema schema of the searching record
     * @param element element of the searching record
     * @param qualifier qualifier of the searching record
     * @return value of the first matching record
     */
    public static List<String> getAllEntriesByMetadatum(ExternalDataObject record, String schema, String element,
            String qualifier) {
        return record.getMetadata().stream()
                .filter(x ->
                    StringUtils.equals(x.getSchema(), schema)
                        && StringUtils.equals(x.getElement(), element)
                        && StringUtils.equals(x.getQualifier(), qualifier))
                .map(x -> x.getValue()).collect(Collectors.toList());
    }

    /**
     * This method receive an ExternalDataObject and a metadatum key.
     * It return only the values of the Metadata associated with the key.
     * 
     * @param record the ExternalDataObject to extract metadata from
     * @param metadataFieldKey the metadata field key (i.e. dc.title or dc.contributor.author),
     *      the jolly char is not supported
     * @return value of the first matching record
     */
    public static List<String> getAllEntriesByMetadatum(ExternalDataObject record, String metadataFieldKey) {
        if (metadataFieldKey == null) {
            return Collections.EMPTY_LIST;
        }
        String[] fields = metadataFieldKey.split("\\.");
        String schema = fields[0];
        String element = fields[1];
        String qualifier = null;
        if (fields.length == 3) {
            qualifier = fields[2];
        }
        return getAllEntriesByMetadatum(record, schema, element, qualifier);
    }

    /**
     * This method receive and ExternalDataObject and a metadatum key.
     * It return only the value of the first Metadatum from the list associated with the key.
     * 
     * @param record the ExternalDataObject to extract metadata from
     * @param schema schema of the searching record
     * @param element element of the searching record
     * @param qualifier qualifier of the searching record
     * @return value of the first matching record
     */
    public static String getFirstEntryByMetadatum(ExternalDataObject record, String schema, String element,
            String qualifier) {
        return record.getMetadata().stream()
                .filter(x ->
                    StringUtils.equals(x.getSchema(), schema)
                        && StringUtils.equals(x.getElement(), element)
                        && StringUtils.equals(x.getQualifier(), qualifier))
                .map(x -> x.getValue()).findFirst().orElse(null);
    }

    /**
     * This method receive and ExternalDataObject and a metadatum key.
     * It return only the value of the first Metadatum from the list associated with the key.
     * 
     * @param record the ExternalDataObject to extract metadata from
     * @param metadataFieldKey the metadata field key (i.e. dc.title or dc.contributor.author),
     *      the jolly char is not supported
     * @return value of the first matching record
     */
    public static String getFirstEntryByMetadatum(ExternalDataObject record, String metadataFieldKey) {
        if (metadataFieldKey == null) {
            return null;
        }
        String[] fields = metadataFieldKey.split("\\.");
        String schema = fields[0];
        String element = fields[1];
        String qualifier = null;
        if (fields.length == 3) {
            qualifier = fields[2];
        }
        return getFirstEntryByMetadatum(record, schema, element, qualifier);
    }
}