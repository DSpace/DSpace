/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ekt.bte.core.Record;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class EPOService {
    private static Logger log = Logger.getLogger(EPOService.class);

    private final String endPointAuthService = "https://ops.epo.org/3.2/auth/accesstoken";
    private final String endPointPublisherDataSearchService = "http://ops.epo.org/rest-services/published-data/search";
    private final String endPointPublisherDataRetriveService = "http://ops.epo.org/rest-services/published-data/publication/$(doctype)/$(id)/biblio";

    // max retrival data is set to 1000 by EPO
    private final int EPO_LIMIT = 1000;
    private final int START = 1;
    private final int SIZE = 25;
    private final int MAX = EPO_LIMIT / SIZE + 1;

    // retrieve formats
    private final String[] formats = new String[] { EPODocumentId.EPODOC, EPODocumentId.DOCDB, EPODocumentId.ORIGIN };

    public List<Record> search(String query, String consumerKey, String consumerSecretKey)
            throws HttpException, IOException {
        List<Record> records = new ArrayList<Record>();

        if (StringUtils.isNotBlank(consumerKey) && StringUtils.isNotBlank(consumerSecretKey)) {

            String bearer = login(consumerKey, consumerSecretKey);
            if (StringUtils.isNotBlank(query) && StringUtils.isNotBlank(bearer)) {
                int start = START;
                int end = SIZE;
                List<EPODocumentId> epoDocIds = new ArrayList<EPODocumentId>();

                for (int i = 0; i < MAX; i++) {
                    List<EPODocumentId> ids = searchDocumentIds(bearer, query, start, end);

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
                    List<Record> recordfounds = searchDocument(bearer, epoDocId);

                    if (recordfounds.size() > 1) {
                        log.warn("More record are returned with epocID " + epoDocId.toString());
                    }
                    records.addAll(recordfounds);
                }
            }

        }
        return records;
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
    private String login(String consumerKey, String consumerSecretKey) throws IOException, HttpException {
        HttpPost method = null;
        String accessToken = null;
        try {
            // open session
            HttpClient client = HttpClientBuilder.create().build();
            method = new HttpPost(endPointAuthService);

            String authString = consumerKey + ":" + consumerSecretKey;
            method.setHeader("Authorization", "Basic " + Base64.encode(authString.getBytes()));
            method.setHeader("Content-type", "application/x-www-form-urlencoded");

            StringEntity entity = new StringEntity("grant_type=client_credentials");
            method.setEntity(entity);

            // Execute the method.
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

    private void logout(String sid) {
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
        } finally {
            logout(bearer);
        }

        return results;
    }

    private List<Record> searchDocument(String bearer, EPODocumentId epoDocId) {
        List<Record> results = new ArrayList<Record>();

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

            results.add(EPOUtils.convertBibliographicData(exchangeDoc, formats, EPODocumentId.ORIGIN));
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (SAXException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            logout(bearer);
        }
        return results;
    }
}
