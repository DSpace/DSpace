/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.tei;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.I18nUtil;
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
     * Inferred granularity of the date string, to ensure we don't format a year into yyyy-01-01
     * and so on
     */
    private enum Granularity { YEAR, MONTH, DAY, TIME }

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
     * Parse {@code raw} using {@link MultiFormatDateParser} and infer
     * granularity to ensure dates are correctly formatted.
     *
     * @param raw the raw date string extracted from the TEI document
     * @return the reformatted date string, or {@code raw} if unparseable
     */
    private String formatDate(String raw) {
        ZonedDateTime date = MultiFormatDateParser.parse(raw);
        if (date != null) {
            return switch (detectGranularity(raw)) {
                case DAY, TIME -> date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                case MONTH -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                default -> date.format(DateTimeFormatter.ofPattern("yyyy"));
            };
        }
        log.warn("TEIDateMetadataContributor: could not parse date '{}', storing as-is", raw);
        return raw;
    }

    private Granularity detectGranularity(String raw) {
        if (raw.contains("T") || raw.contains(":")) {
            // We won't include time in formatted dates anyway
            return Granularity.DAY;
        }
        try {
            LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return Granularity.DAY;
        } catch (DateTimeParseException e) { /* fall through */ }
        try {
            YearMonth.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM"));
            return Granularity.MONTH;
        } catch (DateTimeParseException e) { /* fall through */ }
        try {
            Year.parse(raw, DateTimeFormatter.ofPattern("yyyy"));
            return Granularity.YEAR;
        } catch (DateTimeParseException e) { /* fall through */ }
        try {
            LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd MMM yyyy", I18nUtil.getDefaultLocale()));
            return Granularity.DAY;
        } catch (DateTimeParseException e) { /* fall through */ }
        try {
            LocalDate.parse(raw, DateTimeFormatter.ofPattern("d MMM yyyy", I18nUtil.getDefaultLocale()));
            return Granularity.DAY;
        } catch (DateTimeParseException e) { /* fall through */ }
        try {
            LocalDate.parse(raw, DateTimeFormatter.ofPattern("MMM yyyy", I18nUtil.getDefaultLocale()));
            return Granularity.MONTH;
        } catch (DateTimeParseException e) { /* fall through */ }
        log.info("Could not infer granularity for '{}', assuming YEAR", raw);
        return Granularity.YEAR;
    }

}