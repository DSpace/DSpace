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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * This contributor can perform research on multi-paths
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class SimpleMultiplePathContributor extends SimpleXpathMetadatumContributor {

    private final static Logger log = LogManager.getLogger();

    private List<String> paths;

    public SimpleMultiplePathContributor() {}

    public SimpleMultiplePathContributor(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (String path : this.paths) {
            List<Namespace> namespaces = new ArrayList<Namespace>();
            for (String ns : prefixToNamespaceMapping.keySet()) {
                namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
            }
            XPathExpression<Object> xpath = XPathFactory.instance().compile(path, Filters.fpassthrough(), null,
                    namespaces);
            List<Object> nodes = xpath.evaluate(t);
            for (Object el : nodes) {
                if (el instanceof Element) {
                    values.add(metadataFieldMapping.toDCValue(field, ((Element) el).getText()));
                } else {
                    log.warn("node of type: " + el.getClass());
                }
            }
        }
        return values;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

}