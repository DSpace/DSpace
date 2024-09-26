/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.authority.AuthorityValue;

/**
 * Implements the {@link AuthorityVirtualMetadataConfiguration} interface to achieve the generation of Virtual
 * metadata for authority values, in a similar way to the entity-driven {@link VirtualMetadataConfiguration}.
 * The Concatenate bean will take all the Solr document values for the defined fields and concatenate them together
 * with separators for use with the {@link AuthorityVirtualMetadataPopulator}.
 *
 * @author Kim Shepherd
 *
 */
public class AuthorityConcatenate implements AuthorityVirtualMetadataConfiguration {
    /**
     * The field names to retrieve from the Authority Solr document - note these do NOT have to match
     * the field names of the resulting virtual metadata values - they are set as the key of the map that contains
     * this map. (e.g. 'dcterms.spatial -> ['my_solr_spatial_field' -> [x, y, z], 'another_field' -> [x, y, z]])
     */
    private List<String> fields;
    /**
     * The separator used in concatenation
     */
    private String separator;

    /**
     * Generic getter for the fields property
     * @return The list of field names
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Generic setter for the fields property
     * @param fields the list of field names
     */
    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    /**
     * Generic getter for the separator
     * @return The separator used in concatenation
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Generic setter for the separator property
     * @param separator The separator used in concatenation
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * Retrieve the values from the given AuthorityValue "otherMetadataMap" for each configured fields property
     * and return as a list. The authority value is responsible for how to store and return this map.
     *
     * @param authorityValue The authority value from which to build the list of values
     * @return The String values for each field configured
     */
    public List<String> getValues(AuthorityValue authorityValue) {
        List<String> resultValues = new LinkedList<>();
        List<String> fieldsToRetrieve = this.getFields();
        Map<String, List<String>> metadataMap = authorityValue.getOtherMetadataMap();
        List<String> concatenatedValues = new LinkedList<>();
        for (String field : fieldsToRetrieve) {
            List<String> otherMetadata = metadataMap.get(field);
            if (otherMetadata != null) {
                // Concatenate all values of this particular field (e.g. label_keyword)
                concatenatedValues.add(otherMetadata.stream().collect(Collectors.joining(separator)));
            }
        }
        // Concatenate all the individual concatenated values together (e.g. person.lastName with person.firstName)
        resultValues.add(concatenatedValues.stream().collect(Collectors.joining(separator)));

        return resultValues;
    }
}
