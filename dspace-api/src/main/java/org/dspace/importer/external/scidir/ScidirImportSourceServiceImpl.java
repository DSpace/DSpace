/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scidir;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.Query;
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
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 28/09/15
 * Time: 13:25
 */
public class ScidirImportSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement> {
    private WebTarget scidirWebTarget;
    private String apiUrl;

    private static Logger log = Logger.getLogger(ScidirImportSourceServiceImpl.class);

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
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
            WebTarget getRecordIdsTarget = scidirWebTarget.queryParam("query", query.getParameterAsClass("query", String.class));

//            getRecordIdsTarget = getRecordIdsTarget.path("esearch.fcgi");

            Invocation.Builder invocationBuilder = getRecordIdsTarget.request(MediaType.TEXT_XML_TYPE);

            Response response = invocationBuilder.get();

            String responseString = response.readEntity(String.class);

            String count = getSingleElementValue(responseString, "opensearch:totalResults");

            try {
                return Integer.parseInt(count);
            } catch(NumberFormatException e) {
                log.error(e.getMessage(), e);
                return 0;
            }
        }
    }

    @Override
    public int getNbRecords(String query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
    }

    @Override
    public int getNbRecords(Query query) throws MetadataSourceException {
        return retry(new GetNbRecords(query));
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

            WebTarget getRecordIdsTarget = scidirWebTarget.queryParam("query", queryString);
            getRecordIdsTarget = getRecordIdsTarget.queryParam("start", start);
            getRecordIdsTarget = getRecordIdsTarget.queryParam("count", count);


            Invocation.Builder invocationBuilder = getRecordIdsTarget.request(MediaType.TEXT_XML_TYPE);
            Response response = invocationBuilder.get();

            List<OMElement> omElements = splitToRecords(response.readEntity(String.class));

            for (OMElement record : omElements) {
                records.add(transformSourceRecords(record));
            }

            return records;
        }
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        return retry(new GetRecords(query, start, count));
    }

    @Override
    public Collection<ImportRecord> getRecords(Query q) throws MetadataSourceException {
        return retry(new GetRecords(q));
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

            WebTarget getRecordTarget = scidirWebTarget.queryParam("query", "eid(" + id + ")");

            Invocation.Builder invocationBuilder = getRecordTarget.request(MediaType.TEXT_XML_TYPE);
            Response response = invocationBuilder.get();

            List<OMElement> omElements = splitToRecords(response.readEntity(String.class));

            if(omElements.size()==0) {
                return null;
            }

            return transformSourceRecords(omElements.get(0));
        }
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
        return apiUrl;
//        return ConfigurationManager.getProperty("elsevier-sciencedirect","api.url");
    }

    private class FindMatchingRecords implements Callable<Collection<ImportRecord>> {

        private Query query;

        private FindMatchingRecords(Item item) throws MetadataSourceException {
            query = getGenerateQueryForItem().generateQueryForItem(item);
        }

        public FindMatchingRecords(Query q) {
            query = q;
        }

        @Override
        public Collection<ImportRecord> call() throws Exception {
            List<ImportRecord> records = new LinkedList<ImportRecord>();

            WebTarget getRecordIdsTarget = scidirWebTarget.queryParam("query", query.getParameterAsClass("query", String.class));
            Invocation.Builder invocationBuilder = getRecordIdsTarget.request(MediaType.TEXT_XML_TYPE);

            Response response = invocationBuilder.get();
            List<OMElement> omElements = splitToRecords(response.readEntity(String.class));

            for (OMElement record : omElements) {
                records.add(transformSourceRecords(record));
            }

            return records;
        }
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
        String baseAddress = apiUrl;
        String apiKey = ConfigurationManager.getProperty("elsevier-sciencedirect", "api.key");
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(baseAddress);
        scidirWebTarget = webTarget.queryParam("httpAccept", "application/xml");
        scidirWebTarget = scidirWebTarget.queryParam("apiKey", apiKey);
    }

    private String getSingleElementValue(String src, String elementName){
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(src));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        String value = null;
        try {
            xpath = new AXIOMXPath("//" + elementName);
            xpath.addNamespace("dc", "http://purl.org/dc/elements/1.1/");
            xpath.addNamespace("opensearch", "http://a9.com/-/spec/opensearch/1.1/");
            List<OMElement> recordsList = xpath.selectNodes(element);
            if(!recordsList.isEmpty()) {
                value = recordsList.get(0).getText();
            }
        } catch (JaxenException e) {
            value = null;
        }
        return value;
    }

    private List<OMElement> splitToRecords(String recordsSrc) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
        OMElement element = records.getDocumentElement();

        Iterator childElements = element.getChildElements();

        List<OMElement> recordsList = new ArrayList<>();

        while (childElements.hasNext()){
            OMElement next = (OMElement) childElements.next();

            if(next.getLocalName().equals("entry")){
                recordsList.add(next);
            }
        }
        return recordsList;
    }
}
