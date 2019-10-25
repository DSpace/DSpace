/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.external.provider.impl;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.provider.impl.metadatamapping.contributors.MetadataContributor;
import org.dspace.mock.MockMetadataValue;
import org.jaxen.JaxenException;

/**
 * Implements a data source for querying PubMed Central
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class PubmedArticleDataProvider implements ExternalDataProvider {

    private static Logger log = LogManager.getLogger(PubmedArticleDataProvider.class);

    private String sourceIdentifier;
    private List<MetadataContributor> metadataFieldMapping;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        List<MockMetadataValue> mockMetadataValues = getRecord(id);
        ExternalDataObject externalDataObject = getExternalDataObjectFromMetadataValues(mockMetadataValues);
        return Optional.of(externalDataObject);
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        List<List<MockMetadataValue>> metadataValuesList = getRecords(query, start, limit);
        return metadataValuesList.stream().map(metadataValues -> getExternalDataObjectFromMetadataValues(metadataValues)).collect(
            Collectors.toList());
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        return getNbRecords(query);
    }


    private ExternalDataObject getExternalDataObjectFromMetadataValues(List<MockMetadataValue> metadataValues) {
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        externalDataObject.setMetadata(metadataValues);
        for (MockMetadataValue mockMetadataValue : metadataValues) {
            if (StringUtils.equals(mockMetadataValue.getSchema(), "dc") && StringUtils.equals(mockMetadataValue.getElement(), "title")) {
                externalDataObject.setDisplayValue(mockMetadataValue.getValue());
                externalDataObject.setValue(mockMetadataValue.getValue());
            }
            if (StringUtils.equals(mockMetadataValue.getSchema(), "dc") && StringUtils.equals(mockMetadataValue.getElement(), "identifier") && StringUtils.equals(mockMetadataValue.getQualifier(), "pubmed")) {
                externalDataObject.setId(mockMetadataValue.getValue());
            }
        }
        return externalDataObject;
    }

    private String baseAddress;

    private WebTarget pubmedWebTarget;

    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(baseAddress);
        pubmedWebTarget = webTarget.queryParam("db", "pubmed");
    }

    /**
     * Return the baseAddress set to this object
     *
     * @return The String object that represents the baseAddress of this object
     */
    public String getBaseAddress() {
        return baseAddress;
    }

    /**
     * Set the baseAddress to this object
     *
     * @param baseAddress The String object that represents the baseAddress of this object
     */
    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }


    public void setMetadataFieldMapping(List<MetadataContributor> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
    }

    public List<MetadataContributor> getMetadataFieldMapping() {
        return metadataFieldMapping;
    }
    /**
     * Find the number of records matching a query;
     *
     * @param query a query string to base the search on.
     * @return the sum of the matching records over this import source
     */
    public int getNbRecords(String query) {

        WebTarget getRecordIdsTarget = pubmedWebTarget.queryParam("term", query);

        getRecordIdsTarget = getRecordIdsTarget.path("esearch.fcgi");

        Invocation.Builder invocationBuilder = getRecordIdsTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Response response = invocationBuilder.get();

        String responseString = response.readEntity(String.class);

        String count = getSingleElementValue(responseString, "Count");

        return Integer.parseInt(count);
    }

    private String getSingleElementValue(String src, String elementName) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(src));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        String value = null;
        try {
            xpath = new AXIOMXPath("//" + elementName);
            List<OMElement> recordsList = xpath.selectNodes(element);
            if (!recordsList.isEmpty()) {
                value = recordsList.get(0).getText();
            }
        } catch (JaxenException e) {
            value = null;
        }
        return value;
    }

    /**
     * Find the number of records matching a string query. Supports pagination
     *
     * @param queryString a query string to base the search on.
     * @param start offset to start at
     * @param count number of records to retrieve.
     * @return a set of records. Fully transformed.
     */
    public List<List<MockMetadataValue>> getRecords(String queryString, int start, int count) {

        if (count < 0) {
            count = 10;
        }

        if (start < 0) {
            start = 0;
        }

        List<List<MockMetadataValue>> metadataValuesList = new LinkedList<>();

        WebTarget getRecordIdsTarget = pubmedWebTarget.queryParam("term", queryString);
        getRecordIdsTarget = getRecordIdsTarget.queryParam("retstart", start);
        getRecordIdsTarget = getRecordIdsTarget.queryParam("retmax", count);
        getRecordIdsTarget = getRecordIdsTarget.queryParam("usehistory", "y");
        getRecordIdsTarget = getRecordIdsTarget.path("esearch.fcgi");

        Invocation.Builder invocationBuilder = getRecordIdsTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Response response = invocationBuilder.get();
        String responseString = response.readEntity(String.class);

        String queryKey = getSingleElementValue(responseString, "QueryKey");
        String webEnv = getSingleElementValue(responseString, "WebEnv");

        WebTarget getRecordsTarget = pubmedWebTarget.queryParam("WebEnv", webEnv);
        getRecordsTarget = getRecordsTarget.queryParam("query_key", queryKey);
        getRecordsTarget = getRecordsTarget.queryParam("retmode", "xml");
        getRecordsTarget = getRecordsTarget.path("efetch.fcgi");
        getRecordsTarget = getRecordsTarget.queryParam("retmax", count);
        getRecordsTarget = getRecordsTarget.queryParam("retstart", start);

        invocationBuilder = getRecordsTarget.request(MediaType.TEXT_PLAIN_TYPE);
        response = invocationBuilder.get();

        List<OMElement> omElements = splitToRecords(response.readEntity(String.class));

        for (OMElement record : omElements) {
            List<MockMetadataValue> metadataValues = constructMetadataValueListFromOMElement(record);
            metadataValuesList.add(metadataValues);
        }

        return metadataValuesList;
    }

    private List<OMElement> splitToRecords(String recordsSrc) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        try {
            xpath = new AXIOMXPath("//PubmedArticle");
            List<OMElement> recordsList = xpath.selectNodes(element);
            return recordsList;
        } catch (JaxenException e) {
            return null;
        }
    }

    /**
     * Get a single record from the source.
     *
     * @param id identifier for the record
     * @return the first matching record
     */
    public List<MockMetadataValue> getRecord(String id) {

        WebTarget getRecordTarget = pubmedWebTarget.queryParam("id", id);
        getRecordTarget = getRecordTarget.queryParam("retmode", "xml");
        getRecordTarget = getRecordTarget.path("efetch.fcgi");

        Invocation.Builder invocationBuilder = getRecordTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Response response = invocationBuilder.get();

        List<OMElement> omElements = splitToRecords(response.readEntity(String.class));

        if (omElements.size() == 0) {
            return null;
        }

        return constructMetadataValueListFromOMElement(omElements.get(0));
    }

    private List<MockMetadataValue> constructMetadataValueListFromOMElement(OMElement record) {
        List<MockMetadataValue> values = new LinkedList<>();
        for (MetadataContributor<OMElement> metadataContributor : metadataFieldMapping) {
            try {
                values.addAll(metadataContributor.contributeMetadata(record));
            } catch (Exception e) {
                log.error("Error", e);
            }

        }
        return values;
    }
}
