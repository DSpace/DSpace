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
 * Metadata contributor that takes an PlainMetadataSourceDto instance and turns it into a
 * collection of metadatum
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
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
     * In case of success, new metadatum is constructor (using field elements and PlainMetadataSourceDto value)
     * and added to the list.
     *
     * @param t A class to retrieve metadata and key to match from. t and contained list "metadata" MUST be not null.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(PlainMetadataSourceDto t) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (PlainMetadataKeyValueItem metadatum : t.getMetadata()) {
            if (key.equals(metadatum.getKey())) {
                MetadatumDTO dcValue = new MetadatumDTO();
                dcValue.setValue(metadatum.getValue());
                dcValue.setElement(field.getElement());
                dcValue.setQualifier(field.getQualifier());
                dcValue.setSchema(field.getSchema());
                values.add(dcValue);
            }
        }
        return values;
    }

    /**
     * Method to inject field item
     * 
     * @param field the {@link MetadataFieldConfig} to use in this contributor
     */
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /**
     * Method to inject key value
     */
    public void setKey(String key) {
        this.key = key;
    }

     /**
     * Method to retrieve field item
     */
    public String getKey() {
        return key;
    }

    /**
     * Method to retrieve the {@link MetadataFieldConfig} used in this contributor
     */
    public MetadataFieldConfig getField() {
        return field;
    }
}
