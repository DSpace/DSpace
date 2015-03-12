package org.datadryad.dspace.statistics;

import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.w3c.dom.Document;

public class ItemFileStats {

	private static final Logger LOGGER = Logger.getLogger(ItemFileStats.class);

	private static final String FDL_SEARCH = "/select/?indent=on&q=owningItem:";

	private static final String FV_SEARCH = "/select/?indent=on&q=id:";

	private static final String COUNTER = "//result/@numFound";

	private Item myItem;

	public ItemFileStats(Context aContext, Item aItem) throws SQLException,
			StatisticsException {
		if (aItem != null) {
			String handle = aItem.getHandle();
			Item pkg = (Item) HandleManager.resolveToDataPackage(aContext,
					handle);

			if (handle.equals(pkg.getHandle())) {
				throw new StatisticsException(
						"Supplied item isn't a data file item");
			}
		}
		myItem = aItem;
	}

	public int getDataFileViews() {
		String solr = ConfigurationManager.getProperty("solr.log.server");
		int viewCount = 0;

		if (myItem == null) {
			return viewCount;
		}

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			int dsoID = myItem.getID();
			GetMethod get = new GetMethod(solr + FV_SEARCH + dsoID);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Querying to find data file views for " + dsoID);
			}

			switch (new HttpClient().executeMethod(get)) {
			case 200:
			case 201:
			case 202:
				Document doc = db.parse(get.getResponseBodyAsStream());
				doc.getDocumentElement().normalize();
				XPathFactory xpf = XPathFactory.newInstance();
				XPath xpath = xpf.newXPath();
				String result = xpath.evaluate(COUNTER, doc);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Solr responded: " + result + " views");
				}

				viewCount = Integer.parseInt(result);
				break;
			default:
				LOGGER.error("Solr statistics failed to respond as expected");
			}

			get.releaseConnection();
		}
		catch (Exception details) {
			LOGGER.error(details.getMessage(), details);
		}

		return viewCount;
	}

	public int getFileDownloads() {
		String solr = ConfigurationManager.getProperty("solr.log.server");
		int dlCount = 0;

		if (myItem == null) {
			return dlCount;
		}
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			int dsoID = myItem.getID();
			GetMethod get = new GetMethod(solr + FDL_SEARCH + dsoID);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Querying to find file downloads for " + dsoID);
			}

			switch (new HttpClient().executeMethod(get)) {
			case 200:
			case 201:
			case 202:
				Document doc = db.parse(get.getResponseBodyAsStream());
				doc.getDocumentElement().normalize();
				XPathFactory xpf = XPathFactory.newInstance();
				XPath xpath = xpf.newXPath();
				String result = xpath.evaluate(COUNTER, doc);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Solr responded: " + result + " downloads");
				}

				dlCount = Integer.parseInt(result);
				break;
			default:
				LOGGER.error("Solr statistics failed to respond as expected");
			}

			get.releaseConnection();
		}
		catch (Exception details) {
			LOGGER.error(details.getMessage(), details);
		}

		return dlCount;
	}
}
