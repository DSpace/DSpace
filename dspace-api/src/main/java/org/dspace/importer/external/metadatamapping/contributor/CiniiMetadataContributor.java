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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;

public class CiniiMetadataContributor extends SimpleXpathMetadatumContributor {

    private Map<String, MetadataFieldConfig> languageRemappingMap;

    public void setLanguageRemappingMap(Map<String, MetadataFieldConfig> languageRemappingMap) {
        this.languageRemappingMap = languageRemappingMap;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        try {
            AXIOMXPath xpathLang = new AXIOMXPath("//dc:language");
            for (String ns : prefixToNamespaceMapping.keySet()) {
                xpathLang.addNamespace(prefixToNamespaceMapping.get(ns), ns);
            }
            String lang = xpathLang.stringValueOf(t);
            AXIOMXPath xpath = new AXIOMXPath(query);
            for (String ns : prefixToNamespaceMapping.keySet()) {
                xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
            }
            List<Object> nodes = xpath.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    values.add(metadataFieldMapping.toDCValue(remapped(field, lang), ((OMElement) el).getText()));
                } else if (el instanceof OMAttribute) {
                    values.add(metadataFieldMapping.toDCValue(remapped(field, lang),
                        ((OMAttribute) el).getAttributeValue()));
                } else if (el instanceof String) {
                    values.add(metadataFieldMapping.toDCValue(remapped(field, lang), (String) el));
                } else if (el instanceof OMText) {
                    values.add(metadataFieldMapping.toDCValue(remapped(field, lang), ((OMText) el).getText()));
                } else {
                    System.err.println("node of type: " + el.getClass());
                }
            }
            return values;
        } catch (JaxenException e) {
            System.err.println(query);
            throw new RuntimeException(e);
        }
    }

    public MetadataFieldConfig remapped(MetadataFieldConfig field, String language) {
        if ("JPN".equalsIgnoreCase(language) || "JA".equalsIgnoreCase(language)) {
            return field;
        } else {
            String stringifyField = field.getSchema() + "." + field.getElement();
            if (field.getQualifier() != null) {
                stringifyField = stringifyField + "." + field.getQualifier();
            }
            if (languageRemappingMap.get(stringifyField) != null) {
                return languageRemappingMap.get(stringifyField);
            } else {
                return field;
            }
        }
    }


}
