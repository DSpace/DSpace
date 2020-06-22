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

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;

/**
 * Metadata contributor that takes an PlainMetadataSourceDto instance and turns it into a metadatum
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class SimpleMetadataContributor implements MetadataContributor<PlainMetadataSourceDto> {

    private MetadataFieldConfig field;

    private String key;

    private MetadataFieldMapping<PlainMetadataSourceDto,
        MetadataContributor<PlainMetadataSourceDto>> metadataFieldMapping;

    public SimpleMetadataContributor(MetadataFieldConfig field, String key) {
        this.field = field;
        this.key = key;
    }

    public SimpleMetadataContributor() { }

    /**
     * Set the metadataFieldMapping of this SimpleMetadataContributor
     *
     * @param metadataFieldMapping the new mapping.
     */
    @Override
    public void setMetadataFieldMapping(
        MetadataFieldMapping<PlainMetadataSourceDto,
        MetadataContributor<PlainMetadataSourceDto>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
    }

    /**
     * Retrieve the metadata associated with the given object.
     * It match the key found in PlainMetadataSourceDto instance with the key passed to constructor.
     * In case of success, new metadatum is constructer (using field elements and PlainMetadataSourceDto value)
     * and added to the list.
     *
     * @param t A class to retrieve metadata and key to match from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(PlainMetadataSourceDto t) {
        List<MetadatumDTO> values = new LinkedList<>();
        try {
            for (PlainMetadataKeyValueItem metadatum : t.getMetadata()) {
                if (metadatum.getKey().equals(key)) {
                    MetadatumDTO dcValue = new MetadatumDTO();
                    dcValue.setValue(metadatum.getValue());
                    dcValue.setElement(field.getElement());
                    dcValue.setQualifier(field.getQualifier());
                    dcValue.setSchema(field.getSchema());
                    values.add(dcValue);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return values;
    }

    /*
     * Setter to inject field item
     */
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /*
     * Setter to inject key value
     */
    public void setKey(String key) {
        this.key = key;
    }

}
