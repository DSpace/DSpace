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
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
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
 * Service that queries the ZDB (Zeitschriftendatenbank) SRU API to search for
 * and retrieve journal metadata.
 *
 * <p>Uses {@link DSpaceHttpClientFactory} for HTTP requests and parses the XML
 * responses to build {@link ZDBAuthorityValue} objects containing titles, ISSNs,
 * publishers, and alternative titles.</p>
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBService {

    private static Logger log = LogManager.getLogger(ZDBService.class);

    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^\\d{1,7}-[0-9Xx]$");

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Execute an HTTP GET against the given URL, parse the ZDB XML response,
     * and return a list of {@link ZDBAuthorityValue} objects.
     *
     * @param requestURL the fully-qualified ZDB SRU or detail URL
     * @return list of parsed authority values
     * @throws IOException if the HTTP request fails
     */
    private ZDBSearchResult search(String requestURL) throws IOException {

        List<ZDBAuthorityValue> results = new ArrayList<ZDBAuthorityValue>();
        int total = 0;

        try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().build();
             CloseableHttpResponse response = client.execute(new HttpGet(requestURL))) {

            if (response.getStatusLine() == null) {
                throw new IOException("WS call failed: no status line in the ZDB response");
            }

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new IOException("WS call failed: " + statusCode);
            }

            // A HTTP 200 is theoretically possible without a body, so guard the entity before reading it
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new IOException("WS call failed: empty entity in the ZDB response");
            }

            DocumentBuilder builder;
            try {
                DocumentBuilderFactory factory = XMLUtils.getDocumentBuilderFactory();
                builder = factory.newDocumentBuilder();

                Document inDoc = builder.parse(entity.getContent());

                Element xmlRoot = inDoc.getDocumentElement();

                if ("rdf:RDF".equals(xmlRoot.getNodeName())) {
                    // called details endpoint
                    ZDBAuthorityValue zdbItem = getRecord(xmlRoot);

                    results.add(zdbItem);
                    total = results.size();

                } else {
                    // The SRU service reports the grand total of matching records in the
                    // "numberOfRecords" element; use it so pagination reflects the full result set
                    // rather than the size of the single page returned.
                    total = parseNumberOfRecords(xmlRoot);

                    Element recordsElement = XMLUtils.getSingleElement(xmlRoot, "records");

                    if (recordsElement == null) {
                        // No "records" element found: no results to parse, return an empty list
                        log.info("No 'records' element found in the ZDB response for URL: {}", requestURL);
                        return new ZDBSearchResult(results, total);
                    }

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
        }

        return new ZDBSearchResult(results, total);
    }

    /**
     * Read the grand total of matching records from the SRU {@code numberOfRecords} element.
     *
     * @param xmlRoot the document root of the SRU {@code searchRetrieveResponse}
     * @return the reported total, or {@code 0} when the element is missing or not a valid integer
     */
    private int parseNumberOfRecords(Element xmlRoot) {
        String numberOfRecords = XMLUtils.getElementValue(xmlRoot, "numberOfRecords");
        if (StringUtils.isBlank(numberOfRecords)) {
            return 0;
        }
        try {
            return Integer.parseInt(numberOfRecords.trim());
        } catch (NumberFormatException e) {
            log.warn("Unable to parse ZDB 'numberOfRecords' value: {}", numberOfRecords);
            return 0;
        }
    }

    /**
     * Parse a single {@code rdf:RDF} element into a {@link ZDBAuthorityValue},
     * extracting the ZDB ID, titles, publishers, ISSNs, and alternative titles.
     *
     * @param rdfElementRoot the {@code rdf:RDF} element from the ZDB response
     * @return a populated {@link ZDBAuthorityValue}
     */
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

    /**
     * Retrieve a single ZDB record by its identifier.
     *
     * @param id the ZDB record identifier
     * @return the matching {@link AuthorityValue}, or {@code null} if not found
     * @throws IOException if the HTTP request fails
     */
    public AuthorityValue details(String id) throws IOException {

        String baseUrl = configurationService.getProperty("cris.zdb.detail.url");
        if (StringUtils.isEmpty(baseUrl)) {
            throw new IllegalStateException("ZDB detail URL configuration is missing");
        }

        String url = buildDetailsURL(id);
        List<ZDBAuthorityValue> results = search(url).getRecords();
        if (!results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }

    /**
     * Search the ZDB SRU API for journals matching the given title query.
     *
     * <p>Pagination is delegated to the SRU service via the {@code startRecord} and
     * {@code maximumRecords} parameters. SRU positions are 1-based, so the supplied 0-based
     * {@code start} offset is translated to {@code startRecord = start + 1}. The
     * {@code maximumRecords} parameter is only sent when a positive {@code rows} value is
     * requested; otherwise the SRU service applies its own default page size.</p>
     *
     * @param query the title search string (must not be empty)
     * @param start the 0-based offset of the first record to return
     * @param rows  the maximum number of records to return; ignored when not positive
     * @return the search result holding the matching {@link ZDBAuthorityValue} entries for the
     *         requested page and the grand total reported by the SRU service
     * @throws IOException              if the HTTP request fails
     * @throws IllegalArgumentException if the query is empty
     */
    public ZDBSearchResult list(String query, int start, int rows) throws IOException {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("The query must not be empty");
        }

        String baseUrl = configurationService.getProperty("cris.zdb.search.url");
        if (StringUtils.isEmpty(baseUrl)) {
            throw new IllegalStateException("ZDB search URL configuration is missing");
        }

        StringBuilder queryURL = new StringBuilder(baseUrl)
            .append("&query=tit=")
            .append(URLEncoder.encode(query, Charset.defaultCharset()));

        // SRU uses a 1-based startRecord position; DSpace supplies a 0-based offset
        if (start >= 0) {
            queryURL.append("&startRecord=").append(start + 1);
        }
        // SRU maximumRecords bounds the page size; only send it when a positive value is requested,
        // otherwise let the SRU service apply its default page size
        if (rows > 0) {
            queryURL.append("&maximumRecords=").append(rows);
        }

        return search(queryURL.toString());
    }

    /**
     * Search the ZDB SRU API for journals matching the given title query, returning only the parsed
     * records for the requested page.
     *
     * @param query the title search string (must not be empty)
     * @param start the 0-based offset of the first record to return
     * @param rows  the maximum number of records to return; ignored when not positive
     * @return list of matching {@link ZDBAuthorityValue} entries for the requested page
     * @throws IOException              if the HTTP request fails
     * @throws IllegalArgumentException if the query is empty
     */
    public List<ZDBAuthorityValue> listRecords(String query, int start, int rows) throws IOException {
        return list(query, start, rows).getRecords();
    }

    /**
     * Build the detail URL for a specific ZDB record.
     *
     * @param id the ZDB record identifier
     * @return the formatted detail URL
     */
    public String buildDetailsURL(String id) {
        if (id == null || !SAFE_ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid ZDB record id: " + id);
        }
        return MessageFormat.format(configurationService.getProperty("cris.zdb.detail.url"), id);
    }
}