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
 * This contributor is able to concat multi value.
 * Given a certain path, if it contains several nodes,
 * the values of nodes will be concatenated into a single one.
 * The concrete example we can see in the file wos-responce.xml in the <abstract_text> node,
 * which may contain several <p> paragraphs,
 * this Contributor allows concatenating all <p> paragraphs. to obtain a single one.
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class SimpleConcatContributor extends SimpleXpathMetadatumContributor {

    private final static Logger log = LogManager.getLogger();

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        List<MetadatumDTO> values = new LinkedList<>();
        StringBuilder text = new StringBuilder();
        List<Namespace> namespaces = new ArrayList<Namespace>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
        }
        XPathExpression<Object> xpath = XPathFactory.instance().compile(query, Filters.fpassthrough(), null,namespaces);
        List<Object> nodes = xpath.evaluate(t);
        for (Object el : nodes) {
            if (el instanceof Element) {
                Element element = (Element) el;
                if (StringUtils.isNotBlank(element.getText())) {
                    text.append(element.getText());
                }
            } else {
                log.warn("node of type: " + el.getClass());
            }
        }
        if (StringUtils.isNotBlank(text.toString())) {
            values.add(metadataFieldMapping.toDCValue(field, text.toString()));
        }
        return values;
    }

}