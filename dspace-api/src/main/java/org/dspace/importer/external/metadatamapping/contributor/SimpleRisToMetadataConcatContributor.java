/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * This contributor extends SimpleRisToMetadataContributor,
 * in particular, this one is able to chain multi values into a single one
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SimpleRisToMetadataConcatContributor extends SimpleRisToMetadataContributor {

    private String tag;

    private MetadataFieldConfig metadata;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Map<String, List<String>> record) {
        List<MetadatumDTO> values = new LinkedList<>();
        StringBuilder text = null;
        List<String> fieldValues = record.get(this.tag);
        if (Objects.nonNull(fieldValues)) {
            text = new StringBuilder();
            for (String value : fieldValues) {
                text.append(value).append(" ");
            }
        }
        if (StringUtils.isNotBlank(text.toString())) {
            values.add(metadataFieldMapping.toDCValue(this.metadata, text.toString()));
        }
        return values;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public MetadataFieldConfig getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataFieldConfig metadata) {
        this.metadata = metadata;
    }

}