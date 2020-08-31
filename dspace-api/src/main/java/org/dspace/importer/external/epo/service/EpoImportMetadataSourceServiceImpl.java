/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.epo.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.util.Base64;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.contributor.EpoIdMetadataContributor.EpoDocumentId;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.ConfigurationService;
import org.jaxen.JaxenException;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implements a data source for querying EPO
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4Science dot it)
 *
 */
public class EpoImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement>
    implements QuerySource {

    private String consumerKey;
    private String consumerSecret;


    private static final Logger log = Logger.getLogger(EpoImportMetadataSourceServiceImpl.class);

    private static final String endPointAuthService =
            "https://ops.epo.org/3.2/auth/accesstoken";
    private static final String endPointPublisherDataSearchService =
            "http://ops.epo.org/rest-services/published-data/search";
    private static final String endPointPublisherDataRetriveService =
            "http://ops.epo.org/rest-services/published-data/publication/$(doctype)/$(id)/biblio";

    @Autowired
    private ConfigurationService configurationService;

/**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {
    }

    /**
     * The string that identifies this import implementation. Preferable a URI
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "epo";
    }

    /**
     * Set the customer epo key
     * @param consumerKey the customer consumer key
     */
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    /**
     * Set the costumer epo secret
     * @param consumerSecret the customer epo secret
     */
    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    /***
     * Log to EPO, bearer is valid for 20 minutes
     * 
     * @param consumerKey       The consumer Key
     * @param consumerSecretKey The consumer secret key
     * @return
     * @throws IOException
     * @throws HttpException
     */
    protected String login() throws IOException, HttpException {
        HttpPost method = null;
        String accessToken = null;
        fillKey();
        try {
            HttpClient client = HttpClientBuilder.create().build();
            method = new HttpPost(endPointAuthService);
            String authString = consumerKey + ":" + consumerSecret;
            method.setHeader("Authorization", "Basic " + Base64.encode(authString.getBytes()));
            method.setHeader("Content-type", "application/x-www-form-urlencoded");
            StringEntity entity = new StringEntity("grant_type=client_credentials");
            method.setEntity(entity);
            HttpResponse httpResponse = client.execute(method);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException(
                        "Call to " + endPointAuthService + " fails: " + httpResponse.getStatusLine());
            }
            String json = EntityUtils.toString(httpResponse.getEntity());
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            JsonNode rootNode = mapper.readTree(json);
            JsonNode accessTokenNode = rootNode.get("access_token");
            accessToken = accessTokenNode.asText();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return accessToken;
    }


    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        fillKey();
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                return retry(new CountRecordsCallable(query, bearer));
            } catch (IOException | HttpException e) {
                e.printStackTrace();
            }
        }
        return 0;

    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        fillKey();
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                return retry(new CountRecordsCallable(query, bearer));
            } catch (IOException | HttpException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start,
            int count) throws MetadataSourceException {
        fillKey();
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                return retry(new SearchByQueryCallable(query, bearer, start, count));
            } catch (IOException | HttpException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query)
            throws MetadataSourceException {
        fillKey();
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                return retry(new SearchByQueryCallable(query, bearer));
            } catch (IOException | HttpException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        fillKey();
        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
            try {
                String bearer = login();
                List<ImportRecord> list = retry(new SearchByIdCallable(id, bearer));
                if (list == null || list.isEmpty()) {
                    return null;
                } else {
                    return list.get(0);
                }
            } catch (IOException | HttpException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        fillKey();
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item)
            throws MetadataSourceException {
        fillKey();
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query)
            throws MetadataSourceException {
        fillKey();
        return null;
    }

    private class CountRecordsCallable implements Callable<Integer> {

        String bearer;
        String query;

        private CountRecordsCallable(Query query, String bearer) {
            this.query = query.getParameterAsClass("query", String.class);
            this.bearer = bearer;
        }

        private CountRecordsCallable(String query, String bearer) {
            this.query = query;
            this.bearer = bearer;
        }

        public Integer call() throws Exception {
            return countDocument(bearer, query);
        }
    }


    private class SearchByIdCallable implements Callable<List<ImportRecord>> {
        String bearer;
        String id;

        private SearchByIdCallable(String id, String bearer) {
            this.id = id;
            this.bearer = bearer;
        }

        public List<ImportRecord> call() throws Exception {
            int positionToSplit = id.indexOf(":");
            String docType = id.substring(0, positionToSplit);
            String idS = id.substring(positionToSplit + 1, id.length());
            List<ImportRecord> records = searchDocument(bearer, idS, docType);
            if (records.size() > 1) {
                log.warn("More record are returned with epocID " + id);
            }
            return records;
        }
    }


    private class SearchByQueryCallable implements Callable<List<ImportRecord>> {
        private Query query;
        private String bearer;
        private Integer start;
        private Integer count;

        private SearchByQueryCallable(Query query, String bearer) {
            this.query = query;
            this.bearer = bearer;
        }


        public SearchByQueryCallable(String queryValue, String bearer, int start, int count) {
            this.query = new Query();
            query.addParameter("query", queryValue);
            this.start = query.getParameterAsClass("start", Integer.class) != null ?
                query.getParameterAsClass("start", Integer.class) : 0;
            this.count = query.getParameterAsClass("count", Integer.class) != null ?
                query.getParameterAsClass("count", Integer.class) : 20;
            this.bearer = bearer;
        }

        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> records = new ArrayList<ImportRecord>();
            String queryString = query.getParameterAsClass("query", String.class);
            if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
                if (StringUtils.isNotBlank(queryString) && StringUtils.isNotBlank(bearer)) {
                    List<EpoDocumentId> epoDocIds = searchDocumentIds(bearer, queryString, start + 1, count);
                    for (EpoDocumentId epoDocId : epoDocIds) {
                        List<ImportRecord> recordfounds = searchDocument(bearer, epoDocId);
                        if (recordfounds.size() > 1) {
                            log.warn("More record are returned with epocID " + epoDocId.toString());
                        }
                        records.addAll(recordfounds);
                    }
                }

            }
            return records;
        }
    }

    private Integer countDocument(String bearer, String query) {
        if (StringUtils.isBlank(bearer)) {
            return null;
        }
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet method = new HttpGet(endPointPublisherDataSearchService);
            method.setHeader("Authorization", "Bearer " + bearer);
            method.setHeader("X-OPS-Range", "1-1");
            URI uri = new URIBuilder(method.getURI()).addParameter("q", query).build();
            ((HttpRequestBase) method).setURI(uri);
            HttpResponse httpResponse = client.execute(method);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException(
                        "Call to " + endPointPublisherDataSearchService + " fails: " + httpResponse.getStatusLine());
            }
            InputStream is = httpResponse.getEntity().getContent();
            String response = IOUtils.toString(is, Charsets.UTF_8);
            System.out.println(response);
            Map<String, String> epoNamespaces = new HashMap<>();
            epoNamespaces.put("xlink", "http://www.w3.org/1999/xlink");
            epoNamespaces.put("ops", "http://ops.epo.org");
            epoNamespaces.put("ns", "http://www.epo.org/exchange");
            OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(response));
            OMElement element = records.getDocumentElement();
            String totalRes = getElement(element, epoNamespaces, "//ops:biblio-search/@total-result-count");
            return Integer.parseInt(totalRes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
    private List<EpoDocumentId> searchDocumentIds(String bearer, String query, int start, int count) {
        List<EpoDocumentId> results = new ArrayList<EpoDocumentId>();
        int end = start + count;
        if (StringUtils.isBlank(bearer)) {
            return results;
        }
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet method = new HttpGet(endPointPublisherDataSearchService);
            method.setHeader("Authorization", "Bearer " + bearer);
            if (start >= 1 && end > start) {
                method.setHeader("X-OPS-Range", start + "-" + end);
            }
            URI uri = new URIBuilder(method.getURI()).addParameter("q", query).build();
            ((HttpRequestBase) method).setURI(uri);
            HttpResponse httpResponse = client.execute(method);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException(
                        "Call to " + endPointPublisherDataSearchService + " fails: " + httpResponse.getStatusLine());
            }
            InputStream is = httpResponse.getEntity().getContent();
            String response = IOUtils.toString(is, Charsets.UTF_8);
            System.out.println(response);
            Map<String, String> epoNamespaces = new HashMap<>();
            epoNamespaces.put("xlink", "http://www.w3.org/1999/xlink");
            epoNamespaces.put("ops", "http://ops.epo.org");
            epoNamespaces.put("ns", "http://www.epo.org/exchange");
            OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(response));
            OMElement element = records.getDocumentElement();
            //    <ops:biblio-search total-result-count="10000">
            String totalRes = getElement(element, epoNamespaces, "//ops:biblio-search/@total-result-count");
            List<OMElement> documentIds = splitToRecords(element, epoNamespaces, "//ns:document-id");
            for (OMElement documentId : documentIds) {
                results.add(new EpoDocumentId(documentId, epoNamespaces));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return results;
    }

    private List<ImportRecord> searchDocument(String bearer, EpoDocumentId id) {
        return searchDocument(bearer, id.getId(), id.getDocumentIdType());
    }

    private List<ImportRecord> searchDocument(String bearer, String id, String docType) {
        List<ImportRecord> results = new ArrayList<ImportRecord>();
        if (StringUtils.isBlank(bearer)) {
            return results;
        }
        try {
            String endPointPublisherDataRetriveService = this.endPointPublisherDataRetriveService;
            endPointPublisherDataRetriveService = endPointPublisherDataRetriveService
                    .replace("$(doctype)", docType).replace("$(id)", id);
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet method = new HttpGet(endPointPublisherDataRetriveService);
            method.setHeader("Authorization", "Bearer " + bearer);

            HttpResponse httpResponse = client.execute(method);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException(
                        "Call to " + endPointPublisherDataRetriveService + " fails: " + httpResponse.getStatusLine());
            }
            InputStream is = httpResponse.getEntity().getContent();
            String response = IOUtils.toString(is, Charsets.UTF_8);
            System.out.println(response);
            List<OMElement> omElements = splitToRecords(response);
            for (OMElement record : omElements) {
                results.add(transformSourceRecords(record));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return results;
    }

    private List<OMElement> splitToRecords(OMElement document, Map<String, String> namespaces, String axiomPath) {
        AXIOMXPath xpath = null;
        try {
            xpath = new AXIOMXPath(axiomPath);
            if (namespaces != null) {
                for (Entry<String, String> entry : namespaces.entrySet()) {
                    xpath.addNamespace(entry.getKey(), entry.getValue());
                }
            }
            List<OMElement> recordsList = xpath.selectNodes(document);
            return recordsList;
        } catch (JaxenException e) {
            return null;
        }
    }


    private List<OMElement> splitToRecords(String recordsSrc) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        try {
            xpath = new AXIOMXPath("//ns:exchange-document");
            xpath.addNamespace("ns", "http://www.epo.org/exchange");
            List<OMElement> recordsList = xpath.selectNodes(element);
            return recordsList;
        } catch (JaxenException e) {
            return null;
        }
    }

    private String getElement(OMElement document, Map<String, String> namespaces,
        String axiomPath) throws JaxenException {
        AXIOMXPath xpath = new AXIOMXPath(axiomPath);
        if (namespaces != null) {
            for (Entry<String, String> entry : namespaces.entrySet()) {
                xpath.addNamespace(entry.getKey(), entry.getValue());
            }
        }
        List<Object> nodes = xpath.selectNodes(document);
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

    private void fillKey() {
        this.consumerKey = configurationService.getProperty("submission.lookup.epo.consumerKey");
        this.consumerSecret = configurationService.getProperty("submission.lookup.epo.consumerSecretKey");
    }
}
