/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl.metadatamapping.contributors;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.dspace.content.dto.MetadataFieldDTO;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.provider.impl.pubmed.metadatamapping.utils.MetadatumContributorUtils;
import org.jaxen.JaxenException;
import org.springframework.beans.factory.annotation.Required;

/**
 * Metadata contributor that takes an axiom OMElement and turns it into a metadatum
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class SimpleXpathMetadatumContributor implements MetadataContributor<OMElement> {
    private MetadataFieldDTO field;

    /**
     * Initialize SimpleXpathMetadatumContributor with a query, prefixToNamespaceMapping and MetadataFieldConfig
     *
     * @param query                    query string
     * @param field
     * <a href="https://github.com/DSpace/DSpace/tree/master/dspace-api/src/main/java/org/dspace/importer/external#metadata-mapping-">MetadataFieldConfig</a>
     */
    public SimpleXpathMetadatumContributor(String query, MetadataFieldDTO field) {
        this.query = query;
        this.field = field;
    }

    /**
     * Empty constructor for SimpleXpathMetadatumContributor
     */
    public SimpleXpathMetadatumContributor() {

    }

    private String query;

    /**
     * Return the MetadataFieldConfig used while retrieving MetadatumDTO
     *
     * @return MetadataFieldConfig
     */
    public MetadataFieldDTO getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     *
     * @param field MetadataFieldConfig used while retrieving MetadatumDTO
     */
    @Required
    public void setField(MetadataFieldDTO field) {
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
    public Collection<MetadataValueDTO> contributeMetadata(OMElement t) {
        List<MetadataValueDTO> values = new LinkedList<>();
        try {
            AXIOMXPath xpath = new AXIOMXPath(query);
            List<Object> nodes = xpath.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    values.add(MetadatumContributorUtils.toMockMetadataValue(field, ((OMElement) el).getText()));
                } else if (el instanceof OMAttribute) {
                    values.add(MetadatumContributorUtils.toMockMetadataValue(field,
                                                                             ((OMAttribute) el).getAttributeValue()));
                } else if (el instanceof String) {
                    values.add(MetadatumContributorUtils.toMockMetadataValue(field, (String) el));
                } else if (el instanceof OMText) {
                    values.add(MetadatumContributorUtils.toMockMetadataValue(field, ((OMText) el).getText()));
                } else {
                    System.err.println("node of type: " + el.getClass());
                }
            }
            return values;
        } catch (JaxenException e) {
            throw new RuntimeException(e);
        }

    }
}
