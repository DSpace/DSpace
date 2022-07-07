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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * Web Of Science specific implementation of {@link MetadataContributor}
 * This contributor checks for each node returned for the given path if the node contains "this.attribute"
 * and then checks if the attribute value is one of the values configured
 * in the "this.attributeValue2metadata" map, if the value of the current known is taken.
 * If "this.firstChild" is true, it takes the value of the child of the known.
 * The mapping and configuration of this class can be found in the following wos-integration.xml file.
 *
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class WosAttribute2ValueContributor implements MetadataContributor<Element> {

    private final static Logger log = LogManager.getLogger();

    private String query;

    private String attribute;

    private boolean firstChild;

    private String childName;

    private Map<String, String> prefixToNamespaceMapping;

    private Map<String, MetadataFieldConfig> attributeValue2metadata;

    private MetadataFieldMapping<Element, MetadataContributor<Element>> metadataFieldMapping;

    public WosAttribute2ValueContributor() {}

    public WosAttribute2ValueContributor(String query,
                          Map<String, String> prefixToNamespaceMapping,
                          Map<String, MetadataFieldConfig> attributeValue2metadata) {
        this.query = query;
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
        this.attributeValue2metadata = attributeValue2metadata;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        List<MetadatumDTO> values = new LinkedList<>();
        List<Namespace> namespaces = new ArrayList<Namespace>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
        }
        XPathExpression<Object> xpath = XPathFactory.instance().compile(query, Filters.fpassthrough(), null,
                namespaces);
        List<Object> nodes = xpath.evaluate(t);
        for (Object el : nodes) {
            if (el instanceof Element) {
                Element element = (Element) el;
                String attributeValue = element.getAttributeValue(this.attribute);
                setField(attributeValue, element, values);
            } else {
                log.warn("node of type: " + el.getClass());
            }
        }
        return values;
    }

    private void setField(String attributeValue, Element el, List<MetadatumDTO> values) {
        for (String id : attributeValue2metadata.keySet()) {
            if (StringUtils.equals(id, attributeValue)) {
                if (this.firstChild) {
                    String value = el.getChild(this.childName).getValue();
                    values.add(metadataFieldMapping.toDCValue(attributeValue2metadata.get(id), value));
                } else {
                    values.add(metadataFieldMapping.toDCValue(attributeValue2metadata.get(id), el.getText()));
                }
            }
        }
    }

    public MetadataFieldMapping<Element, MetadataContributor<Element>> getMetadataFieldMapping() {
        return metadataFieldMapping;
    }

    public void setMetadataFieldMapping(
        MetadataFieldMapping<Element, MetadataContributor<Element>> metadataFieldMapping) {
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