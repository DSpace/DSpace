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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;

/**
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4science dot it)
 */
public class PageRangeXPathMetadataContributor extends SimpleXpathMetadatumContributor {

    private MetadataFieldConfig startPageMetadata;

    private MetadataFieldConfig endPageMetadata;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        List<MetadatumDTO> metadatums = null;
        try {
            AXIOMXPath xpath = new AXIOMXPath(query);
            for (String ns : prefixToNamespaceMapping.keySet()) {
                xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
            }
            List<Object> nodes = xpath.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    metadatums = getMetadatum(((OMElement) el).getText());
                } else if (el instanceof OMAttribute) {
                    metadatums = getMetadatum(((OMAttribute) el).getAttributeValue());
                } else if (el instanceof String) {
                    metadatums =  getMetadatum((String) el);
                } else if (el instanceof OMText) {
                    metadatums = getMetadatum(((OMText) el).getText());
                } else {
                    System.err.println("node of type: " + el.getClass());
                }

                if (Objects.nonNull(metadatums)) {
                    for (MetadatumDTO metadatum : metadatums) {
                        values.add(metadatum);
                    }
                }
            }
            return values;
        } catch (JaxenException e) {
            System.err.println(query);
            throw new RuntimeException(e);
        }
    }

    private  List<MetadatumDTO> getMetadatum(String value) {
        List<MetadatumDTO> metadatums = new ArrayList<MetadatumDTO>();
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String [] range = value.split("-");
        if (range.length == 2) {
            metadatums.add(setStartPage(range));
            metadatums.add(setEndPage(range));
        } else if (range.length != 0) {
            metadatums.add(setStartPage(range));
        }
        return metadatums;
    }

    private MetadatumDTO setEndPage(String[] range) {
        MetadatumDTO endPage = new MetadatumDTO();
        endPage.setValue(range[1]);
        endPage.setElement(endPageMetadata.getElement());
        endPage.setQualifier(endPageMetadata.getQualifier());
        endPage.setSchema(endPageMetadata.getSchema());
        return endPage;
    }

    private MetadatumDTO setStartPage(String[] range) {
        MetadatumDTO startPage = new MetadatumDTO();
        startPage.setValue(range[0]);
        startPage.setElement(startPageMetadata.getElement());
        startPage.setQualifier(startPageMetadata.getQualifier());
        startPage.setSchema(startPageMetadata.getSchema());
        return startPage;
    }

    public MetadataFieldConfig getStartPageMetadata() {
        return startPageMetadata;
    }

    public void setStartPageMetadata(MetadataFieldConfig startPageMetadata) {
        this.startPageMetadata = startPageMetadata;
    }

    public MetadataFieldConfig getEndPageMetadata() {
        return endPageMetadata;
    }

    public void setEndPageMetadata(MetadataFieldConfig endPageMetadata) {
        this.endPageMetadata = endPageMetadata;
    }

}