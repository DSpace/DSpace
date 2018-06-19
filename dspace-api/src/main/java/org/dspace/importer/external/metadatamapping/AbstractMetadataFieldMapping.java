/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping;


import org.apache.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.importer.external.metadatamapping.transform.MetadataProcessorService;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that implements {@link MetadataFieldMapping}
 * This class adds a default implementation for the MetadataFieldMapping methods
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public abstract class AbstractMetadataFieldMapping<RecordType> implements MetadataFieldMapping<RecordType, MetadataContributor<RecordType>> {

    private Map<MetadataFieldConfig, MetadataContributor<RecordType>> metadataFieldMap;

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AbstractMetadataFieldMapping.class);

    /* A map containing what processing has to be done on a given metadataFieldConfig.
     * The processing of a value is used to determine the actual value that will be returned used.
     */
    private Map<MetadataFieldConfig, MetadataProcessorService> metadataProcessorMap;

    /**
     * Set a map of metadataprocessors. This map is used to process metadata to make it more compliant for certain metadata fields
     * @param metadataProcessorMap
     */
    public void setMetadataProcessorMap(Map<MetadataFieldConfig, MetadataProcessorService> metadataProcessorMap)
    {
        this.metadataProcessorMap = metadataProcessorMap;
    }

    /**
     * Return the metadataProcessor used to update values to make them more compliant for certain goals
     * @param metadataField to retrieve processor for
     * @return metadataProcessor
     */
    public MetadataProcessorService getMetadataProcessor(MetadataFieldConfig metadataField)
    {
        if(metadataProcessorMap != null)
        {
            return metadataProcessorMap.get(metadataField);
        }else{
            return null;
        }
    }

    /**
     * @param field MetadataFieldConfig representing what to map the value to
     * @param value The value to map to a MetadatumDTO
     * @return A metadatumDTO created from the field and value
     */
    public MetadatumDTO toDCValue(MetadataFieldConfig field, String value) {
        MetadatumDTO dcValue = new MetadatumDTO();

        if (field == null) return null;
        MetadataProcessorService metadataProcessor = getMetadataProcessor(field);
        if(metadataProcessor != null)
        {
            value = metadataProcessor.processMetadataValue(value);
        }
        dcValue.setValue(value);
        dcValue.setElement(field.getElement());
        dcValue.setQualifier(field.getQualifier());
        dcValue.setSchema(field.getSchema());
        return dcValue;
    }

    /**
     * Retrieve the metadataFieldMap set to this class
     * @return Map<MetadataFieldConfig, MetadataContributor<RecordType>> representing the metadataFieldMap
     */
    public Map<MetadataFieldConfig, MetadataContributor<RecordType>> getMetadataFieldMap() {
        return metadataFieldMap;
    }

    /** Defines which metadatum is mapped on which metadatum. Note that while the key must be unique it
     * only matters here for postprocessing of the value. The mapped MetadatumContributor has full control over
     * what metadatafield is generated.
     * @param metadataFieldMap The map containing the link between retrieve metadata and metadata that will be set to the item.
     */
    public void setMetadataFieldMap(Map<MetadataFieldConfig, MetadataContributor<RecordType>> metadataFieldMap) {
        this.metadataFieldMap = metadataFieldMap;
        for(MetadataContributor<RecordType> mc:metadataFieldMap.values()){
            mc.setMetadataFieldMapping(this);
        }
    }

    /**
     * Loop over the MetadataContributors and return their concatenated retrieved metadatumDTO objects
     * @param record Used to retrieve the MetadatumDTO
     * @return Lit of metadatumDTO
     */
    @Override
    public Collection<MetadatumDTO> resultToDCValueMapping(RecordType record) {
        List<MetadatumDTO> values=new LinkedList<MetadatumDTO>();


        for(MetadataContributor<RecordType> query:getMetadataFieldMap().values()){
            try {
                values.addAll(query.contributeMetadata(record));
            } catch (Exception e) {
                log.error("Error", e);
            }

        }
        return values;

    }
}
