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
import java.util.Objects;

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
 * Web Of Science specific implementation of {@link MetadataContributor}
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class WosIdentifierRidContributor extends SimpleXpathMetadatumContributor {

    private final static Logger log = LogManager.getLogger();

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
                Element element = ((Element) el).getChild("name");
                if (Objects.nonNull(element)) {
                    String type = element.getAttributeValue("role");
                    setIdentyfier(type, element, values);
                }
            } else {
                log.warn("node of type: " + el.getClass());
            }
        }
        return values;
    }

    private void setIdentyfier(String type, Element el, List<MetadatumDTO> values) {
        if (StringUtils.equals("researcher_id", type)) {
            String value = el.getAttributeValue("r_id");
            if (StringUtils.isNotBlank(value)) {
                values.add(metadataFieldMapping.toDCValue(this.field, value));
            }
        }
    }

}