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

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Constants;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class WosIdentifierRidContributor extends SimpleXpathMetadatumContributor {

    private static final Logger log = LoggerFactory.getLogger(WosIdentifierRidContributor.class);

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        try {
            for (String ns : prefixToNamespaceMapping.keySet()) {
                List<Element> nodes = element.getChildren(query, Namespace.getNamespace(ns));
                for (Element el : nodes) {
                    // Element element2 = el.getFirstChildWithName("name");
                    if (Objects.nonNull(element)) {
                        String type = element.getAttributeValue("role");
                        setIdentyfire(type, element, values);
                    }
                }
            }
            return values;
        } catch (JaxenException e) {
            log.error(query, e);
            throw new RuntimeException(e);
        }
    }

    private void setIdentyfire(String type, Element el, List<MetadatumDTO> values) throws JaxenException {
        if (StringUtils.equals("researcher_id", type)) {
            String value = el.getAttributeValue("r_id");
            if (StringUtils.isNotBlank(value)) {
                values.add(metadataFieldMapping.toDCValue(this.field, value));
            } else {
                values.add(metadataFieldMapping.toDCValue(this.field,
                           Constants.PLACEHOLDER_PARENT_METADATA_VALUE));
            }
        }
    }

}