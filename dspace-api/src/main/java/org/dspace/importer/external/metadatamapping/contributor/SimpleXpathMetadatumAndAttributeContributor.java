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
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.dspace.core.CrisConstants;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This contributor checks for each node returned for the supplied path
 * if node contains supplied attribute - the value of the current node is taken,
 * otherwise #PLACEHOLDER_PARENT_METADATA_VALUE#
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class SimpleXpathMetadatumAndAttributeContributor extends SimpleXpathMetadatumContributor {

    private static final Logger log = LoggerFactory.getLogger(SimpleXpathMetadatumAndAttributeContributor.class);

    private String attribute;

    @Override
    @SuppressWarnings("unchecked")
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        try {
            AXIOMXPath xpath = new AXIOMXPath(query);
            for (String ns : prefixToNamespaceMapping.keySet()) {
                xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
            }
            List<Object> nodes = xpath.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    OMElement element = (OMElement) el;
                    String attributeValue = element.getAttributeValue(new QName(this.attribute));
                    if (StringUtils.isNotBlank(attributeValue)) {
                        values.add(metadataFieldMapping.toDCValue(this.field, attributeValue));
                    } else {
                        values.add(metadataFieldMapping.toDCValue(this.field,
                                CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE));
                    }
                } else {
                    log.warn("node of type: " + el.getClass());
                }
            }
            return values;
        } catch (JaxenException e) {
            log.error(query, e);
            throw new RuntimeException(e);
        }
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

}