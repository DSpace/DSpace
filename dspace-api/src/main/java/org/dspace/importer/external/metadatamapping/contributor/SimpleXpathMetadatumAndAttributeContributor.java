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

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Constants;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * This contributor checks for each node returned for the supplied path
 * if node contains supplied attribute - the value of the current node is taken,
 * otherwise #PLACEHOLDER_PARENT_METADATA_VALUE#
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class SimpleXpathMetadatumAndAttributeContributor extends SimpleXpathMetadatumContributor {

    private String attribute;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            List<Element> nodes = element.getChildren(query, Namespace.getNamespace(ns));
            for (Element el : nodes) {
                String attributeValue = el.getAttributeValue(this.attribute);
                if (StringUtils.isNotBlank(attributeValue)) {
                    values.add(metadataFieldMapping.toDCValue(this.field, attributeValue));
                } else {
                    values.add(metadataFieldMapping.toDCValue(this.field, Constants.PLACEHOLDER_PARENT_METADATA_VALUE));
                }
            }
        }
        return values;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

}