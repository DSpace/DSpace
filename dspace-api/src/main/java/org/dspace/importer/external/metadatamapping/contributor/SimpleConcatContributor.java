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
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * This contributor is able to concat multi value.
 * Given a certain path, if it contains several nodes,
 * the values of nodes will be concatenated into a single one
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class SimpleConcatContributor extends SimpleXpathMetadatumContributor {

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        StringBuilder text = new StringBuilder();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            List<Element> nodes = element.getChildren(query, Namespace.getNamespace(ns));
            for (Element el : nodes) {
                if (StringUtils.isNotBlank(el.getValue())) {
                    text.append(element.getText());
                }
            }
        }
        if (StringUtils.isNotBlank(text.toString())) {
            values.add(metadataFieldMapping.toDCValue(field, text.toString()));
        }
        return values;
    }

}