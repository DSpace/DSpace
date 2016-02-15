/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.wos.dto;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.util.XMLUtils;
import org.dspace.core.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WosResponse {

    /** log4j logger */
    private static Logger log = Logger.getLogger(WosResponse.class);
    
	private boolean error = false;

	private List<CrisMetrics> wosCitations = new ArrayList<CrisMetrics>();

	public WosResponse(String response, String label) {
		this.error = true;
		log.error(label + ":" +response);
	}

	public WosResponse(InputStream xmlData) {
		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(true);

			DocumentBuilder db = factory.newDocumentBuilder();
			Document inDoc = db.parse(xmlData);

			Element xmlRoot = inDoc.getDocumentElement();
			Element fnElement = XMLUtils.getSingleElement(xmlRoot, "fn");
			Element mapElement = XMLUtils.getSingleElement(fnElement, "map");

			List<Element> mapElements = XMLUtils.getElementList(mapElement, "map");
			for (Element element : mapElements) {
				if (element.hasAttribute("name")) {
				    CrisMetrics wosCitation = new CrisMetrics();
					String itemIdElementValue = element.getAttribute("name");
					wosCitation.setResourceId(Integer.parseInt(itemIdElementValue));
					wosCitation.setResourceTypeId(Constants.ITEM);
					Element lastMapElement = XMLUtils.getSingleElement(element, "map");
					
					List<Element> valElements = XMLUtils.getElementList(lastMapElement, "val");
					for (Element valElement : valElements) {
						if (valElement.hasAttribute("name")) {
							if ("ut".equalsIgnoreCase(valElement.getAttribute("name"))) {
							    wosCitation.getTmpRemark().put("identifier", valElement.getTextContent());
							}
							if ("citingArticlesURL".equalsIgnoreCase(valElement.getAttribute("name"))) {
								wosCitation.getTmpRemark().put("link", valElement.getTextContent());
							}
							if ("timesCited".equalsIgnoreCase(valElement.getAttribute("name"))) {								
					            try {
					                wosCitation.setMetricCount(Double.parseDouble(valElement.getTextContent()));
					            }
					            catch(NullPointerException ex) {
					                log.error("try to parse timesCited:" + valElement.getTextContent());					                
					                throw new Exception(ex);
					            }
							}
						}
					}
					
					wosCitation.setEndDate(new Date());
					wosCitation.setRemark(wosCitation.buildMetricsRemark());
					wosCitation.setMetricType(ConstantMetrics.STATS_INDICATOR_TYPE_WOS);
	                if (log.isDebugEnabled())
                    {
                        DOMSource domSource = new DOMSource(element);
                        StringWriter writer = new StringWriter();
                        StreamResult result = new StreamResult(writer);
                        TransformerFactory tf = TransformerFactory
                                .newInstance();
                        Transformer transformer = tf.newTransformer();
                        transformer.transform(domSource, result);
                        log.debug(writer.toString());
                    }
					this.wosCitations.add(wosCitation);
				}
			}

		} catch (Exception e) {
		    log.error(e.getMessage(), e);
			error = true;
		}
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public List<CrisMetrics> getCitations() {
		return wosCitations;
	}

}
