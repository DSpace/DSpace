/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.tei;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.SimpleXpathMetadatumContributor;
import org.dspace.util.MultiFormatDateParser;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * TEI-specific date metadata contributor.
 * Extracts a date value via XPath and parses via {@link MultiFormatDateParser}
 * ({@see discovery-solr.xml})
 * with a default target format of {@code yyyy-MM-dd}).
 *
 */
public class TEIDateMetadataContributor extends SimpleXpathMetadatumContributor {

    /**
     * Output format for the stored metadata value. Defaults to {@code yyyy-MM-dd}
     * but can be overridden in Spring XML via {@code <property name="dateFormatTo"/>}.
     */
    private String dateFormatTo = "yyyy-MM-dd";

    private static final Logger log = LogManager.getLogger(TEIDateMetadataContributor.class);

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
            String raw = extractRawValue(node);
            if (StringUtils.isNotBlank(raw)) {
                values.add(metadataFieldMapping.toDCValue(field, formatDate(raw.trim())));
            }
        }

        return values;
    }

    /**
     * Extract the raw string value from an XPath result node.
     */
    private String extractRawValue(Object node) {
        if (node instanceof Element) {
            return ((Element) node).getText();
        } else if (node instanceof Attribute) {
            return ((Attribute) node).getValue();
        } else if (node instanceof String) {
            return (String) node;
        } else if (node instanceof Text) {
            return ((Text) node).getText();
        }
        return null;
    }

    /**
     * Parse {@code raw} using {@link MultiFormatDateParser}, which tries each
     * configured regex/format pair in order. If parsing succeeds, reformat
     * using {@link #dateFormatTo}. If not, return raw value.
     *
     * Be careful with yyyy-MM-dd if only input dates are expected, or you could end
     * up with 2022-01-01 instead of 2022, etc.
     *
     * @param raw the raw date string extracted from the TEI document
     * @return the reformatted date string, or {@code raw} if unparseable
     */
    private String formatDate(String raw) {
        ZonedDateTime parsed = MultiFormatDateParser.parse(raw);
        if (parsed != null) {
            return parsed.format(DateTimeFormatter.ofPattern(dateFormatTo));
        }
        log.warn("TEIDateMetadataContributor: could not parse date '{}', storing as-is", raw);
        return raw;
    }

    public String getDateFormatTo() {
        return dateFormatTo;
    }

    public void setDateFormatTo(String dateFormatTo) {
        this.dateFormatTo = dateFormatTo;
    }
}