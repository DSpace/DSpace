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

import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * This contributor can perform research on multi-paths
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class SimpleMultiplePathContributor extends SimpleXpathMetadatumContributor {

    private List<String> paths;

    public SimpleMultiplePathContributor() {}

    public SimpleMultiplePathContributor(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        for (String path : this.paths) {
            for (String ns : prefixToNamespaceMapping.keySet()) {
                List<Element> nodes = element.getChildren(path, Namespace.getNamespace(ns));
                for (Element el : nodes) {
                    values.add(metadataFieldMapping.toDCValue(field, el.getValue()));
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