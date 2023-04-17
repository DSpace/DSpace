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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * This contributor checks for each node returned for the supplied path
 * if node contains supplied attribute - the value of the current node is taken if exist.
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot com)
 */
public class SimpleXpathMetadatumAndAttributeContributor extends SimpleXpathMetadatumContributor {

    private final static Logger log = LogManager.getLogger();

    private String attribute;

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
                if (StringUtils.isNotBlank(attributeValue)) {
                    values.add(metadataFieldMapping.toDCValue(this.field, attributeValue));
                }
            } else {
                log.warn("node of type: " + el.getClass());
            }
        }
        return values;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

}