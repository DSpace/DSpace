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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import gr.ekt.bte.core.Record;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.dspace.app.util.XMLUtils;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class SciValService {

    private static Logger log = Logger.getLogger(SciValService.class);

    private final String REQUEST_HEAD_SINGLE = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soapenv:Envelope " +
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:msap=\"sais.scivalcontent.comm\" " +
        "xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">"
        + "<soapenv:Header/><soapenv:Body><msap:match soapenv:encodingStyle=\"http://schemas.xmlsoap" +
        ".org/soap/encoding/\"><matchrequest xsi:type=\"msap:MatchRequest\"><requestmetadata " +
        "soapenc:arrayType=\"msap:RequestMetadata[1]\" xsi:type=\"msap:RequestMetadataArray\">";
    private final String REQUEST_HEAD_MULTI = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soapenv:Envelope " +
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:msap=\"sais.scivalcontent.com\" " +
        "xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">"
        + "<soapenv:Header/><soapenv:Body><msap:match soapenv:encodingStyle=\"http://schemas.xmlsoap" +
        ".org/soap/encoding/\"><matchrequest xsi:type=\"msap:MatchRequest\">"
        + "<requestmetadata soapenc:arrayType=\"msap:RequestMetadata[PLACE-HOLDER-MULTI]\" " +
        "xsi:type=\"msap:RequestMetadataArray\">";
    private final String REQUEST_END = "</requestmetadata><maxhits xsi:type=\"xsd:int\">10</maxhits><clientKey " +
        "xsi:type=\"xsd:string\">PLACE-HOLDER_CLIENT_KEY</clientKey></matchrequest></msap:match></soapenv:Body" +
        "></soapenv:Envelope>";

    private String endPoint = "http://sais.scivalcontent.com/";
    private String retrieveBaseURL = "http://sais.scivalcontent.com/REST/?";

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public List<Record> search(String doi, String title, String author, int year, String clientKey)
        throws HttpException, IOException {
        StringBuffer query = buildQueryPart(doi, title, author, year);
        return search(query.toString(), 1, clientKey);
    }

    public List<Record> search(Set<String> dois, String clientKey)
        throws HttpException, IOException {
        if (dois != null && dois.size() > 0) {
            StringBuffer query = new StringBuffer();
            for (String doi : dois) {
                query.append(buildQueryPart(doi, null, null, -1));
            }
            return search(query.toString(), dois.size(), clientKey);
        }
        return null;
    }

    public StringBuffer buildQueryPart(String doi, String title, String author,
                                       int year) {
        StringBuffer query = new StringBuffer(" <item xsi:type=\"ns1:RequestMetadata\">");
        if (StringUtils.isNotBlank(doi)) {
            query.append("<doi xsi:type=\"xsd:string\">");
            query.append(doi);
            query.append("</doi>");
        }
        if (StringUtils.isNotBlank(title)) {
            query.append("<articleTitle xsi:type=\"xsd:string\">");
            query.append(title);
            query.append("</articleTitle>");
        }
        if (StringUtils.isNotBlank(author)) {
            query.append("<namedAuthor xsi:type=\"xsd:string\">");
            query.append(author);
            query.append("</namedAuthor>");
        }
        if (year != -1) {
            query.append("<year xsi:type=\"xsd:int\">");
            query.append(year);
            query.append("</year>");
        }
        query.append("</item>");
        return query;
    }

    public List<Record> search(String query, int count, String clientKey) throws IOException,
        HttpException {

        List<Record> results = new ArrayList<Record>();
        if (!clientKey.equals("") && clientKey != null) {
            if (!ConfigurationManager.getBooleanProperty("remoteservice.demo")) {
                HttpPost method = null;
                try {
                    HttpClient client = HttpClientBuilder.create().build();
                    method = new HttpPost(
                        endPoint);
                    method.addHeader("SOAPAction", endPoint + "?method=match");

                    String requestHead = count > 1 ? REQUEST_HEAD_MULTI
                        .replace("PLACE-HOLDER-MULTI", String.valueOf(count)) : REQUEST_HEAD_SINGLE;
                    String raw_body = requestHead + query + REQUEST_END.replace("PLACE-HOLDER_CLIENT_KEY", clientKey);

                    StringEntity re = new StringEntity(raw_body, "text/xml", "UTF-8");
                    method.setEntity(re);
                    // Execute the method.
                    HttpResponse httpResponse = client.execute(method);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();

                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException(
                            "Chiamata al webservice fallita: "
                                + httpResponse.getStatusLine());
                    }

                    DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                    factory.setValidating(false);
                    factory.setIgnoringComments(true);
                    factory.setIgnoringElementContentWhitespace(true);

                    DocumentBuilder builder;
                    try {
                        builder = factory.newDocumentBuilder();

                        InputStream responseBodyAsStream = httpResponse.getEntity().getContent();
                        Document inDoc = builder.parse(responseBodyAsStream);

                        Element xmlRoot = inDoc.getDocumentElement();
                        Element soapBody = XMLUtils
                            .getSingleElement(xmlRoot, "SOAP-ENV:Body");

                        Element matchResponse = XMLUtils.getSingleElement(soapBody, "SOAP-ENV:matchResponse");
                        Element matchReturn = XMLUtils.getSingleElement(matchResponse, "matchReturn");
                        List<Element> matchItems = XMLUtils.getElementList(matchReturn, "item");
                        for (Element matchItem : matchItems) {
                            Element resultmetadata = XMLUtils.getSingleElement(matchItem, "resultmetadata");
                            if (resultmetadata == null) {
                                continue;
                            }
                            List<Element> items = XMLUtils.getElementList(resultmetadata, "item");
                            int tot = items.size();
                            for (int i = 0; i < tot; i++) {
                                Record scopusItem = SciValUtils.convertScopusDomToRecord(items.get(i));
                                results.add(scopusItem);
                            }
                        }
                    } catch (ParserConfigurationException e) {
                        log.error(e.getMessage(), e);
                    } catch (SAXException e1) {
                        log.error(e1.getMessage(), e1);
                    }
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
                            + "/config/crosswalks/demo/scopus.xml");
                    stream = new FileInputStream(file);
                    DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                    factory.setValidating(false);
                    factory.setIgnoringComments(true);
                    factory.setIgnoringElementContentWhitespace(true);

                    DocumentBuilder builder;
                    builder = factory.newDocumentBuilder();

                    Document inDoc = builder.parse(stream);

                    Element xmlRoot = inDoc.getDocumentElement();
                    Element soapBody = XMLUtils
                        .getSingleElement(xmlRoot,
                                          "SOAP-ENV:Body");

                    Element matchResponse = XMLUtils.getSingleElement(soapBody,
                                                                      "SOAP-ENV:matchResponse");
                    Element matchReturn = XMLUtils.getSingleElement(matchResponse, "matchReturn");
                    Element item = XMLUtils.getSingleElement(matchReturn, "item");
                    Element resultmetadata = XMLUtils.getSingleElement(item, "resultmetadata");
                    List<Element> items = XMLUtils.getElementList(resultmetadata, "item");
                    int tot = items.size();
                    for (int i = 0; i < tot; i++) {
                        Record scopusItem = SciValUtils.convertScopusDomToRecord(items.get(i));
                        results.add(scopusItem);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * @throws IOException *
     */
    public Record retrieve(String eid, String clientKey) throws IOException {
        Record fsi = null;
        if (!ConfigurationManager.getBooleanProperty("remoteservice.demo")) {
            String retrieveEndPoint = retrieveBaseURL + "clientKey=" + clientKey + "&retrieve=" + eid;
            if (StringUtils.isBlank(clientKey)) {
                return fsi;
            }
            try {
                HttpGet method = new HttpGet(retrieveEndPoint);
                HttpClient client = HttpClientBuilder.create().build();
                HttpResponse httpResponse = client.execute(method);

                InputStream responseStream = httpResponse.getEntity().getContent();
                if (responseStream.toString().startsWith("{\"error\"")) {
                    return null;
                }

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder;
                try {
                    builder = factory.newDocumentBuilder();
                    Document inDoc = builder.parse(responseStream);
                    Element xmlRoot = inDoc.getDocumentElement();

                    fsi = SciValUtils.convertFullScopusDomToRecord(xmlRoot);

                } catch (ParserConfigurationException e) {

                    log.error(e.getMessage(), e);
                } catch (SAXException e) {

                    log.error(e.getMessage(), e);
                }
            } catch (MalformedURLException e1) {

                log.error(e1.getMessage(), e1);
            } catch (IOException e) {

                log.error(e.getMessage(), e);
            }
        } else {
            InputStream responseStream = null;
            File file = new File(ConfigurationManager.getProperty("dspace.dir")
                                     + "/config/crosswalks/demo/scopus-full.xml");
            responseStream = new FileInputStream(file);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder();
                Document inDoc = builder.parse(responseStream);
                Element xmlRoot = inDoc.getDocumentElement();

                fsi = SciValUtils.convertFullScopusDomToRecord(xmlRoot);

            } catch (ParserConfigurationException e) {

                log.error(e.getMessage(), e);
            } catch (SAXException e) {

                log.error(e.getMessage(), e);
            }
        }

        return fsi;

    }

}
