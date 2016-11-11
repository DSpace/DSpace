/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Metadata contributor that takes an axiom OMElement and turns it into a metadatum
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class SimpleXpathMetadatumContributor implements MetadataContributor<OMElement> {
    private MetadataFieldConfig field;

    /**
     * Return prefixToNamespaceMapping
     * @return a prefixToNamespaceMapping map
     */
    public Map<String, String> getPrefixToNamespaceMapping() {
        return prefixToNamespaceMapping;
    }

    private MetadataFieldMapping<OMElement,MetadataContributor<OMElement>> metadataFieldMapping;

    /**
     * Return metadataFieldMapping
     * @return MetadataFieldMapping
     */
    public MetadataFieldMapping<OMElement,MetadataContributor<OMElement>> getMetadataFieldMapping() {
        return metadataFieldMapping;
    }

    /**
     * Set the metadataFieldMapping of this SimpleXpathMetadatumContributor
     * @param metadataFieldMapping the new mapping.
     */
    public void setMetadataFieldMapping(MetadataFieldMapping<OMElement,MetadataContributor<OMElement>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
    }

    /**
     * Set the prefixToNamespaceMapping for this object,
     * @param prefixToNamespaceMapping the new mapping.
     */
    @Resource(name="isiFullprefixMapping")
    public void setPrefixToNamespaceMapping(Map<String, String> prefixToNamespaceMapping) {
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
    }

    private Map<String,String> prefixToNamespaceMapping;

    /**
     * Initialize SimpleXpathMetadatumContributor with a query, prefixToNamespaceMapping and MetadataFieldConfig
     * @param query
     *     query string
     * @param prefixToNamespaceMapping
     *     metadata prefix to namespace mapping
     * @param field
     *     <a href="https://github.com/DSpace/DSpace/tree/master/dspace-api/src/main/java/org/dspace/importer/external#metadata-mapping-">MetadataFieldConfig</a>
     */
    public SimpleXpathMetadatumContributor(String query, Map<String, String> prefixToNamespaceMapping, MetadataFieldConfig field) {
        this.query = query;
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
        this.field = field;
    }

    /**
     * Empty constructor for SimpleXpathMetadatumContributor
     */
    public SimpleXpathMetadatumContributor() {

    }

    private String query;

    /**
     * Return the MetadataFieldConfig used while retrieving MetadatumDTO
     * @return MetadataFieldConfig
     */
    public MetadataFieldConfig getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     * @param field MetadataFieldConfig used while retrieving MetadatumDTO
     */
    @Required
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /**
     * Return query used to create an xpathExpression on, this query is used to
     * @return the query this instance is based on
     */
    public String getQuery() {
        return query;
    }

    @Required
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Retrieve the metadata associated with the given object.
     * Depending on the retrieved node (using the query), different types of values will be added to the MetadatumDTO list
     * @param t A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values=new LinkedList<>();
        try {
            AXIOMXPath xpath=new AXIOMXPath(query);
            for(String ns:prefixToNamespaceMapping.keySet()){
                xpath.addNamespace(prefixToNamespaceMapping.get(ns),ns);
            }
            List<Object> nodes=xpath.selectNodes(t);
            for(Object el:nodes)
                if(el instanceof OMElement)
                    values.add(metadataFieldMapping.toDCValue(field, ((OMElement) el).getText()));
                else if(el instanceof OMAttribute){
                    values.add(metadataFieldMapping.toDCValue(field, ((OMAttribute) el).getAttributeValue()));
                } else if(el instanceof String){
                    values.add(metadataFieldMapping.toDCValue(field, (String) el));
                } else if(el instanceof OMText)
                    values.add(metadataFieldMapping.toDCValue(field, ((OMText) el).getText()));
                else
                {
                    System.err.println("node of type: "+el.getClass());
                }
            return values;
        } catch (JaxenException e) {
            System.err.println(query);
            throw new RuntimeException(e);
        }

    }
}
