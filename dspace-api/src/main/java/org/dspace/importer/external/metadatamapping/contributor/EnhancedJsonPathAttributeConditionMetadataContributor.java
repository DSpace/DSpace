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
 * In addition from some array where `query` points to the values can be selected using some additional condition check.
 * This can be expressed in jsonpath, but not in jsonpointer expression.
 *
 * e.g.
 * query /descriptions
 * conditionkey "descriptionType"
 * conditionvalue "Abstract"
 * retrievedvalue "description"
 *
 * and json
 *
 * "descriptions": [
 *   {"description":"Description A","descriptionType":"Abstract"},
 *   {"description":"Description B","descriptionType":"Other"}
 * ]
 *
 * should deliver the value "Description A".
 * In addition there is some metadataPostProcessor modifying and changing the retrieved value,
 * e.g. mapping to controlled values
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 */
public class EnhancedJsonPathAttributeConditionMetadataContributor implements MetadataContributor<String> {
    private final static Logger log = LogManager.getLogger();
    private String query;
    private String conditionKey;
    private String conditionValue;
    private String elementAttribute;
    private MetadataFieldConfig field;
    protected JsonPathMetadataProcessor metadataProcessor;
    protected JsonPathMetadataProcessor metadataPostProcessor;
    /**
     * Initialize SimpleJsonPathMetadataContributor with a query, prefixToNamespaceMapping and MetadataFieldConfig
     *
     * @param query The JSonPath query
     * @param field the matadata field to map the result of the Json path query
     * <a href="https://github.com/DSpace/DSpace/tree/master/dspace-api/src/main/java/org/dspace/importer/external#metadata-mapping-">MetadataFieldConfig</a>
     */
    public EnhancedJsonPathAttributeConditionMetadataContributor(String query, MetadataFieldConfig field) {
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
    public EnhancedJsonPathAttributeConditionMetadataContributor() {
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
     * Return condition attribute key which is checked
     *
     * @return the attribute key which is checked
     */
    public String getConditionKey() {
        return conditionKey;
    }
    /**
     * The key of the json attribute which is checked for the condition
     *
     * @param conditionKey
     */
    public void setConditionKey(String conditionKey) {
        this.conditionKey = conditionKey;
    }
    /**
     * Return condition attribute value which is checked
     *
     * @return the attribute value which is checked
     */
    public String getConditionValue() {
        return conditionValue;
    }
    /**
     * The value of the json attribute which must match the condition
     *
     * @param conditionValue
     */
    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }
    /**
     * Return element attribute key where the final value is retrieved from
     *
     * @return the attribute key for the element
     */
    public String getElementAttribute() {
        return elementAttribute;
    }
    /**
     * The json attribute where the value is retrieved from
     *
     * @param elementAttribute
     */
    public void setElementAttribute(String elementAttribute) {
        this.elementAttribute = elementAttribute;
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
     * Used to process data got by jsonpath expression, like arrays to stringify, change date format or else
     * If it is null, toString will be used.
     *
     * @param metadataPostProcessor
     */
    public void setMetadataPostProcessor(JsonPathMetadataProcessor metadataPostProcessor) {
        this.metadataPostProcessor = metadataPostProcessor;
    }
    /**
     * Retrieve the metadata associated with the given object.
     * The toString() of the resulting object will be used.
     *
     * @param fullJson A class to retrieve metadata from.
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
                    JsonNode nodeV = nodes.next();
                    if (!checkCondition(nodeV)) {
                        continue;
                    }
                    if (getElementAttribute() != null) {
                        JsonNode element = nodeV.get(getElementAttribute());
                        if (element != null && !element.isMissingNode()) {
                            String nodeValue = getStringValue(element);
                            if (StringUtils.isNotBlank(nodeValue)) {
                                metadataValue.add(nodeValue);
                            }
                        }
                    } else {
                        String nodeValue = getStringValue(nodeV);
                        if (StringUtils.isNotBlank(nodeValue)) {
                            metadataValue.add(nodeValue);
                        }
                    }
                }
            } else if (!node.isNull() && StringUtils.isNotBlank(node.toString())) {
                if (getElementAttribute() != null && checkCondition(node)) {
                    JsonNode element = node.get(getElementAttribute());
                    if (element != null && !element.isMissingNode()) {
                        String nodeValue = getStringValue(element);
                        if (StringUtils.isNotBlank(nodeValue)) {
                            metadataValue.add(nodeValue);
                        }
                    }
                } else if (checkCondition(node)) {
                    String nodeValue = getStringValue(node);
                    if (StringUtils.isNotBlank(nodeValue)) {
                        metadataValue.add(nodeValue);
                    }
                }
            }
        }
        if (metadataPostProcessor != null) {
            Collection<String> postmetadataValues = new ArrayList<>();
            for (String value: metadataValue) {
                Collection<String> postmetadataValue = metadataPostProcessor.processMetadata(value);
                if (postmetadataValue != null) {
                    postmetadataValues.addAll(postmetadataValue);
                }
            }
            metadataValue = postmetadataValues;
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
    /**
     * Check some jsonNode if some jsonpointer expression matches some condition for some match.
     * The match is parsed as some regex condition
     * @param node
     * @return true if node matches the condition or false if not
     */
    private boolean checkCondition(JsonNode node) {
        if (getConditionKey() == null && getConditionValue() == null) {
            return true;
        }
        if (getConditionKey() != null) {
            JsonNode conditionnode = node.get(getConditionKey());
            if (getConditionValue() == null && conditionnode != null && !conditionnode.isMissingNode()) {
                return true;
            } else if (conditionnode != null && !conditionnode.isMissingNode() &&
                conditionnode.toString().matches(getConditionValue())) {
                return true;
            }
        } else if (getConditionKey() == null && node.toString().matches(getConditionValue())) {
            return true;
        }
        return false;
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
