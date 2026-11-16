/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.generator;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.MetadataFieldName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.enhancer.source.MetadataSource;
import org.dspace.content.enhancer.source.RawMetadataValue;

/**
 * Strategy that derives persist-ready relationship additional metadata from a related entity.
 *
 * <p>Implementations only declare the single source field they read through the
 * {@link MetadataSource} port and are therefore IO-free: they never depend on the database, on
 * Solr or on an {@code ItemService}.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public interface RelationshipMetadataGenerator {

    /**
     * The metadata field read on the related entity.
     *
     * @return the field in the {@code schema.element[.qualifier]} form
     */
    String getSourceField();

    /**
     * Extract the additional metadata values for this generator from the given source.
     *
     * <p>The default implementation copies the values of {@link #getSourceField()} 1:1, preserving
     * the value, language, authority and confidence of each raw value. The returned DTOs carry the
     * field derived from {@link #getSourceField()}.</p>
     *
     * @param source the source to read the values from
     * @return the extracted values, never {@code null}, possibly empty
     */
    default List<MetadataValueDTO> extract(MetadataSource source) {
        MetadataFieldName fieldName = new MetadataFieldName(getSourceField());
        List<RawMetadataValue> values = source.getValues(getSourceField());
        List<MetadataValueDTO> result = new ArrayList<>(values.size());
        for (RawMetadataValue value : values) {
            result.add(new MetadataValueDTO(fieldName.schema, fieldName.element, fieldName.qualifier,
                value.language(), value.value(), value.authority(), value.confidence()));
        }
        return result;
    }

}
