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
import java.util.Objects;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.util.SimpleMapConverter;
import org.jaxen.JaxenException;

/**
 * This contributor replace metadata value
 * if this matched in mapConverter-openAccesFlag.properties file
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4science dot it)
 */
public class ReplaceFieldXPathMetadataContributor extends SimpleXpathMetadatumContributor {

    private static final String UNSPECIFIED = "Unspecified";
    private SimpleMapConverter simpleMapConverter;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        MetadatumDTO metadatum = null;
        try {
            AXIOMXPath xpath = new AXIOMXPath(query);
            for (String ns : prefixToNamespaceMapping.keySet()) {
                xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
            }
            List<Object> nodes = xpath.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    metadatum = getMetadatum(field, ((OMElement) el).getText());
                } else if (el instanceof OMAttribute) {
                    metadatum = getMetadatum(field, ((OMAttribute) el).getAttributeValue());
                } else if (el instanceof String) {
                    metadatum =  getMetadatum(field, (String) el);
                } else if (el instanceof OMText) {
                    metadatum = getMetadatum(field, ((OMText) el).getText());
                } else {
                    System.err.println("node of type: " + el.getClass());
                }

                if (Objects.nonNull(metadatum)) {
                    values.add(metadatum);
                }
            }
            return values;
        } catch (JaxenException e) {
            System.err.println(query);
            throw new RuntimeException(e);
        }
    }

    private MetadatumDTO getMetadatum(MetadataFieldConfig field, String value) {
        String convertedValue = simpleMapConverter.getValue(value);
        if (UNSPECIFIED.equals(convertedValue)) {
            return null;
        }
        MetadatumDTO dcValue = new MetadatumDTO();
        if (field == null) {
            return null;
        }
        dcValue.setValue(convertedValue);
        dcValue.setElement(field.getElement());
        dcValue.setQualifier(field.getQualifier());
        dcValue.setSchema(field.getSchema());
        return dcValue;
    }

    public SimpleMapConverter getSimpleMapConverter() {
        return simpleMapConverter;
    }

    public void setSimpleMapConverter(SimpleMapConverter simpleMapConverter) {
        this.simpleMapConverter = simpleMapConverter;
    }
}