/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.dspace.content.MetadataFieldName;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * This class contains all MetadatumDTO objects from an imported item
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class ImportRecord {
    private List<MetadatumDTO> valueList = null;

    /**
     * Retrieve an unmodifiableList of MetadatumDTO
     *
     * @return List of MetadatumDTO
     */
    public List<MetadatumDTO> getValueList() {
        return Collections.unmodifiableList(valueList);
    }

    /**
     * Create an ImportRecord instance initialized with a List of MetadatumDTO objects
     *
     * @param valueList list of metadata values
     */
    public ImportRecord(List<MetadatumDTO> valueList) {
        //don't want to alter the original list. Also now I can control the type of list
        this.valueList = new ArrayList<>(valueList);
    }

    /**
     * Build a string based on the values in the valueList object
     * The syntax will be
     * Record{valueList={"schema"; "element" ; "qualifier"; "value"}}
     *
     * @return a concatenated string containing all values of the MetadatumDTO objects in valueList
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Record");
        sb.append("{valueList=");
        for (MetadatumDTO val : valueList) {
            sb.append("{");
            sb.append(val.getSchema());
            sb.append("; ");
            sb.append(val.getElement());
            sb.append("; ");

            sb.append(val.getQualifier());
            sb.append("; ");

            sb.append(val.getValue());
            sb.append("; ");
            sb.append("}\n");

        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Return the MetadatumDTO's that are related to a given schema/element/qualifier pair/triplet
     *
     * @param schema    metadata field schema
     * @param element   metadata field element
     * @param qualifier metadata field qualifier
     * @return the MetadatumDTO's that are related to a given schema/element/qualifier pair/triplet
     */
    public Collection<MetadatumDTO> getValue(String schema, String element, String qualifier) {
        List<MetadatumDTO> values = new ArrayList<MetadatumDTO>();
        for (MetadatumDTO value : valueList) {
            if (value.getSchema().equals(schema) && value.getElement().equals(element)) {
                if (qualifier == null && value.getQualifier() == null) {
                    values.add(value);
                } else if (value.getQualifier() != null && value.getQualifier().equals(qualifier)) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    /**
     * Returns an {@code Optional<String>} representing the value
     * of the metadata {@code field} found inside the {@code valueList}.
     * @param field String of the MetadataField to search
     * @return {@code Optional<String>} non empty if found.
     */
    public Optional<String> getSingleValue(String field) {
        MetadataFieldName metadataFieldName = new MetadataFieldName(field);
        return getSingleValue(metadataFieldName.schema, metadataFieldName.element, metadataFieldName.qualifier);
    }

    /**
     * Retrieves a single value for the given schema, element, and qualifier.
     *
     * @param  schema    the schema for the value
     * @param  element   the element for the value
     * @param  qualifier the qualifier for the value
     * @return           an optional containing the single value, if present
     */
    public Optional<String> getSingleValue(String schema, String element, String qualifier) {
        return getValue(schema, element, qualifier).stream()
            .map(MetadatumDTO::getValue)
            .findFirst();
    }

    /**
     * Add a value to the valueList
     *
     * @param value The MetadatumDTO to add to the valueList
     */
    public void addValue(MetadatumDTO value) {
        this.valueList.add(value);
    }
}
