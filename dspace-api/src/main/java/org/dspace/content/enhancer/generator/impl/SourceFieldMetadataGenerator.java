/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.generator.impl;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.MetadataFieldName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.enhancer.generator.RelationshipMetadataGenerator;
import org.dspace.content.enhancer.source.MetadataSource;
import org.dspace.content.enhancer.source.RawMetadataValue;

/**
 * Default {@link RelationshipMetadataGenerator} that copies the values of its configured field
 * from the related entity.
 *
 * <p>This bean is meant to be declared in configuration with a {@code sourceField} property, e.g.:</p>
 *
 * <pre>
 * &lt;bean id="authorOrcidMetadataGenerator"
 *       class="org.dspace.content.enhancer.generator.impl.SourceFieldMetadataGenerator"&gt;
 *   &lt;property name="sourceField" value="person.identifier.orcid"/&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class SourceFieldMetadataGenerator implements RelationshipMetadataGenerator {

    private String sourceField;

    @Override
    public String getSourceField() {
        return sourceField;
    }

    /**
     * Set the metadata field to copy from the related entity.
     *
     * @param sourceField the field in the {@code schema.element[.qualifier]} form
     */
    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    @Override
    public List<MetadataValueDTO> extract(MetadataSource source) {
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
