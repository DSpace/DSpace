/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.scopus;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.XMLUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is the implementation of the ExternalDataProvider interface that
 * will deal with the SCOPUS External Data lookup
 * 
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class ScopusProvider {

    private static final Logger log = LogManager.getLogger(ScopusProvider.class);

    private List<String> logsCache = new ArrayList<>();

    @Autowired
    private ScopusRestConnector scopusRestConnector;

    public List<String> getLogs() {
        return logsCache;
    }

    /**
     * <p>
     *  This methods fetch a list of metrics using the {@code id} param,
     *  once fetched and parsed the response explores for linked urls
     *  to fetch and adds all of them to the list.
     * </p>
     * 
     * @param id Appender to the URL defined in the props
     * @return List of CrisMetrics fetched
     */
    public List<CrisMetricDTO> getScopusList(String id) {
        logsCache = new ArrayList<>();
        String scopusResponse = getRecords(id);
        if (StringUtils.isNotBlank(scopusResponse)) {
            List<CrisMetricDTO> crisMetricList = mapToCrisMetricList(scopusResponse);
            String nextItemUrl = this.getNext(scopusResponse);
            // explore all linked items with next clause
            while (crisMetricList != null && !crisMetricList.isEmpty() && StringUtils.isNotEmpty(nextItemUrl)) {
                scopusResponse = this.scopusRestConnector.getNextItem(nextItemUrl);
                crisMetricList.addAll(mapToCrisMetricList(scopusResponse));
                nextItemUrl = this.getNext(scopusResponse);
            }
            return crisMetricList;
        }
        logAndCache("The query : " + id + " is wrong!");
        return List.of();
    }

    public CrisMetricDTO getScopusObject(String id) {
        String scopusResponse = getRecords(id);
        if (StringUtils.isNotBlank(scopusResponse)) {
            return mapToCrisMetric(scopusResponse);
        }
        logAndCache("The query : " + id + " is wrong!");
        return null;
    }

    private String getRecords(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return scopusRestConnector.get(id);
    }

    private CrisMetricDTO mapToCrisMetric(String scopusResponse) {
        Document parsedResponse = null;
        DocumentBuilder docBuilder = null;
        try {
            DocumentBuilderFactory docBuilderFactory = XMLUtils.getDocumentBuilderFactory();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            parsedResponse = docBuilder.parse(new InputSource(new StringReader(scopusResponse)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logAndCacheError(e);
        }
        return mapToCrisMetric(parsedResponse);
    }

    private List<CrisMetricDTO> mapToCrisMetricList(String scopusResponse) {
        Document parsedResponse = null;
        DocumentBuilder docBuilder = null;
        try {
            DocumentBuilderFactory docBuilderFactory = XMLUtils.getDocumentBuilderFactory();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            parsedResponse = docBuilder.parse(new InputSource(new StringReader(scopusResponse)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logAndCacheError(e);
        }
        return mapToCrisMetricList(parsedResponse);
    }

    private String getNext(String scopusResponse) {
        Document parsedResponse = null;
        DocumentBuilder docBuilder = null;
        String nextUrl = null;
        try {
            DocumentBuilderFactory docBuilderFactory = XMLUtils.getDocumentBuilderFactory();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            parsedResponse = docBuilder.parse(new InputSource(new StringReader(scopusResponse)));
            nextUrl = Optional.ofNullable(
                    XMLUtils.getElementList(parsedResponse.getDocumentElement(), "link")
                )
                .map(List::stream)
                .orElse(Stream.empty())
                .filter(element ->
                    element.hasAttribute("ref") &&
                    "next".equals(element.getAttribute("ref")) &&
                    element.hasAttribute("ref")
                )
                .findFirst()
                .map(element -> element.getAttribute("href"))
                .orElse(null);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logAndCacheError(e);
        }
        return nextUrl;
    }

    private List<CrisMetricDTO> mapToCrisMetricList(Document doc) {
        List<CrisMetricDTO> scopusCitationList = new ArrayList<>();
        try {
            scopusCitationList = XMLUtils.getElementList(doc.getDocumentElement(), "entry")
                    .stream()
                    .map(this::mapToCrisMetric)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logAndCacheError(e);
        }
        return scopusCitationList;
    }

    private CrisMetricDTO mapToCrisMetric(Document doc) {
        CrisMetricDTO scopusCitation = null;
        try {
            scopusCitation = Optional.ofNullable(
                        XMLUtils.getSingleElement(doc.getDocumentElement(), "entry")
                    )
                    .map(this::mapToCrisMetric)
                    .orElse(null);
        } catch (Exception e) {
            logAndCacheError(e);
        }
        return scopusCitation;
    }

    private CrisMetricDTO mapToCrisMetric(Element dataRoot) {
        CrisMetricDTO scopusCitation = new CrisMetricDTO();
        if (dataRoot == null) {
            logAndCache("No citation entry found in Scopus");
            return scopusCitation;
        }

        Element errorScopusResp = XMLUtils.getSingleElement(dataRoot, "error");
        if (errorScopusResp != null) {
            logAndCache("Error citation entry found in Scopus: " + errorScopusResp.getTextContent());
            return scopusCitation;
        }

        String eid = XMLUtils.getElementValue(dataRoot, "eid");
        String doi = XMLUtils.getElementValue(dataRoot, "prism:doi");
        String pmid = XMLUtils.getElementValue(dataRoot, "pubmed-id");
        String numCitations = XMLUtils.getElementValue(dataRoot, "citedby-count");

        XMLUtils.getElementList(dataRoot, "link")
            .stream()
            .filter(element -> element.hasAttribute("ref") && "scopus-citedby".equals(element.getAttribute("ref")))
            .findFirst()
            .ifPresent(element -> scopusCitation.getTmpRemark().put("link", element.getAttribute("href")));

        if (StringUtils.isNotBlank(eid)) {
            scopusCitation.getTmpRemark().put("identifier", eid);
        }
        if (StringUtils.isNotBlank(doi)) {
            scopusCitation.getTmpRemark().put("doi", doi);
        }
        if (StringUtils.isNotBlank(pmid)) {
            scopusCitation.getTmpRemark().put("pmid", pmid);
        }
        try {
            scopusCitation.setMetricCount(Double.valueOf(numCitations));
        } catch (NullPointerException | NumberFormatException ex) {
            logAndCacheErrorWithMessage("Error while trying to parse numCitations:" + numCitations, ex);
        }
        scopusCitation.setRemark(scopusCitation.buildMetricsRemark());
        return scopusCitation;
    }

    private void logAndCache(String message) {
        logsCache.add("INFO: " + message);
        log.debug(message);
    }

    private void logAndCacheErrorWithMessage(String message, Throwable e) {
        logsCache.add("ERROR: " + message + '\n' + e.getMessage());
        log.error(message, e);
    }

    private void logAndCacheError(Throwable e) {
        logsCache.add("ERROR: " + e.getMessage());
        log.error(e.getMessage(), e);
    }

}