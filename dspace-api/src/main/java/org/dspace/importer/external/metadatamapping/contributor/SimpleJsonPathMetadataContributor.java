/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;


public class SimpleJsonPathMetadataContributor implements MetadataContributor<String> {

    private MetadataFieldConfig field;

    private String query;

    private JsonPathMetadataProcessor metadataProcessor;

    /**
     * Initialize SimpleJsonPathMetadataContributor with a query, prefixToNamespaceMapping and MetadataFieldConfig
     *
     * @param query The JSonPath query
     * @param field the matadata field to map the result of the Json path query
     * <a href="https://github.com/DSpace/DSpace/tree/master/dspace-api/src/main/java/org/dspace/importer/external#metadata-mapping-">MetadataFieldConfig</a>
     */
    public SimpleJsonPathMetadataContributor(String query, MetadataFieldConfig field) {
        this.query = query;
        this.field = field;
    }


    /**
     * Unused by this implementation
     */
    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<String, MetadataContributor<String>> rt) {

    }

    /**
     * Empty constructor for SimpleJsonPathMetadataContributor
     */
    public SimpleJsonPathMetadataContributor() {

    }

    /**
     * Return the MetadataFieldConfig used while retrieving MetadatumDTO
     *
     * @return MetadataFieldConfig
     */
    public MetadataFieldConfig getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     *
     * @param field MetadataFieldConfig used while retrieving MetadatumDTO
     */
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /**
     * Return query used to create the JSonPath
     *
     * @return the query this instance is based on
     */
    public String getQuery() {
        return query;
    }

    /**
     * Return query used to create the JSonPath
     *
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Used to process data got by jsonpath expression, like arrays to stringify, change date format or else
     * If it is null, toString will be used.
     * 
     * @param metadataProcessor
     */
    public void setMetadataProcessor(JsonPathMetadataProcessor metadataProcessor) {
        this.metadataProcessor = metadataProcessor;
    }

    /**
     * Retrieve the metadata associated with the given object.
     * The toString() of the resulting object will be used.
     * 
     * @param t A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(String fullJson) {
        Collection<MetadatumDTO> metadata = new ArrayList<>();
        Collection<String> metadataValue = new ArrayList<>();
        if (metadataProcessor != null) {
            metadataValue = metadataProcessor.processMetadata(fullJson);
        } else {
            ReadContext ctx = JsonPath.parse(fullJson);
            Object o = ctx.read(query);
            if (o.getClass().isAssignableFrom(JSONArray.class)) {
                JSONArray results = (JSONArray)o;
                for (int i = 0; i < results.size(); i++) {
                    String value = results.get(i).toString();
                    metadataValue.add(value);
                }
            } else {
                metadataValue.add(o.toString());
            }
        }
        for (String value : metadataValue) {
            MetadatumDTO metadatumDto = new MetadatumDTO();
            metadatumDto.setValue(value);
            metadatumDto.setElement(field.getElement());
            metadatumDto.setQualifier(field.getQualifier());
            metadatumDto.setSchema(field.getSchema());
            metadata.add(metadatumDto);
        }
        return metadata;
    }
}
