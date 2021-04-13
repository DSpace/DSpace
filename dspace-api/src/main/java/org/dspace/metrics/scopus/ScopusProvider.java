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
import java.util.List;
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

    private static Logger log = LogManager.getLogger(ScopusProvider.class);

    @Autowired
    private ScopusRestConnector scopusRestConnector;

    public CrisMetricDTO getScopusObject(String id) {
        String is = getRecords(id);
        if (StringUtils.isNotBlank(is)) {
            return convertToScopusDTO(is);
        }
        log.error("The query : " + id + " is wrong!");
        return null;
    }

    private String getRecords(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return scopusRestConnector.get(id);
    }

    private CrisMetricDTO convertToScopusDTO(String inputStream) {
        Document doc = null;
        DocumentBuilder docBuilder = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(new InputSource(new StringReader(inputStream)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error(e.getMessage(), e);
        }
        return loadScopusMetrics(doc);
    }

    private CrisMetricDTO loadScopusMetrics(Document doc) {
        CrisMetricDTO scopusCitation = null;
        try {
            Element xmlRoot = doc.getDocumentElement();
            Element dataRoot = XMLUtils.getSingleElement(xmlRoot, "entry");
            Element errorScopusResp = XMLUtils.getSingleElement(dataRoot, "error");
            if (dataRoot != null && errorScopusResp == null) {
                scopusCitation = new CrisMetricDTO();
                String eid = XMLUtils.getElementValue(dataRoot, "eid");
                String doi = XMLUtils.getElementValue(dataRoot, "prism:doi");
                String pmid = XMLUtils.getElementValue(dataRoot, "pubmed-id");
                String numCitations = XMLUtils.getElementValue(dataRoot, "citedby-count");
                List<Element> citedByLinkElements = XMLUtils.getElementList(dataRoot, "link");
                for (Element element : citedByLinkElements) {
                    if (element.hasAttribute("ref")) {
                        if ("scopus-citedby".equals(element.getAttribute("ref"))) {
                            scopusCitation.getTmpRemark().put("link", element.getAttribute("href"));
                            break;
                        }
                    }
                }
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
                    scopusCitation.setMetricCount(Double.parseDouble(numCitations));
                } catch (NullPointerException ex) {
                    log.error("Error while trying to parse numCitations:" + numCitations);
                    throw new Exception(ex);
                }
                scopusCitation.setRemark(scopusCitation.buildMetricsRemark());
                return scopusCitation;
            } else {
                if (dataRoot == null) {
                    log.debug("No citation entry found in Scopus");
                } else {
                    log.debug("Error citation entry found in Scopus: " + errorScopusResp.getTextContent());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return scopusCitation;
    }
}