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
import java.util.Iterator;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * A simple JsonPath Metadata processor
 * that allow extract value from json object
 * by configuring the path in the query variable via the bean.
 * moreover this can also perform more compact extractions
 * by configuring specific json processor in "metadataProcessor"
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class SimpleJsonPathMetadataContributor implements MetadataContributor<String> {

    private final static Logger log = LogManager.getLogger();

    private String query;

    private MetadataFieldConfig field;

    protected JsonPathMetadataProcessor metadataProcessor;

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
        if (Objects.nonNull(metadataProcessor)) {
            metadataValue = metadataProcessor.processMetadata(fullJson);
        } else {
            JsonNode jsonNode = convertStringJsonToJsonNode(fullJson);
            JsonNode node = jsonNode.at(query);
            if (node.isArray()) {
                Iterator<JsonNode> nodes = node.iterator();
                while (nodes.hasNext()) {
                    String nodeValue = getStringValue(nodes.next());
                    if (StringUtils.isNotBlank(nodeValue)) {
                        metadataValue.add(nodeValue);
                    }
                }
            } else if (!node.isNull() && StringUtils.isNotBlank(node.toString())) {
                String nodeValue = getStringValue(node);
                if (StringUtils.isNotBlank(nodeValue)) {
                    metadataValue.add(nodeValue);
                }
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

    private String getStringValue(JsonNode node) {
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isNumber()) {
            return node.numberValue().toString();
        }
        log.error("It wasn't possible to convert the value of the following JsonNode:" + node.asText());
        return StringUtils.EMPTY;
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return body;
    }

}