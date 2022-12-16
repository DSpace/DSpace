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
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Scopus specific implementation of {@link MetadataContributor}
 * Responsible for generating the Scopus startPage and endPage from the retrieved item.
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4science.com)
 */
public class PageRangeXPathMetadataContributor extends SimpleXpathMetadatumContributor {

    private MetadataFieldConfig startPageMetadata;

    private MetadataFieldConfig endPageMetadata;

    /**
     * Retrieve the metadata associated with the given Element object.
     * Depending on the retrieved node (using the query),
     * StartPage and EndPage values will be added to the MetadatumDTO list
     *
     * @param el    A class to retrieve metadata from.
     * @return      A collection of import records. Only the StartPage and EndPage
     *                of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element el) {
        List<MetadatumDTO> values = new LinkedList<>();
        List<MetadatumDTO> metadatums = null;
        for (String ns : prefixToNamespaceMapping.keySet()) {
            List<Element> nodes = el.getChildren(query, Namespace.getNamespace(ns));
            for (Element element : nodes) {
                metadatums = getMetadatum(element.getValue());
                if (Objects.nonNull(metadatums)) {
                    for (MetadatumDTO metadatum : metadatums) {
                        values.add(metadatum);
                    }
                }
            }
        }
        return values;
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