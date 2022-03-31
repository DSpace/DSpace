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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * This contributor can retrieve the identifiers
 * configured in "this.identifire2field" from the WOS response.
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class WosIdentifierContributor extends SimpleXpathMetadatumContributor {

    protected Map<String, MetadataFieldConfig> identifire2field;

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        List<Namespace> namespaces = new ArrayList<>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
        }
        XPathExpression<Element> xpath =
                XPathFactory.instance().compile(query, Filters.element(), null, namespaces);

        List<Element> nodes = xpath.evaluate(element);
        for (Element el : nodes) {
            String type = el.getAttributeValue("type");
            setIdentyfire(type, el, values);
        }
        return values;
    }

    private void setIdentyfire(String type, Element el, List<MetadatumDTO> values) {
        for (String id : identifire2field.keySet()) {
            if (StringUtils.equals(id, type)) {
                String value = el.getAttributeValue("value");
                values.add(metadataFieldMapping.toDCValue(identifire2field.get(id), value));
            }
        }
    }

    public Map<String, MetadataFieldConfig> getIdentifire2field() {
        return identifire2field;
    }

    public void setIdentifire2field(Map<String, MetadataFieldConfig> identifire2field) {
        this.identifire2field = identifire2field;
    }

}