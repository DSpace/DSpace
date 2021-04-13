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
import javax.annotation.Resource;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This contributor checks for each node returned for the given path if the node contains "this.attribute"
 * and then checks if the attribute value is one of the values configured
 * in the "this.attributeValue2metadata" map, if the value of the current known is taken.
 * If "this.firstChild" is true, it takes the value of the child of the known.
 *
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class WosAttribute2ValueContributor implements MetadataContributor<OMElement> {

    private static final Logger log = LoggerFactory.getLogger(WosAttribute2ValueContributor.class);

    private String query;

    private String attribute;

    private boolean firstChild;

    private String childName;

    private Map<String, String> prefixToNamespaceMapping;

    private Map<String, MetadataFieldConfig> attributeValue2metadata;

    private MetadataFieldMapping<OMElement, MetadataContributor<OMElement>> metadataFieldMapping;

    public WosAttribute2ValueContributor() {}

    public WosAttribute2ValueContributor(String query,
                          Map<String, String> prefixToNamespaceMapping,
                          Map<String, MetadataFieldConfig> attributeValue2metadata) {
        this.query = query;
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
        this.attributeValue2metadata = attributeValue2metadata;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        try {
            AXIOMXPath xpath = new AXIOMXPath(query);
            for (String ns : prefixToNamespaceMapping.keySet()) {
                xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
            }
            List<Object> nodes = xpath.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    OMElement element = (OMElement) el;
                    String attributeValue = element.getAttributeValue(new QName(this.attribute));
                    setField(attributeValue, element, values);
                } else {
                    log.warn("node of type: " + el.getClass());
                }
            }
            return values;
        } catch (JaxenException e) {
            log.warn(query, e);
            throw new RuntimeException(e);
        }
    }

    private void setField(String attributeValue, OMElement el, List<MetadatumDTO> values) throws JaxenException {
        for (String id : attributeValue2metadata.keySet()) {
            if (StringUtils.equals(id, attributeValue)) {
                if (this.firstChild) {
                    String value = el.getFirstChildWithName(new QName(this.childName)).getText();
                    values.add(metadataFieldMapping.toDCValue(attributeValue2metadata.get(id), value));
                } else {
                    values.add(metadataFieldMapping.toDCValue(attributeValue2metadata.get(id), el.getText()));
                }
            }
        }
    }

    public MetadataFieldMapping<OMElement, MetadataContributor<OMElement>> getMetadataFieldMapping() {
        return metadataFieldMapping;
    }

    public void setMetadataFieldMapping(
        MetadataFieldMapping<OMElement, MetadataContributor<OMElement>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
    }

    @Resource(name = "isiFullprefixMapping")
    public void setPrefixToNamespaceMapping(Map<String, String> prefixToNamespaceMapping) {
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
    }

    public Map<String, String> getPrefixToNamespaceMapping() {
        return prefixToNamespaceMapping;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Map<String, MetadataFieldConfig> getAttributeValue2metadata() {
        return attributeValue2metadata;
    }

    public void setAttributeValue2metadata(Map<String, MetadataFieldConfig> attributeValue2metadata) {
        this.attributeValue2metadata = attributeValue2metadata;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isFirstChild() {
        return firstChild;
    }

    public void setFirstChild(boolean firstChild) {
        this.firstChild = firstChild;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

}