package org.dspace.app.xmlui.aspect.dryadinfo;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DryadBlogFeed extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {

	private static final Logger LOGGER = Logger.getLogger(DryadBlogFeed.class);

	private static final Message HEADER = message("xmlui.Site.blogfeed.title");

	private SourceValidity myValidity;

	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {
		Division home = body.addDivision("dryad-info-home", "blog-box");
		Division dryadFeed = home.addDivision("blog-hook");
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			GetMethod get = new GetMethod("http://blog.datadryad.org/feed/");

			switch (new HttpClient().executeMethod(get)) {
			case 200:
			case 201:
			case 202:
				Document doc = db.parse(get.getResponseBodyAsStream());
				doc.getDocumentElement().normalize();
				XPathFactory xpf = XPathFactory.newInstance();
				NodeList nodes = (NodeList) xpf.newXPath().evaluate("//item",
						doc, XPathConstants.NODESET);
				String entryCount = ConfigurationManager
						.getProperty("dryad.feed.entry.count");
				int totalEntries;

				try {
					totalEntries = Integer.parseInt(entryCount);
				}
				catch (NumberFormatException details) {
					totalEntries = nodes.getLength();
				}

				for (int index = 0; index < totalEntries; index++) {
					Element element = (Element) nodes.item(index);
					Node title = element.getElementsByTagName("title").item(0);
					Node link = element.getElementsByTagName("link").item(0);
//					NodeList dn = element.getElementsByTagName("description");
					String titleValue = title.getTextContent();
					String linkValue = link.getTextContent();
					List list = dryadFeed.addList("entries", List.TYPE_SIMPLE);

					list.addItemXref(linkValue, titleValue);

/* Decided we didn't want to display any text from the description */
//					if (dn.getLength() > 0) {
//						String text = dn.item(0).getTextContent();
//						int end = text.indexOf("[...]");
//						
//						if (end != -1) {
//							text = text.substring(0, end + 5);
//						}
//						else if ((end = text.indexOf("<")) != -1) {
//							text = text.substring(0, end) + " [...]";
//						}
//
//						text = StringEscapeUtils.unescapeHtml(text);
//						list.addList("entry-description").addItem(text);
//					}
				}
			default:
				LOGGER.warn("Failed to connect with blog.datadryad.org/feed");
			}

			// We don't want to display unless we have some entries
			dryadFeed.setHead(HEADER);

			get.releaseConnection();
		}
		catch (Exception details) {
			LOGGER.error("Failed to pull in blog.datadryad.org/feed", details);
		}
	}

//	@Override
	public Serializable getKey() {
		return getClass().getName();
	}

//	@Override
	public SourceValidity getValidity() {
		if (myValidity == null) {
			DSpaceValidity newValidity = new DSpaceValidity();
			newValidity.setAssumedValidityDelay(21600000); // 4x a day
			myValidity = newValidity.complete();
		}

		return myValidity;
	}

}
