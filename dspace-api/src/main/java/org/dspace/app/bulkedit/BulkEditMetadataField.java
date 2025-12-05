/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to parse raw key values from a bulk edit import to metadata field properties
 *
 * Usage: BulkEditMetadataField parsedFieldObject = BulkEditMetadataField.parse("dc.title[nl]");
 */
public class BulkEditMetadataField {
    /**
     * Regex to match and retrieve metadata field properties from the bulk edit key
     *
     * Expected structure: ({anything}:){schema}.{element}(.{qualifier})([{language}])
     *  where segments between parentheses are optional and result in null values for the properties within
     *
     * Examples:
     *  - "dc.title" results in schema "dc", element "title", qualifier null and language null
     *  - "someprefix:dc.description.abstract[en]" results in schema "dc", element "description", qualifier "abstract"
     *     and language "en"
     *  - "dc.subject[fr]" results in schema "dc", element "subject", qualifier null and language "fr"
     *  - "dc" results in null values across the board (regex doesn't match)
     */
    private static final String regex = "(?:[^:]+:)?(?<schema>[^.\\[]+)\\.(?<element>[^.\\[]+)(?:\\." +
        "(?<qualifier>[^\\[]+))?(?:\\[(?<language>[^\\]]+)\\])?";

    /**
     * The original field key the properties were parsed from
     */
    private String field;

    private String schema;
    private String element;
    private String qualifier;
    private String language;

    private BulkEditMetadataField(String field) {
        this.field = field;
    }

    public static BulkEditMetadataField parse(String field) {
        BulkEditMetadataField bulkEditMetadataField = new BulkEditMetadataField(field);

        Matcher matcher = Pattern.compile(regex).matcher(field);
        if (matcher.matches()) {
            bulkEditMetadataField.schema = matcher.group("schema");
            bulkEditMetadataField.element = matcher.group("element");
            bulkEditMetadataField.qualifier = matcher.group("qualifier");
            bulkEditMetadataField.language = matcher.group("language");
        }

        return bulkEditMetadataField;
    }

    public String getMetadataField(String separator) {
        return schema + separator + field + (qualifier != null ? (separator + qualifier) : "");
    }

    public String getField() {
        return field;
    }

    public String getSchema() {
        return schema;
    }

    public String getElement() {
        return element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getLanguage() {
        return language;
    }
}
