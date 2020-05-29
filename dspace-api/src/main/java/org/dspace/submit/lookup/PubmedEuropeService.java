/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import gr.ekt.bte.core.Record;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.dspace.app.util.XMLUtils;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * @author cineca
 */
public class PubmedEuropeService {

    private static final Logger log = Logger.getLogger(PubmedEuropeService.class);

    private int timeout = 1000;

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Record getByPubmedEuropeID(String pubmedid) throws HttpException,
            IOException, ParserConfigurationException, SAXException {
        List<String> ids = new ArrayList<String>();
        ids.add(pubmedid.trim());
        List<Record> items = getByPubmedEuropeIDs(ids);
        if (items != null && items.size() > 0) {
            return items.get(0);
        }
        return null;
    }

    public List<Record> search(String title, String author, int year)
            throws HttpException, IOException {
        StringBuffer query = new StringBuffer();
        query.append("(");
        if (StringUtils.isNotBlank(title)) {
            query.append("TITLE:").append(title);
            query.append(")");
        }
        if (StringUtils.isNotBlank(author)) {
            String splitRegex = "(\\s*,\\s+|\\s*;\\s+|\\s*;+|\\s*,+|\\s+)";
            String[] authors = author.split(splitRegex);
            // [FAU]
            if (query.length() > 0) {
                query.append(" AND ");
            }
            query.append("(");
            int x = 0;
            for (String auth : authors) {
                x++;
                query.append("AUTH:\"").append(auth).append("\"");
                if (x < authors.length) {
                    query.append(" AND ");
                }
            }
            query.append(")");
        }
        if (year != -1) {
            // [DP]
            if (query.length() > 0) {
                query.append(" AND ");
            }
            query.append("( PUB_YEAR:").append(year).append(")");
        }
        query.append(")");
        return search(query.toString());
    }

    public List<Record> search(String query) throws IOException, HttpException {
        List<Record> PMCEuropeResults = new ArrayList<>();
        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
        String proxyPort = ConfigurationManager.getProperty("http.proxy.port");
        if (!ConfigurationManager.getBooleanProperty(SubmissionLookupService.CFG_MODULE, "remoteservice.demo")) {
            HttpGet method = null;
            HttpHost proxy = null;
            try {
                HttpClient client = new DefaultHttpClient();
                client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

                URIBuilder uriBuilder = new URIBuilder(
                        "https://www.ebi.ac.uk/europepmc/webservices/rest/search");
                uriBuilder.addParameter("format", "xml");
                uriBuilder.addParameter("resulttype", "core");
                uriBuilder.addParameter("pageSize", "1000");
                uriBuilder.addParameter("query", query);

                boolean lastPage = false;
                while (!lastPage) {
                    method = new HttpGet(uriBuilder.build());
                    if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
                        proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                    }
                    // Execute the method.
                    HttpResponse response = client.execute(method);
                    StatusLine statusLine = response.getStatusLine();
                    int statusCode = statusLine.getStatusCode();

                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("WS call failed: "
                                + statusLine);
                    }

                    DocumentBuilderFactory factory = DocumentBuilderFactory
                            .newInstance();
                    factory.setValidating(false);
                    factory.setIgnoringComments(true);
                    factory.setIgnoringElementContentWhitespace(true);

                    DocumentBuilder builder;
                    try {
                        builder = factory.newDocumentBuilder();

                        Document inDoc = builder.parse(response.getEntity().getContent());

                        Element xmlRoot = inDoc.getDocumentElement();
                        Element resList = XMLUtils.getSingleElement(xmlRoot,
                                "resultList");
                        List<Element> res = XMLUtils.getElementList(
                                resList, "result");
                        if (!res.isEmpty()) {
                            PMCEuropeResults.addAll(getByPubmedEuropeResults(res));
                            String cursorMark = XMLUtils.getElementValue(xmlRoot,
                                    "nextCursorMark");
                            if (cursorMark != null && !"*".equals(cursorMark)) {
                                uriBuilder.setParameter("cursorMar", cursorMark);
                            } else {
                                lastPage = true;
                            }
                        } else {
                            lastPage = true;
                        }
                    } catch (ParserConfigurationException e1) {
                        log.error(e1.getMessage(), e1);
                    } catch (SAXException e1) {
                        log.error(e1.getMessage(), e1);
                    }

                }
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
        } else {
            InputStream stream = null;
            try {
                File file = new File(
                        ConfigurationManager.getProperty("dspace.dir")
                                + "/config/crosswalks/demo/pubmedeurope-search.xml");
                stream = new FileInputStream(file);
                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                factory.setValidating(false);
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(true);

                DocumentBuilder builder = factory.newDocumentBuilder();
                Document inDoc = builder.parse(stream);
                Element xmlRoot = inDoc.getDocumentElement();
                Element resList = XMLUtils.getSingleElement(xmlRoot,
                        "resultList");
                List<Element> res = XMLUtils.getElementList(
                        resList, "result");
                PMCEuropeResults.addAll(getByPubmedEuropeResults(res));
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return PMCEuropeResults;
    }

    public List<Record> getByPubmedEuropeResults(List<Element> results)
            throws HttpException, IOException, ParserConfigurationException,
            SAXException {
        List<Record> pubmedEuropeResult = new ArrayList<Record>();

        for (Element xmlArticle : results) {
            Record pubmedItem = null;
            try {
                pubmedItem = PubmedEuropeUtils
                        .convertPubmedEuropeDomToRecord(xmlArticle);
                pubmedEuropeResult.add(pubmedItem);
            } catch (Exception e) {
                throw new RuntimeException(
                        "PubmedID is not valid or not exist: "
                                + e.getMessage(), e);
            }
        }

        return pubmedEuropeResult;
    }

    public List<Record> getByPubmedEuropeIDs(List<String> pubmedIDs) throws IOException, HttpException {

        StringBuffer query = new StringBuffer();
        query.append("(");
        int x = 0;
        for (String pubmedID : pubmedIDs) {
            x++;
            query.append("EXT_ID:").append(pubmedID);
            if (x < pubmedIDs.size()) {
                query.append(" OR ");
            }
        }
        query.append(")");

        return search(query.toString());
    }


    public List<Record> search(String doi, String pmid) throws HttpException,
            IOException {
        StringBuffer query = new StringBuffer();
        if (StringUtils.isNotBlank(doi)) {
            query.append("DOI:");
            query.append(doi);
        }
        if (StringUtils.isNotBlank(pmid)) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            query.append("EXT_ID:").append(pmid);
        }
        return search(query.toString());
    }
}
