/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.zdb;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
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

    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[\\w\\-.]+$");

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Validate that the given URL uses HTTPS and does not resolve to a
     * private or reserved IP address. This mitigates SSRF attacks.
     *
     * @param url the URL to validate
     * @throws IOException if the URL is invalid, not HTTPS, or points to a
     *                     private/reserved IP address
     */
    private void validateUrl(String url) throws IOException {
        String searchBase = configurationService.getProperty("cris.zdb.search.url");
        String detailBase = configurationService.getProperty("cris.zdb.detail.url");

        // Clean up base URLs for prefix checking if they contain formatters or query parameters
        String safeSearchPrefix = searchBase != null && searchBase.contains("?")
            ? searchBase.substring(0, searchBase.indexOf("?")) : searchBase;
        String safeDetailPrefix = detailBase != null && detailBase.contains("{0}")
            ? detailBase.substring(0, detailBase.indexOf("{0}")) : detailBase;

        boolean isAllowedPrefix = (safeSearchPrefix != null && url.startsWith(safeSearchPrefix)) ||
            (safeDetailPrefix != null && url.startsWith(safeDetailPrefix));

        if (!isAllowedPrefix) {
            throw new IOException("URL is not targeting an allowed ZDB endpoint prefix.");
        }

        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol();
            if (!"https".equalsIgnoreCase(protocol)) {
                throw new IOException("Only HTTPS URLs are allowed");
            }

            String host = parsedUrl.getHost();
            if (host == null || host.isEmpty()) {
                throw new IOException("URL must have a host");
            }
            InetAddress address = InetAddress.getByName(host);
            if (address.isSiteLocalAddress() || address.isLoopbackAddress()
                || address.isAnyLocalAddress() || address.isLinkLocalAddress()) {
                throw new IOException("URL points to a private or reserved IP address");
            }
        } catch (MalformedURLException e) {
            throw new IOException("Invalid URL: " + url, e);
        } catch (UnknownHostException e) {
            throw new IOException("Cannot resolve host in URL: " + url, e);
        }
    }

    /**
     * Execute an HTTP GET against the given URL, parse the ZDB XML response,
     * and return a list of {@link ZDBAuthorityValue} objects.
     *
     * @param requestURL the fully-qualified ZDB SRU or detail URL
     * @return list of parsed authority values
     * @throws IOException if the HTTP request fails
     */
    private List<ZDBAuthorityValue> search(String requestURL) throws IOException {

        List<ZDBAuthorityValue> results = new ArrayList<ZDBAuthorityValue>();
        validateUrl(requestURL);

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
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
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
        List<ZDBAuthorityValue> results = search(url);
        if (!results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }

    /**
     * Search the ZDB SRU API for journals matching the given title query.
     *
     * @param query    the title search string (must not be empty)
     * @param page     the page number (currently unused by ZDB)
     * @param pagesize the desired page size (currently unused by ZDB)
     * @return list of matching {@link ZDBAuthorityValue} entries
     * @throws IOException              if the HTTP request fails
     * @throws IllegalArgumentException if the query is empty
     */
    public List<ZDBAuthorityValue> list(String query, int page, int pagesize) throws IOException {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("The query must not be empty");
        }

        String baseUrl = configurationService.getProperty("cris.zdb.search.url");
        if (StringUtils.isEmpty(baseUrl)) {
            throw new IllegalStateException("ZDB search URL configuration is missing");
        }

        String queryURL = baseUrl + "&query=tit=" + URLEncoder.encode(query, Charset.defaultCharset());
        return search(queryURL);
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