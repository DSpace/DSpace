/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Custom MetadataContributor to manage Epo ID.
 * Need as input <publication-reference> element and all children.
 * 
 * @author Pasquale Cavallo
 */
public class EpoIdMetadataContributor implements MetadataContributor<Element> {

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

    protected MetadataFieldMapping<Element, MetadataContributor<Element>> metadataFieldMapping;

    /**
     * Return metadataFieldMapping
     *
     * @return MetadataFieldMapping
     */
    public MetadataFieldMapping<Element, MetadataContributor<Element>> getMetadataFieldMapping() {
        return metadataFieldMapping;
    }

    /**
     * Set the metadataFieldMapping of this SimpleXpathMetadatumContributor
     *
     * @param metadataFieldMapping the new mapping.
     */
    public void setMetadataFieldMapping(
        MetadataFieldMapping<Element, MetadataContributor<Element>> metadataFieldMapping) {
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
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        try {
            List<Namespace> namespaces = Arrays.asList(
                    Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink"),
                    Namespace.getNamespace("ops", "http://ops.epo.org"),
                    Namespace.getNamespace("ns", "http://www.epo.org/exchange"));
            XPathExpression<Element> xpath = XPathFactory.instance().compile(query, Filters.element(), null,
                    namespaces);
            List<Element> elements = xpath.evaluate(element);
            for (Element el : elements) {
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
        private List<Namespace> namespaces;


        public static final String DOCDB = "docdb";
        public static final String EPODOC = "epodoc";
        public static final String ORIGIN = "origin";


        public EpoDocumentId(Element documentId, List<Namespace> namespaces) throws JaxenException {
            this.namespaces = namespaces;
            Element preferredId = null;
            XPathExpression<Object> xpath = XPathFactory.instance().compile(
                    "./ns:document-id[@document-id-type=\"epodoc\"]", Filters.fpassthrough(), null, namespaces);

            List<Object> nodes = xpath.evaluate(documentId);
            if (CollectionUtils.isNotEmpty(nodes)) {
                preferredId = (Element) nodes.get(0);
            }
            if (Objects.isNull(preferredId)) {
                preferredId = documentId;
            }

            this.documentIdType = buildDocumentIdType(preferredId);
            this.country = buildCountry(preferredId);
            this.docNumber = buildDocNumber(preferredId);
            this.kind = buildKind(preferredId);
            this.date = buildDate(preferredId);
        }

        private String buildDocumentIdType(Element documentId) throws JaxenException {
            return getElement(documentId, "./@document-id-type");
        }

        private String buildCountry(Element documentId) throws JaxenException {
            return getElement(documentId, "./ns:country");
        }

        private String buildDocNumber(Element documentId) throws JaxenException {
            return getElement(documentId, "./ns:doc-number");
        }

        private String buildKind(Element documentId) throws JaxenException {
            return getElement(documentId, "./ns:kind");
        }

        private String buildDate(Element documentId) throws JaxenException {
            return getElement(documentId, "./ns:date");
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
                return docNumber + ((kind != null) ? kind : StringUtils.EMPTY);
            } else {
                return StringUtils.EMPTY;
            }
        }

        public String getIdAndType() {
            if (EPODOC.equals(documentIdType)) {
                return documentIdType + ":" + docNumber + ((kind != null) ? kind : "");
            } else if (DOCDB.equals(documentIdType)) {
                return documentIdType + ":" + country + "." + docNumber + "." + kind;
            } else {
                return StringUtils.EMPTY;
            }
        }


        private String getElement(Element documentId, String path) throws JaxenException {
            if (Objects.isNull(documentId)) {
                return StringUtils.EMPTY;
            }
            XPathExpression<Object> xpath = XPathFactory.instance().compile(path, Filters.fpassthrough(), null,
                    namespaces);
            List<Object> nodes = xpath.evaluate(documentId);
            //exactly one element expected for any field
            return CollectionUtils.isNotEmpty(nodes) ? getValue(nodes.get(0)) : StringUtils.EMPTY;
        }

        private String getValue(Object el) {
            if (el instanceof Element) {
                return ((Element) el).getText();
            } else if (el instanceof Attribute) {
                return ((Attribute) el).getValue();
            } else if (el instanceof String) {
                return (String)el;
            } else if (el instanceof Text) {
                return ((Text) el).getText();
            } else {
                return StringUtils.EMPTY;
            }
        }
    }

}