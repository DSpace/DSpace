/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.tei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.importer.external.metadatamapping.contributor.SimpleXpathMetadatumContributor;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * TEI-specific implementation of {@link MetadataContributor}.
 * Extracts author names from TEI XML in "surname, forename(s)" format,
 * emitting one {@link MetadatumDTO} per author found under tei:analytic/tei:author.
 * If a "middle" forename is present, it will appear after the "first" forename.
 *
 * @author Kim Shepherd
 */
public class TEIAuthorMetadataContributor extends SimpleXpathMetadatumContributor {

    private static final Namespace TEI_NS =
            Namespace.getNamespace("tei", "http://www.tei-c.org/ns/1.0");

    /**
     * Retrieve the metadata associated with the given object.
     * Iterates over every {@code <author>} child of the supplied element
     * and builds a "surname, forename(s)" author name string for each one.
     *
     * @param element The root element to extract author metadata from.
     *                Should correspond to the {@code <analytic>} element
     *                based on the configured XPath query.
     * @return A collection of {@link MetadatumDTO}, one per author
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();

        List<Namespace> namespaces = new ArrayList<>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
        }

        XPathExpression<Object> xpath = XPathFactory.instance()
                .compile(query, Filters.fpassthrough(), null, namespaces);

        List<Object> nodes = xpath.evaluate(element);
        for (Object node : nodes) {
            if (node instanceof Element) {
                MetadatumDTO dto = extractAuthor((Element) node);
                if (dto != null) {
                    values.add(dto);
                }
            }
        }

        return values;
    }
    /**
     * Extract a single author's name from an {@code <author>} element.
     *
     * @param author A {@code <author>} element in the TEI namespace.
     * @return A populated {@link MetadatumDTO}, or {@code null} if the
     *         element contains no usable name.
     */
    private MetadatumDTO extractAuthor(Element author) {
        Element persName = author.getChild("persName", TEI_NS);
        if (null == persName) {
            return null;
        }

        String surname  = childText(persName, "surname");
        String forenames = collectForenames(persName);

        String fullName;
        if (StringUtils.isNotBlank(surname) && StringUtils.isNotBlank(forenames)) {
            fullName = surname + ", " + forenames;
        } else if (StringUtils.isNotBlank(surname)) {
            fullName = surname;
        } else if (StringUtils.isNotBlank(forenames)) {
            fullName = forenames;
        } else {
            return null;
        }

        MetadatumDTO dto = new MetadatumDTO();
        dto.setSchema(getField().getSchema());
        dto.setElement(getField().getElement());
        dto.setQualifier(getField().getQualifier());
        dto.setValue(fullName);
        return dto;
    }

    /**
     * Collect all {@code <forename>} children of the given {@code <persName>}
     * element in document order, trim each value, discard blanks, and join
     * with a single space.
     *
     * @param persName The {@code <persName>} element to read from.
     * @return A space-joined string of all forename values, or empty string
     */
    private String collectForenames(Element persName) {
        return persName.getChildren("forename", TEI_NS).stream()
                .map(forename -> StringUtils.trimToEmpty(forename.getValue()))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));
    }

    /**
     * Safely retrieve the trimmed text value of a named child element
     * in the TEI namespace, returning an empty string if the child is absent.
     * Currently, this is just used for surname but could be used for affiliation etc
     * in the future
     *
     * @param parent    The parent element to search within.
     * @param childName The local name of the child element.
     * @return The trimmed text content, or empty string if not found.
     */
    private String childText(Element parent, String childName) {
        Element child = parent.getChild(childName, TEI_NS);
        if (null == child) {
            return StringUtils.EMPTY;
        }
        return StringUtils.trimToEmpty(child.getValue());
    }
}