/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.service.other;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.dspace.content.Item;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.Query;
import org.dspace.importer.external.datamodel.ImportRecord;
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
 * Created by jonas - jonas@atmire.com on 06/11/15.
 */
public class ImportMetadataSourceServiceImpl extends org.dspace.importer.external.service.AbstractImportMetadataSourceService {
    private String baseAddress;

    private WebTarget pubmedWebTarget;


    @Override
    public int getNbRecords(String query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
    }

    @Override
    public int getNbRecords(Query query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new GetRecords(query, start, count));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query q) throws MetadataSourceException {
        return retry(new GetRecords(q));
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        return retry(new GetRecord(id));
    }

    @Override
    public ImportRecord getRecord(Query q) throws MetadataSourceException {
        return retry(new GetRecord(q));
    }

    @Override
    public String getImportSource() {
        return baseAddress;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        return retry(new FindMatchingRecords(item));
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query q) throws MetadataSourceException {
        return retry(new FindMatchingRecords(q));
    }

    @Override
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(baseAddress);
        pubmedWebTarget = webTarget.queryParam("db", "pubmed");
    }
    public String getBaseAddress() {
        return baseAddress;
    }

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
