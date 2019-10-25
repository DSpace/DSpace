/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.external.provider.impl;

import java.io.StringReader;
import java.util.Collection;
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
import org.dspace.content.Item;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.mock.MockMetadataValue;
import org.jaxen.JaxenException;

/**
 * Implements a data source for querying PubMed Central
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class PubmedArticleDataProvider extends AbstractImportMetadataSourceService<OMElement> implements ExternalDataProvider {

    private static Logger log = LogManager.getLogger(PubmedArticleDataProvider.class);

    private String sourceIdentifier;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        ImportRecord importRecord = getRecord(id);
        ExternalDataObject externalDataObject = getExternalDataObjectFromImportRecord(importRecord);

        //TODO MetadatumDTO naar mockmetadata
            //TODO Classes veranderen naar methods
            //TODO dc.identifier.other veranderen naar dc.identifier.pubmed
        return Optional.of(externalDataObject);
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        Collection<ImportRecord> importRecords = getRecords(query, start, limit);
        return importRecords.stream().map(importRecord -> getExternalDataObjectFromImportRecord(importRecord)).collect(
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


    private ExternalDataObject getExternalDataObjectFromImportRecord(ImportRecord importRecord) {
        List<MetadatumDTO> metadatumDTOList = importRecord.getValueList();
        List<MockMetadataValue> metadataValues = metadatumDTOList.stream().map(
            metadatumDTO -> new MockMetadataValue(metadatumDTO.getSchema(), metadatumDTO.getElement(),
                                                  metadatumDTO.getQualifier(), null,
                                                  metadatumDTO.getValue())).collect(Collectors.toList());
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        externalDataObject.setMetadata(metadataValues);
        for (MockMetadataValue mockMetadataValue : metadataValues) {
            if (StringUtils.equals(mockMetadataValue.getSchema(), "dc") && StringUtils.equals(mockMetadataValue.getElement(), "title")) {
                externalDataObject.setDisplayValue(mockMetadataValue.getValue());
                externalDataObject.setValue(mockMetadataValue.getValue());
            }
            if (StringUtils.equals(mockMetadataValue.getSchema(), "dc") && StringUtils.equals(mockMetadataValue.getElement(), "identifier") && StringUtils.equals(mockMetadataValue.getQualifier(), "other")) {
                mockMetadataValue.setQualifier("pubmed");
                externalDataObject.setId(mockMetadataValue.getValue());
            }
        }
        return externalDataObject;
    }

    private String baseAddress;

    private WebTarget pubmedWebTarget;

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    }


    /**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
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


    /**
     * Find the number of records matching a query;
     *
     * @param query a query object to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public int getNbRecords(Query query) throws MetadataSourceException {
        String queryString = query.getParameterAsClass("query", String.class);
        return getNbRecords(queryString);
    }

    /**
     * Find the number of records matching a query;
     *
     * @param query a query string to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
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
     * Find records based on a object query.
     *
     * @param query a query object to base the search on.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        String queryString = query.getParameterAsClass("query", String.class);
        Integer start = query.getParameterAsClass("start", Integer.class);
        Integer count = query.getParameterAsClass("count", Integer.class);
        return getRecords(queryString, start, count);
    }

    /**
     * Find the number of records matching a string query. Supports pagination
     *
     * @param queryString a query string to base the search on.
     * @param start offset to start at
     * @param count number of records to retrieve.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> getRecords(String queryString, int start, int count) {

        if (count < 0) {
            count = 10;
        }

        if (start < 0) {
            start = 0;
        }

        List<ImportRecord> records = new LinkedList<ImportRecord>();

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
            records.add(transformSourceRecords(record));
        }

        return records;
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
     * @param query a query matching a single record
     * @return the first matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        String id = query.getParameterAsClass("id", String.class);
        return getRecord(id);
    }


    /**
     * Get a single record from the source.
     *
     * @param id identifier for the record
     * @return the first matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public ImportRecord getRecord(String id) {

        WebTarget getRecordTarget = pubmedWebTarget.queryParam("id", id);
        getRecordTarget = getRecordTarget.queryParam("retmode", "xml");
        getRecordTarget = getRecordTarget.path("efetch.fcgi");

        Invocation.Builder invocationBuilder = getRecordTarget.request(MediaType.TEXT_PLAIN_TYPE);

        Response response = invocationBuilder.get();

        List<OMElement> omElements = splitToRecords(response.readEntity(String.class));

        if (omElements.size() == 0) {
            return null;
        }

        return transformSourceRecords(omElements.get(0));
    }


    /**
     * Finds records based on an item
     *
     * @param item an item to base the search on
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        Query query = getGenerateQueryForItem().generateQueryForItem(item);
        return findMatchingRecords(query);
    }

    /**
     * Finds records based on query object.
     * Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     *
     * @param query a query object to base the search on.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) {
        List<ImportRecord> records = new LinkedList<ImportRecord>();

        WebTarget getRecordIdsTarget = pubmedWebTarget
            .queryParam("term", query.getParameterAsClass("term", String.class));
        getRecordIdsTarget = getRecordIdsTarget
            .queryParam("field", query.getParameterAsClass("field", String.class));
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

        invocationBuilder = getRecordsTarget.request(MediaType.TEXT_PLAIN_TYPE);
        response = invocationBuilder.get();

        List<OMElement> omElements = splitToRecords(response.readEntity(String.class));

        for (OMElement record : omElements) {
            records.add(transformSourceRecords(record));
        }

        return records;
    }
}
