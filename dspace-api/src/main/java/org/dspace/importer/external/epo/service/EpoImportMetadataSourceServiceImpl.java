/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.epo.service;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
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
import org.dspace.app.util.XMLUtils;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.submit.lookup.EPODocumentId;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Implements a data source for querying EPO
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4Science dot it)
 *
 */
public class EpoImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<OMElement>
    implements QuerySource {

    private WebTarget webTarget;
    private String baseAddress;
    private String consumerKey;
    private String consumerSecret;

    // max retrival data is set to 1000 by EPO
    private static final int EPO_LIMIT = 1000;
    private static final int START = 1;
    private static final int SIZE = 25;
    private static final int MAX = EPO_LIMIT / SIZE + 1;

    private static final Logger log = Logger.getLogger(EpoImportMetadataSourceServiceImpl.class);

    private static final String endPointAuthService =
            "https://ops.epo.org/3.2/auth/accesstoken";
    private static final String endPointPublisherDataSearchService =
            "http://ops.epo.org/rest-services/published-data/search";
    private static final String endPointPublisherDataRetriveService =
            "http://ops.epo.org/rest-services/published-data/publication/$(doctype)/$(id)/biblio";
/**
     * Initialize the class
     *
     * @throws Exception on generic exception
     */
    @Override
    public void init() throws Exception {
        Client client = ClientBuilder.newClient();
        webTarget = client.target(baseAddress);
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
            // find "access_token"
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
    public int getNbRecords(String query) throws MetadataSourceException {
        return 0;
    }

    @Override
    public int getNbRecords(Query query) throws MetadataSourceException {
        return 0;
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start,
            int count) throws MetadataSourceException {
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
    public Collection<ImportRecord> getRecords(Query query)
            throws MetadataSourceException {
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
        return null;
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item)
            throws MetadataSourceException {
        return null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query)
            throws MetadataSourceException {
        return null;
    }


    private class SearchByQueryCallable implements Callable<List<ImportRecord>> {
        private Query query;
        private String bearer;

        private SearchByQueryCallable(String queryString, String bearer) {
            this.query = new Query();
            query.addParameter("query", queryString);
            this.bearer = bearer;
        }

        private SearchByQueryCallable(Query query, String bearer) {
            this.query = query;
            this.bearer = bearer;
        }


        @Override
        public List<ImportRecord> call() throws Exception {
            List<ImportRecord> records = new ArrayList<ImportRecord>();
            String queryString = query.getParameterAsClass("query", String.class);
            if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecret)) {
                if (StringUtils.isNotBlank(queryString) && StringUtils.isNotBlank(bearer)) {
                    int start = START;
                    int end = SIZE;
                    List<EPODocumentId> epoDocIds = new ArrayList<EPODocumentId>();

                    for (int i = 0; i < MAX; i++) {
                        List<EPODocumentId> ids = searchDocumentIds(bearer, queryString, start, end);
                        start = end + 1;
                        end = end + SIZE;
                        if (ids.size() > 0) {
                            epoDocIds.addAll(ids);
                        }
                        if (ids.size() < SIZE) {
                            break;
                        }
                    }
                    for (EPODocumentId epoDocId : epoDocIds) {
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

    private List<EPODocumentId> searchDocumentIds(String bearer, String query, int start, int end) {
        List<EPODocumentId> results = new ArrayList<EPODocumentId>();

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
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document inDoc = builder.parse(httpResponse.getEntity().getContent());
            inDoc.getDocumentElement().normalize();

            Element xmlRoot = inDoc.getDocumentElement();
            Element biblio = XMLUtils.getSingleElement(xmlRoot, "ops:biblio-search");
            String totalRes = biblio.getAttribute("total-result-count");
            Element range = XMLUtils.getSingleElement(biblio, "ops:range");
            String beginRange = range.getAttribute("begin");
            String endRange = range.getAttribute("end");
            Element searchResult = XMLUtils.getSingleElement(biblio, "ops:search-result");
            List<Element> pubReferences = XMLUtils.getElementList(searchResult, "ops:publication-reference");
            for (Element pubReference : pubReferences) {
                Element documentId = XMLUtils.getSingleElement(pubReference, "document-id");

                results.add(new EPODocumentId(documentId));
            }
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (SAXException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return results;
    }

    private List<ImportRecord> searchDocument(String bearer, EPODocumentId epoDocId) {
        List<ImportRecord> results = new ArrayList<ImportRecord>();

        if (StringUtils.isBlank(bearer)) {
            return results;
        }
        try {
            String endPointPublisherDataRetriveService = this.endPointPublisherDataRetriveService;
            endPointPublisherDataRetriveService = endPointPublisherDataRetriveService
                    .replace("$(doctype)", epoDocId.getDocumentIdType()).replace("$(id)", epoDocId.getId());
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet method = new HttpGet(endPointPublisherDataRetriveService);
            method.setHeader("Authorization", "Bearer " + bearer);

            HttpResponse httpResponse = client.execute(method);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException(
                        "Call to " + endPointPublisherDataRetriveService + " fails: " + httpResponse.getStatusLine());
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document inDoc = builder.parse(httpResponse.getEntity().getContent());
            inDoc.getDocumentElement().normalize();

            Element xmlRoot = inDoc.getDocumentElement();
            Element exchangeDocs = XMLUtils.getSingleElement(xmlRoot, "exchange-documents");
            Element exchangeDoc = XMLUtils.getSingleElement(exchangeDocs, "exchange-document");
//
//            results.add(EPOUtils.convertBibliographicData(exchangeDoc, formats, EPODocumentId.ORIGIN));
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (SAXException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return results;
    }

    private List<OMElement> splitToRecords(String recordsSrc) {
        OMXMLParserWrapper records = OMXMLBuilderFactory.createOMBuilder(new StringReader(recordsSrc));
        OMElement element = records.getDocumentElement();
        AXIOMXPath xpath = null;
        try {
            xpath = new AXIOMXPath("ns:entry");
            xpath.addNamespace("ns", "http://www.w3.org/2005/Atom");
            List<OMElement> recordsList = xpath.selectNodes(element);
            return recordsList;
        } catch (JaxenException e) {
            return null;
        }
    }
}
