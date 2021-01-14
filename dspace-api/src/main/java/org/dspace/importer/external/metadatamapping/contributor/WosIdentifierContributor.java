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
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This contributor can retrieve the identifiers
 * configured in "this.identifire2field" from the WOS response.
 * 
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4Science dot it)
 */
public class WosIdentifierContributor extends SimpleXpathMetadatumContributor {

    private static final Logger log = LoggerFactory.getLogger(WosIdentifierContributor.class);

    protected Map<String, MetadataFieldConfig> identifire2field;

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
                    String type = element.getAttributeValue(new QName("type"));
                    setIdentyfire(type, element, values);
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
        for (String id : identifire2field.keySet()) {
            if (StringUtils.equals(id, type)) {
                String value = el.getAttributeValue(new QName("value"));
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