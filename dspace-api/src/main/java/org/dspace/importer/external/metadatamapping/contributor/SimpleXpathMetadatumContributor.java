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
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 11/01/13
 * Time: 09:21
 */
public class SimpleXpathMetadatumContributor implements MetadataContributor<OMElement> {
    private MetadataFieldConfig field;

    public Map<String, String> getPrefixToNamespaceMapping() {
        return prefixToNamespaceMapping;
    }

    private MetadataFieldMapping<OMElement,MetadataContributor<OMElement>> metadataFieldMapping;

    public MetadataFieldMapping<OMElement,MetadataContributor<OMElement>> getMetadataFieldMapping() {
        return metadataFieldMapping;
    }

    public void setMetadataFieldMapping(MetadataFieldMapping<OMElement,MetadataContributor<OMElement>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
    }

    @Resource(name="isiFullprefixMapping")
    public void setPrefixToNamespaceMapping(Map<String, String> prefixToNamespaceMapping) {
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
    }

    private Map<String,String> prefixToNamespaceMapping;

    public SimpleXpathMetadatumContributor(String query, Map<String, String> prefixToNamespaceMapping, MetadataFieldConfig field) {
        this.query = query;
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
        this.field = field;
    }

    public SimpleXpathMetadatumContributor() {

    }

    private String query;

    public MetadataFieldConfig getField() {
        return field;
    }
    @Required
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    public String getQuery() {
        return query;
    }
    @Required
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values=new LinkedList<MetadatumDTO>();
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
