/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.service;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.dspace.content.Item;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.jaxen.JaxenException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Implements a data source for querying pubmed central
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class PubmedImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement> {
    private String baseAddress;

    private WebTarget pubmedWebTarget;

    /** Find the number of records matching a query;
     *
     * @param query a query string to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException
     */
    @Override
    public int getNbRecords(String query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
    }

    /** Find the number of records matching a query;
     *
     * @param query a query object to base the search on.
     * @return the sum of the matching records over this import source
     * @throws MetadataSourceException
     */    @Override
    public int getNbRecords(Query query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
    }

    /**  Find the number of records matching a string query. Supports pagination
     *
     * @param query a query string to base the search on.
     * @param start offset to start at
     * @param count number of records to retrieve.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException
     */
    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new GetRecords(query, start, count));
    }

    /** Find records based on a object query.
     *
     * @param query a query object to base the search on.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException
     */
    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        return retry(new GetRecords(query));
    }

    /** Get a single record from the source.
     * The first match will be returned
     * @param id identifier for the record
     * @return a matching record
     * @throws MetadataSourceException
     */
    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        return retry(new GetRecord(id));
    }

    /** Get a single record from the source.
     * The first match will be returned
     * @param query a query matching a single record
     * @return a matching record
     * @throws MetadataSourceException
     */
    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        return retry(new GetRecord(query));
    }

    /**
     * The string that identifies this import implementation. Preferable a URI
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    }

    /** Finds records based on an item
     * @param item an item to base the search on
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        return retry(new FindMatchingRecords(item));
    }

    /** Finds records based on query object.
     *  Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     * @param query a query object to base the search on.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException
     */
    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        return retry(new FindMatchingRecords(query));
    }

    /**
     * Initialize the class
     * @throws Exception
     */
    @Override
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(baseAddress);
        pubmedWebTarget = webTarget.queryParam("db", "pubmed");
    }

    /**
     * Return the baseAddress set to this object
     * @return The String object that represents the baseAddress of this object
     */
    public String getBaseAddress() {
        return baseAddress;
    }

    /**
     * Set the baseAddress to this object
     * @param baseAddress The String object that represents the baseAddress of this object
     */
    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    private class GetNbRecords implements Callable<Integer> {

        private GetNbRecords(String queryString) {
            query = new Query();
            query.addParameter("query",queryString);
        }

        private Query query;

        public GetNbRecords(Query query) {
            this.query = query;
        }

        @Override
        public Integer call() throws Exception {
            WebTarget getRecordIdsTarget = pubmedWebTarget.queryParam("term", query.getParameterAsClass("query", String.class));

            getRecordIdsTarget = getRecordIdsTarget.path("esearch.fcgi");

            Invocation.Builder invocationBuilder = getRecordIdsTarget.request(MediaType.TEXT_PLAIN_TYPE);

            Response response = invocationBuilder.get();

            String responseString = response.readEntity(String.class);

            String count = getSingleElementValue(responseString, "Count");

            return Integer.parseInt(count);
        }
    }


    private String getSingleElementValue(String src, String elementName){
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(src));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        String value = null;
        try {
            xpath = new AXIOMXPath("//" + elementName);
            List<OMElement> recordsList = xpath.selectNodes(element);
            if(!recordsList.isEmpty()) {
                value = recordsList.get(0).getText();
            }
        } catch (JaxenException e) {
            value = null;
        }
        return value;
    }

    private class GetRecords implements Callable<Collection<ImportRecord>> {

        private Query query;

        private GetRecords(String queryString, int start, int count) {
            query = new Query();
            query.addParameter("query",queryString);
            query.addParameter("start",start);
            query.addParameter("count",count);
        }

        private GetRecords(Query q) {
            this.query = q;
        }

        @Override
        public Collection<ImportRecord> call() throws Exception {
            String queryString = query.getParameterAsClass("query",String.class);
            Integer start = query.getParameterAsClass("start",Integer.class);
            Integer count = query.getParameterAsClass("count",Integer.class);

            if(count==null || count < 0){
                count = 10;
            }

            if(start==null || start < 0){
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

    private class GetRecord implements Callable<ImportRecord> {

        private Query query;

        private GetRecord(String id) {
            query = new Query();
            query.addParameter("id",id);
        }

        public GetRecord(Query q) {
            query = q;
        }

        @Override
        public ImportRecord call() throws Exception {
            String id = query.getParameterAsClass("id", String.class);

            WebTarget getRecordTarget = pubmedWebTarget.queryParam("id", id);
            getRecordTarget = getRecordTarget.queryParam("retmode", "xml");
            getRecordTarget = getRecordTarget.path("efetch.fcgi");

            Invocation.Builder invocationBuilder = getRecordTarget.request(MediaType.TEXT_PLAIN_TYPE);

            Response response = invocationBuilder.get();

            List<OMElement> omElements = splitToRecords(response.readEntity(String.class));

            if(omElements.size()==0) {
                return null;
            }

            return transformSourceRecords(omElements.get(0));
        }
    }

    private class FindMatchingRecords implements Callable<Collection<ImportRecord>> {

        private Query query;

        private FindMatchingRecords(Item item) throws  MetadataSourceException {
            query = getGenerateQueryForItem().generateQueryForItem(item);
        }

        public FindMatchingRecords(Query q) {
            query = q;
        }

        @Override
        public Collection<ImportRecord> call() throws Exception {
            List<ImportRecord> records = new LinkedList<ImportRecord>();

            WebTarget getRecordIdsTarget = pubmedWebTarget.queryParam("term", query.getParameterAsClass("term", String.class));
            getRecordIdsTarget = getRecordIdsTarget.queryParam("field", query.getParameterAsClass("field",String.class));
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
}
