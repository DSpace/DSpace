/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.pubmed.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.SimpleXpathMetadatumContributor;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * This class is responsible for extracting the abstract from a PubMed XML document.
 * It uses XPath to find the relevant elements and constructs a formatted string for the abstract, respecting
 * PubMed's labelled abstract format, and including the labels in the output.
 */
public class PubmedAbstractMetadatumContributor extends SimpleXpathMetadatumContributor {

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        List<MetadatumDTO> values = new LinkedList<>();

        List<Namespace> namespaces = new ArrayList<>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
        }

        XPathExpression<Element> xpath = XPathFactory.instance().compile(query, Filters.element(), null, namespaces);
        List<Element> nodes = xpath.evaluate(t);
        StringBuilder sb = new StringBuilder();

        for (Element el : nodes) {
            String label = el.getAttributeValue("Label");
            String text = el.getTextNormalize();

            if (text == null || text.isEmpty()) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append("\n\n");
            }

            if (label != null && !label.equalsIgnoreCase("UNLABELLED")) {
                sb.append(label).append(": ");
            }
            sb.append(text);
        }

        String fullAbstract = sb.toString().trim();
        if (!fullAbstract.isEmpty()) {
            values.add(metadataFieldMapping.toDCValue(field, fullAbstract));
        }
        return values;
    }
}
