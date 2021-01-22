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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This contributor is able to concat multi value.
 * Given a certain path, if it contains several nodes,
 * the values of nodes will be concatenated into a single one
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class SimpleConcatContributor extends SimpleXpathMetadatumContributor {
    private static final Logger log = LoggerFactory.getLogger(SimpleConcatContributor.class);

    @Override
    @SuppressWarnings("unchecked")
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        StringBuilder text = new StringBuilder();
        try {
            AXIOMXPath xpath = new AXIOMXPath(query);
            for (String ns : prefixToNamespaceMapping.keySet()) {
                xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
            }
            List<Object> nodes = xpath.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    OMElement element = (OMElement) el;
                    if (StringUtils.isNotBlank(element.getText())) {
                        text.append(element.getText());
                    }
                } else {
                    log.warn("node of type: " + el.getClass());
                }
            }
            if (StringUtils.isNotBlank(text.toString())) {
                values.add(metadataFieldMapping.toDCValue(field, text.toString()));
            }
            return values;
        } catch (JaxenException e) {
            log.error(query, e);
            throw new RuntimeException(e);
        }
    }

}