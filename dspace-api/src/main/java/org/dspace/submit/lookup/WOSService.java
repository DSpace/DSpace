/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.util.Base64;
import org.dspace.app.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WOSService {
    private static Logger log = Logger.getLogger(WOSService.class);

    private final String SEARCH_HEAD_BY_AFFILIATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope " +
        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wok=\"http://woksearch.v3.wokmws" +
        ".thomsonreuters.com\"><soapenv:Header/><soapenv:Body><wok:search><queryParameters><databaseId>WOK" +
        "</databaseId>";
    private final String SEARCH_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope " +
        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wok=\"http://woksearch.v3.wokmws" +
        ".thomsonreuters.com\"><soapenv:Header/><soapenv:Body><wok:search><queryParameters><databaseId>WOK" +
        "</databaseId>";
    private final String RETRIEVEBYID_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope " +
        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wok=\"http://woksearch.v3.wokmws" +
        ".thomsonreuters.com\"><soapenv:Header/><soapenv:Body><wok:retrieveById><databaseId>WOK</databaseId>";

    private final String SEARCH_END_BY_AFFILIATION =
        "<queryLanguage>en</queryLanguage></queryParameters><retrieveParameters><firstRecord>1</firstRecord><count" +
            ">100</count></retrieveParameters></wok:search></soapenv:Body></soapenv:Envelope>";
    private final String SEARCH_END = "<queryLanguage>en</queryLanguage></queryParameters><retrieveParameters" +
        "><firstRecord>1</firstRecord><count>100</count></retrieveParameters></wok:search></soapenv:Body></soapenv" +
        ":Envelope>";
    private final String RETRIEVEBYID_END = "<queryLanguage>en</queryLanguage><retrieveParameters><firstRecord>1" +
        "</firstRecord><count>100</count></retrieveParameters></wok:retrieveById></soapenv:Body></soapenv:Envelope>";

    private final String AUTH_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope " +
        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www" +
        ".w3.org/2001/XMLSchema-instance\" xmlns:ns=\"http://auth.cxf.wokmws.thomsonreuters.com\">"
        + "<soapenv:Header></soapenv:Header><soapenv:Body><ns:authenticate/></soapenv:Body></soapenv:Envelope>";
    private final String CLOSE_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope " +
        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:auth=\"http://auth.cxf.wokmws" +
        ".thomsonreuters.com\"><soapenv:Header/><soapenv:Body>"
        + "<auth:closeSession/></soapenv:Body></soapenv:Envelope>";

    private final String endPointAuthService = "http://search.webofknowledge.com/esti/wokmws/ws/WOKMWSAuthenticate";
    //private final String endPointAuthService = "http://localhost:9998/esti/wokmws/ws/WOKMWSAuthenticate";
    private final String endPointSearchService = "http://search.webofknowledge.com/esti/wokmws/ws/WokSearch";
    //private final String endPointSearchService = "http://localhost:9998/esti/wokmws/ws/WokSearch";

    public List<Record> search(String doi, String title, String author, int year, String username, String password,
                               boolean ipAuth)
        throws HttpException,
        IOException {
        StringBuffer query = new StringBuffer("<userQuery>");
        query.append(buildQueryPart(doi, title, author, year));
        query.append("</userQuery>");
        String message = SEARCH_HEAD + query.toString() + SEARCH_END;
        return internalSearch(message, username, password, ipAuth);
    }

    public List<Record> search(Set<String> dois, String username, String password, boolean ipAuth)
        throws HttpException, IOException {
        if (dois != null && dois.size() > 0) {
            StringBuffer query = new StringBuffer("<userQuery>");
            for (String doi : dois) {
                if (query.length() > "<userQuery>".length()) {
                    query.append(" OR ");
                }
                query.append(buildQueryPart(doi, null, null, -1));
            }
            query.append("</userQuery>");
            String message = SEARCH_HEAD + query.toString() + SEARCH_END;
            return internalSearch(message, username, password, ipAuth);
        }
        return null;
    }

    public Record retrieve(String wosid, String username, String password, boolean ipAuth) throws IOException {
        Record result = null;
        if (StringUtils.isNotBlank(wosid)) {
            String message = RETRIEVEBYID_HEAD + "<uid>" + wosid + "</uid>" + RETRIEVEBYID_END;
            List<Record> ret = internalSearch(message, username, password, ipAuth);
            if (ret.size() > 0) {
                result = ret.get(0);
            }
        }
        return result;
    }

    public StringBuffer buildQueryPart(String doi, String title, String author, int year) {
        StringBuffer query = new StringBuffer("");
        if (StringUtils.isNotBlank(doi)) {
            query.append("DO=(");
            query.append(doi);
            query.append(")");
        }
        if ((StringUtils.isNotBlank(title)) && (query.length() > 0)) {
            query.append(" AND ");
        }
        if (StringUtils.isNotBlank(title)) {
            query.append("TI=(\"");
            query.append(title);
            query.append("\")");
        }
        if ((StringUtils.isNotBlank(author)) && (query.length() > 0)) {
            query.append(" AND ");
        }
        if (StringUtils.isNotBlank(author)) {
            query.append("AU=(\"");
            query.append(author);
            query.append("\")");
        }
        if ((year != -1) && (query.length() > 0)) {
            query.append(" AND ");
        }
        if (year != -1) {
            query.append("PY=(");
            query.append(year);
            query.append(")");
        }
        return query;
    }

    private String login(String username, String password, boolean ipAuth) throws IOException, HttpException {
        String ret = null;
        HttpPost method = null;
        try {
            // open session
            HttpClient client = HttpClientBuilder.create().build();
            method = new HttpPost(endPointAuthService);

            if (!ipAuth) {
                String authString = username + ":" + password;
                method.setHeader("Authorization", "Basic " + Base64.encode(authString.getBytes()));
            }

            StringEntity re = new StringEntity(AUTH_MESSAGE, "text/xml", "UTF-8");
            method.setEntity(re);

            // Execute the method.
            HttpResponse httpResponse = client.execute(method);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException("Chiamata al webservice fallita: " + httpResponse.getStatusLine());
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder();

                InputStream responseBodyAsStream = httpResponse.getEntity().getContent();
                Document inDoc = builder.parse(responseBodyAsStream);

                Element xmlRoot = inDoc.getDocumentElement();
                Element soapBody = XMLUtils.getSingleElement(xmlRoot, "soap:Body");

                Element response = XMLUtils.getSingleElement(soapBody, "ns2:authenticateResponse");
                Element sidTag = XMLUtils.getSingleElement(response, "return");
                ret = sidTag.getTextContent();
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
        return ret;
    }

    private void logout(String sid) throws IOException, HttpException {
        if (StringUtils.isNotBlank(sid)) {
            // close session
            HttpPost method = null;
            try {
                HttpClient client = HttpClientBuilder.create().build();
                method = new HttpPost(endPointAuthService);
                method.setHeader("Cookie", "SID=" + sid);
                StringEntity re = new StringEntity(CLOSE_MESSAGE, "text/xml", "UTF-8");
                method.setEntity(re);

                HttpResponse httpResponse = client.execute(method);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException("Chiamata al webservice fallita: " + httpResponse.getStatusLine());
                }
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
        }
    }

    private List<Record> internalSearch(String message, String username, String password, boolean ipAuth) {
        List<Record> results = new ArrayList<Record>();
        if (!ipAuth && (StringUtils.isBlank(username) || (StringUtils.isBlank(password)))) {
            return results;
        }
        try {
            String sid = login(username, password, ipAuth);
            if (StringUtils.isNotBlank(sid)) {
                try {
                    HttpClient client = HttpClientBuilder.create().build();
                    HttpPost method = new HttpPost(endPointSearchService);
                    method.setHeader("Cookie", "SID=" + sid);
                    StringEntity re = new StringEntity(message, "text/xml", "UTF-8");
                    method.setEntity(re);
                    HttpResponse httpResponse = client.execute(method);
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        throw new RuntimeException("Chiamata al webservice fallita: " + httpResponse.getStatusLine());
                    }
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document inDoc = builder.parse(httpResponse.getEntity().getContent());
                    Element xmlRoot = inDoc.getDocumentElement();
                    Element tmp = XMLUtils.getSingleElement(xmlRoot, "soap:Body");
                    if (message.indexOf("<userQuery>") > 0) {
                        tmp = XMLUtils.getSingleElement(tmp, "ns2:searchResponse");
                    } else {
                        tmp = XMLUtils.getSingleElement(tmp, "ns2:retrieveByIdResponse");
                    }
                    tmp = XMLUtils.getSingleElement(tmp, "return");
                    String recordsFound = XMLUtils.getElementValue(tmp, "recordsFound");
                    if (!"0".equals(recordsFound)) {
                        Element records = XMLUtils.getSingleElement(tmp, "records");
                        Document newDoc = builder.parse(new InputSource(new ByteArrayInputStream(records
                                                                                                     .getTextContent()
                                                                                                     .getBytes(
                                                                                                         "UTF-8"))));
                        Element recordsElement = newDoc.getDocumentElement();
                        List<Element> recList = XMLUtils.getElementList(recordsElement, "REC");
                        for (int i = 0; i < recList.size(); i++) {
                            Element rec = recList.get(i);
                            results.add(WOSUtils.convertWosDomToRecord(rec));
                        }
                    }
                } catch (ParserConfigurationException e) {
                    log.error(e.getMessage(), e);
                } catch (SAXException e) {
                    log.error(e.getMessage(), e);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    logout(sid);
                }
            }
        } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
        }
        return results;
    }

    //TODO databaseID not used
    public List<Record> searchByAffiliation(String userQuery, String databaseID, String symbolicTimeSpan, String start,
                                            String end,
                                            String username, String password, boolean ipAuth)
        throws HttpException, IOException {
        StringBuffer query = new StringBuffer("<userQuery>");
        query.append(userQuery);
        query.append("</userQuery>");
        if (StringUtils.isNotBlank(symbolicTimeSpan)) {
            query.append("<symbolicTimeSpan>");
            query.append(symbolicTimeSpan);
            query.append("</symbolicTimeSpan>");
        } else {
            query.append("<timeSpan>");
            query.append("<begin>");
            query.append(start);
            query.append("</begin>");
            query.append("<end>");
            query.append(end);
            query.append("</end>");
            query.append("</timeSpan>");
        }
        String message = SEARCH_HEAD_BY_AFFILIATION + query.toString() + SEARCH_END_BY_AFFILIATION;
        return internalSearch(message, username, password, ipAuth);
    }
}
