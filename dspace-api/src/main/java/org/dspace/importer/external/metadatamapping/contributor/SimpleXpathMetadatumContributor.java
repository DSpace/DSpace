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
import javax.annotation.Resource;

import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Metadata contributor that takes a JDOM Element and turns it into a metadatum
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class SimpleXpathMetadatumContributor implements MetadataContributor<Element> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    protected MetadataFieldConfig field;

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
    @Override
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
     * Initialize SimpleXpathMetadatumContributor with a query, prefixToNamespaceMapping and MetadataFieldConfig
     *
     * @param query                    query string
     * @param prefixToNamespaceMapping metadata prefix to namespace mapping
     * @param field
     * <a href="https://github.com/DSpace/DSpace/tree/main/dspace-api/src/main/java/org/dspace/importer/external#metadata-mapping-">MetadataFieldConfig</a>
     */
    public SimpleXpathMetadatumContributor(String query, Map<String, String> prefixToNamespaceMapping,
                                           MetadataFieldConfig field) {
        this.query = query;
        this.prefixToNamespaceMapping = prefixToNamespaceMapping;
        this.field = field;
    }

    /**
     * Empty constructor for SimpleXpathMetadatumContributor
     */
    public SimpleXpathMetadatumContributor() {

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
    @Autowired(required = true)
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

    @Autowired(required = true)
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Retrieve the metadata associated with the given object.
     * Depending on the retrieved node (using the query), different types of values will be added to the MetadatumDTO
     * list
     *
     * @param t An element to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element t) {
        List<MetadatumDTO> values = new LinkedList<>();

        List<Namespace> namespaces = new ArrayList<>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
        }
        XPathExpression<Object> xpath = XPathFactory.instance().compile(query, Filters.fpassthrough(), null,namespaces);
        List<Object> nodes = xpath.evaluate(t);
        for (Object el : nodes) {
            if (el instanceof Element) {
                values.add(metadataFieldMapping.toDCValue(field, ((Element) el).getText()));
            } else if (el instanceof Attribute) {
                values.add(metadataFieldMapping.toDCValue(field, ((Attribute) el).getValue()));
            } else if (el instanceof String) {
                values.add(metadataFieldMapping.toDCValue(field, (String) el));
            } else if (el instanceof Text) {
                values.add(metadataFieldMapping.toDCValue(field, ((Text) el).getText()));
            } else {
                log.error("Encountered unsupported XML node of type: {}. Skipped that node.", el.getClass());
            }
        }
        return values;
    }

}