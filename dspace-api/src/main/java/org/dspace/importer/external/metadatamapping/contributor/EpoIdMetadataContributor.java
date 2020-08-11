/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Resource;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.springframework.beans.factory.annotation.Required;

/**
 * Custom MetadataContributor to manage Epo ID.
 * Need as input <publication-reference> element and all children.
 * 
 * @author Pasquale Cavallo
 *
 */

public class EpoIdMetadataContributor implements MetadataContributor<OMElement> {
    protected MetadataFieldConfig field;

    private boolean needType;

    /**
     * This property will be used in ID definition.
     * If this is true, id will be in the form docType:EpoID, otherwise EpoID will be returned
     * 
     * @param needType if true, docType will be included in id definition
     */
    public void setNeedType(boolean needType) {
        this.needType = needType;
    }

    /**
     * Return prefixToNamespaceMapping
     *
     * @return a prefixToNamespaceMapping map
     */
    public Map<String, String> getPrefixToNamespaceMapping() {
        return prefixToNamespaceMapping;
    }

    protected MetadataFieldMapping<OMElement, MetadataContributor<OMElement>> metadataFieldMapping;

    /**
     * Return metadataFieldMapping
     *
     * @return MetadataFieldMapping
     */
    public MetadataFieldMapping<OMElement, MetadataContributor<OMElement>> getMetadataFieldMapping() {
        return metadataFieldMapping;
    }

    /**
     * Set the metadataFieldMapping of this SimpleXpathMetadatumContributor
     *
     * @param metadataFieldMapping the new mapping.
     */
    public void setMetadataFieldMapping(
        MetadataFieldMapping<OMElement, MetadataContributor<OMElement>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
    }

    /**
     * Set the prefixToNamespaceMapping for this object,
     *
     * @param prefixToNamespaceMapping the new mapping.
     */
    @Resource(name = "isiFullprefixMapping")
    public void setPrefixToNamespaceMapping(Map<String, String> prefixToNamespaceMapping) {
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
    }

    protected Map<String, String> prefixToNamespaceMapping;

    /**
     * Initialize EpoIdMetadataContributor with a query, prefixToNamespaceMapping and MetadataFieldConfig
     *
     * @param query                    query string
     * @param prefixToNamespaceMapping metadata prefix to namespace mapping
     * @param field
     * <a href="https://github.com/DSpace/DSpace/tree/master/dspace-api/src/main/java/org/dspace/importer/external#metadata-mapping-">MetadataFieldConfig</a>
     */
    public EpoIdMetadataContributor(String query, Map<String, String> prefixToNamespaceMapping,
        MetadataFieldConfig field) {
        this.query = query;
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
        this.field = field;
    }

    /**
     * Empty constructor for EpoIdMetadataContributor
     */
    public EpoIdMetadataContributor() {

    }

    protected String query;

    /**
     * Return the MetadataFieldConfig used while retrieving MetadatumDTO
     *
     * @return MetadataFieldConfig
     */
    public MetadataFieldConfig getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     *
     * @param field MetadataFieldConfig used while retrieving MetadatumDTO
     */
    @Required
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /**
     * Return query used to create an xpathExpression on, this query is used to
     *
     * @return the query this instance is based on
     */
    public String getQuery() {
        return query;
    }

    @Required
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Retrieve the metadata associated with the given object.
     * Depending on the retrieved node (using the query), different types of values will be added to the MetadatumDTO
     * list
     *
     * @param t A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        try {
            Map<String, String> namespaces = new HashMap<>();
            AXIOMXPath xpath = new AXIOMXPath(query);
            for (String ns : prefixToNamespaceMapping.keySet()) {
                xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
                namespaces.put(prefixToNamespaceMapping.get(ns), ns);
            }
            List<OMElement> nodes = xpath.selectNodes(t);
            for (OMElement el : nodes) {
                EpoDocumentId document = new EpoDocumentId(el, namespaces);
                MetadatumDTO metadatum = new MetadatumDTO();
                metadatum.setElement(field.getElement());
                metadatum.setQualifier(field.getQualifier());
                metadatum.setSchema(field.getSchema());
                if (needType) {
                    metadatum.setValue(document.getIdAndType());
                } else {
                    metadatum.setValue(document.getId());
                }
                values.add(metadatum);
            }
            return values;
        } catch (JaxenException e) {
            System.err.println(query);
            throw new RuntimeException(e);
        }

    }

    /**
     * This class maps EPO's response metadata needs to extract epo ID.
     * 
     * @author Pasquale Cavallo
     *
     */
    public static class EpoDocumentId {
        private String documentIdType;
        private String country;
        private String docNumber;
        private String kind;
        private String date;
        private Map<String, String> namespaces;


        private final String DOCDB = "docdb";
        private final String EPODOC = "epodoc";
        private final String ORIGIN = "origin";


        public EpoDocumentId(OMElement documentId, Map<String, String> namespaces) throws JaxenException {
            this.namespaces = namespaces;
            this.documentIdType = buildDocumentIdType(documentId);
            this.country = buildCountry(documentId);
            this.docNumber = buildDocNumber(documentId);
            this.kind = buildKind(documentId);
            this.date = buildDate(documentId);
        }

        private String buildDocumentIdType(OMElement documentId) throws JaxenException {
            return getElement(documentId, "//ns:document-id/@document-id-type");
        }

        private String buildCountry(OMElement documentId) throws JaxenException {
            return getElement(documentId, "//ns:country");
        }

        private String buildDocNumber(OMElement documentId) throws JaxenException {
            return getElement(documentId, "//ns:doc-number");
        }

        private String buildKind(OMElement documentId) throws JaxenException {
            return getElement(documentId, "//ns:kind");
        }

        private String buildDate(OMElement documentId) throws JaxenException {
            return getElement(documentId, "//ns:date");
        }


        public String getDocumentIdType() {
            return documentIdType;
        }
        /**
         * This method compute the epo ID from fields
         * 
         * @return the EPO id
         */
        public String getId() {
            if (DOCDB.equals(documentIdType)) {
                return country + "." + docNumber + "." + kind;

            } else if (EPODOC.equals(documentIdType)) {
                return docNumber + ((kind != null) ? kind : "");
            } else {
                return "";
            }
        }

        public String getIdAndType() {
            if (DOCDB.equals(documentIdType)) {
                return documentIdType + ":" + country + "." + docNumber + "." + kind;

            } else if (EPODOC.equals(documentIdType)) {
                return documentIdType + ":" + docNumber + ((kind != null) ? kind : "");
            } else {
                return "";
            }
        }


        private String getElement(OMElement documentId, String axiomPath) throws JaxenException {
            AXIOMXPath xpath = new AXIOMXPath(axiomPath);
            if (namespaces != null) {
                for (Entry<String, String> entry : namespaces.entrySet()) {
                    xpath.addNamespace(entry.getKey(), entry.getValue());
                }
            }
            List<Object> nodes = xpath.selectNodes(documentId);
            //exactly one element expected for any field
            if (nodes == null || nodes.isEmpty()) {
                return "";
            } else {
                return getValue(nodes.get(0));
            }
        }

        private String getValue(Object el) {
            if (el instanceof OMElement) {
                return ((OMElement) el).getText();
            } else if (el instanceof OMAttribute) {
                return ((OMAttribute) el).getAttributeValue();
            } else if (el instanceof String) {
                return (String)el;
            } else if (el instanceof OMText) {
                return ((OMText) el).getText();
            } else {
                System.err.println("node of type: " + el.getClass());
                return "";
            }
        }
    }

}
