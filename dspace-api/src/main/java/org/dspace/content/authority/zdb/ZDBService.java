/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.zdb;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.app.util.XMLUtils;
import org.dspace.authority.AuthorityValue;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBService {

    private static Logger log = LogManager.getLogger(ZDBService.class);

    @Autowired
    private ConfigurationService configurationService;

    private List<ZDBAuthorityValue> search(String requestURL) throws IOException {

        List<ZDBAuthorityValue> results = new ArrayList<ZDBAuthorityValue>();

        HttpGet method = null;
        try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().build()) {

            method = new HttpGet(requestURL);
            // Execute the method.
            HttpResponse response = client.execute(method);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException("WS call failed: " + statusCode);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder();

                Document inDoc = builder.parse(response.getEntity().getContent());

                Element xmlRoot = inDoc.getDocumentElement();

                if ("rdf:RDF".equals(xmlRoot.getNodeName())) {
                    // called details endpoint
                    ZDBAuthorityValue zdbItem = getRecord(xmlRoot);

                    results.add(zdbItem);

                } else {
                    Element recordsElement = XMLUtils.getSingleElement(xmlRoot, "records");

                    // called search endpoint
                    List<Element> recordElement = XMLUtils.getElementList(recordsElement, "record");

                    for (Element element : recordElement) {

                        Element recordDataElement = XMLUtils.getSingleElement(element, "recordData");

                        Element rdfElementRoot = XMLUtils.getSingleElement(recordDataElement, "rdf:RDF");

                        ZDBAuthorityValue zdbItem = getRecord(rdfElementRoot);

                        results.add(zdbItem);
                    }
                }

            } catch (ParserConfigurationException e1) {
                log.error(e1.getMessage(), e1);
            } catch (SAXException e1) {
                log.error(e1.getMessage(), e1);
            }
        } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

        return results;
    }

    private ZDBAuthorityValue getRecord(Element rdfElementRoot) {

        Element rdfDescElementRoot = XMLUtils.getSingleElement(rdfElementRoot, "rdf:Description");

        ZDBAuthorityValue zdbItem = new ZDBAuthorityValue();

        String rdfAboutAttribute = rdfDescElementRoot.getAttribute("rdf:about");
        zdbItem.setServiceId(
                rdfAboutAttribute.substring(rdfAboutAttribute.lastIndexOf("/") + 1, rdfAboutAttribute.length()));
        zdbItem.addOtherMetadata("journalZDBID", zdbItem.getServiceId());

        List<String> titles = XMLUtils.getElementValueList(rdfDescElementRoot, "dc:title");
        int i = 0;
        for (String title : titles) {
            if (i == 0) {
                zdbItem.setValue(title);
            } else {
                zdbItem.addOtherMetadata("journalTitle", title);
            }
            i++;
        }

        List<String> publishers = XMLUtils.getElementValueList(rdfDescElementRoot, "dc:publisher");
        for (String publisher : publishers) {
            zdbItem.addOtherMetadata("journalPublisher", publisher);
        }

        List<String> issns = XMLUtils.getElementValueList(rdfDescElementRoot, "bibo:issn");
        for (String issn : issns) {
            zdbItem.addOtherMetadata("journalIssn", issn);
        }

        List<String> alternativeTitles = XMLUtils.getElementValueList(rdfDescElementRoot, "dcterms:alternative");
        for (String alternativeTitle : alternativeTitles) {
            zdbItem.addOtherMetadata("journalAlternativeTitle", alternativeTitle);
        }
        return zdbItem;
    }

    public AuthorityValue details(String id) throws IOException {

        String url = buildDetailsURL(id);
        List<ZDBAuthorityValue> results = search(url);
        if (!results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }

    public List<ZDBAuthorityValue> list(String query, int page, int pagesize) throws IOException {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("The query must not be empty");
        }

        String queryURL = configurationService.getProperty("cris.zdb.search.url");

        // TODO seems that numberOfRecords is not supported
        // if (pagesize >= 0)
        // {
        // searchURL += "&numberOfRecords=" + Integer.toString(pagesize);
        // }

        queryURL += "&query=tit=" + URLEncoder.encode(query, Charset.defaultCharset());
        return search(queryURL);
    }

    public String buildDetailsURL(String id) {
        return MessageFormat.format(configurationService.getProperty("cris.zdb.detail.url"), id);
    }
}