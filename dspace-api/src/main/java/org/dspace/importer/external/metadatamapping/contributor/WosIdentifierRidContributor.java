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
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class WosIdentifierRidContributor extends SimpleXpathMetadatumContributor {

    private static final Logger log = LoggerFactory.getLogger(WosIdentifierRidContributor.class);

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
                    OMElement element = ((OMElement) el).getFirstChildWithName(new QName("name"));
                    if (Objects.nonNull(element)) {
                        String type = element.getAttributeValue(new QName("role"));
                        setIdentyfire(type, element, values);
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

    private void setIdentyfire(String type, OMElement el, List<MetadatumDTO> values) throws JaxenException {
        if (StringUtils.equals("researcher_id", type)) {
            String value = el.getAttributeValue(new QName("r_id"));
            if (StringUtils.isNotBlank(value)) {
                values.add(metadataFieldMapping.toDCValue(this.field, value));
            } else {
                values.add(metadataFieldMapping.toDCValue(this.field,
                           CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE));
            }
        }
    }

}