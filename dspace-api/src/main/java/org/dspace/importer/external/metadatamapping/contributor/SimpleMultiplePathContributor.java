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
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This contributor can perform research on multi-paths
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class SimpleMultiplePathContributor extends SimpleXpathMetadatumContributor {

    private static final Logger log = LoggerFactory.getLogger(SimpleMultiplePathContributor.class);

    private List<String> paths;

    public SimpleMultiplePathContributor() {}

    public SimpleMultiplePathContributor(List<String> paths) {
        this.paths = paths;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        try {
            for (String path : this.paths) {
                AXIOMXPath xpath = new AXIOMXPath(path);
                for (String ns : prefixToNamespaceMapping.keySet()) {
                    xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
                }
                List<Object> nodes = xpath.selectNodes(t);
                for (Object el : nodes) {
                    if (el instanceof OMElement) {
                        values.add(metadataFieldMapping.toDCValue(field, ((OMElement) el).getText()));
                    } else {
                        log.warn("node of type: " + el.getClass());
                    }
                }
            }
            return values;
        } catch (JaxenException e) {
            log.error(query, e);
            throw new RuntimeException(e);
        }
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

}