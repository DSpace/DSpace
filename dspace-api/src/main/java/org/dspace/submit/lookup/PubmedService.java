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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.submit.importer.pubmed.PubmedItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class PubmedService {
	private int timeout = 1000;
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public PubmedItem getByPubmedID(String pubmedid) throws HttpException,
			IOException, ParserConfigurationException, SAXException {
		List<String> ids = new ArrayList<String>();
		ids.add(pubmedid.trim());
		List<PubmedItem> items = getByPubmedIDs(ids);
		if (items != null && items.size() > 0)
		{
			return items.get(0);
		}
		return null;
	}

	public List<PubmedItem> search(String title, String author, int year)
			throws HttpException, IOException {
		StringBuffer query = new StringBuffer();
		if (StringUtils.isNotBlank(title)) {
			query.append("((").append(title).append("[TI]) OR (");
			// [TI] non funziona sempre, titolo di capitoli di libro
			query.append("(").append(title).append("[book]))");
		}
		if (StringUtils.isNotBlank(author)) {
			// [FAU]
			if (query.length() > 0)
				query.append(" AND ");
			query.append("(").append(author).append("[AU])");
		}
		if (year != -1)
		{
			// [DP]
			if (query.length() > 0)
				query.append(" AND ");
			query.append(year).append("[DP]");
		}
		return search(query.toString());
	}

	public List<PubmedItem> search(String query) throws IOException,
			HttpException {
		List<PubmedItem> results = null;
		if (!ConfigurationManager.getBooleanProperty("remoteservice.demo")) {
			GetMethod method = null;
			try {
				HttpClient client = new HttpClient();
				client.setTimeout(timeout);
				method = new GetMethod(
						"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi");

				NameValuePair db = new NameValuePair("db", "pubmed");
				NameValuePair datetype = new NameValuePair("datetype", "edat");
				NameValuePair retmax = new NameValuePair("retmax", "10");
				NameValuePair queryParam = new NameValuePair("term",
						query.toString());
				method.setQueryString(new NameValuePair[] { db, queryParam,
						retmax, datetype });
				// Execute the method.
				int statusCode = client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {
					throw new RuntimeException(
							"Chiamata al webservice fallita: "
									+ method.getStatusLine());
				}

				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				factory.setIgnoringElementContentWhitespace(true);

				DocumentBuilder builder;
				try {
					builder = factory.newDocumentBuilder();

					Document inDoc = builder.parse(method
							.getResponseBodyAsStream());

					Element xmlRoot = inDoc.getDocumentElement();
					Element idList = XMLUtils.getSingleElement(xmlRoot,
							"IdList");
					List<String> pubmedIDs = XMLUtils.getElementValueList(
							idList, "Id");
					results = getByPubmedIDs(pubmedIDs);
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}
				catch (SAXException e1) {
					e1.printStackTrace();
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
								+ "/config/crosswalks/demo/pubmed-search.xml");
				stream = new FileInputStream(file);
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				factory.setIgnoringElementContentWhitespace(true);

				DocumentBuilder builder = factory.newDocumentBuilder();
				Document inDoc = builder.parse(stream);

				Element xmlRoot = inDoc.getDocumentElement();
				Element idList = XMLUtils.getSingleElement(xmlRoot, "IdList");
				List<String> pubmedIDs = XMLUtils.getElementValueList(idList,
						"Id");
				results = getByPubmedIDs(pubmedIDs);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return results;
	}

	public List<PubmedItem> getByPubmedIDs(List<String> pubmedIDs)
			throws HttpException, IOException, ParserConfigurationException, SAXException {
		List<PubmedItem> results = new ArrayList<PubmedItem>();
		if (!ConfigurationManager.getBooleanProperty("remoteservice.demo")) {
			GetMethod method = null;
			try {
				HttpClient client = new HttpClient();
				client.setTimeout(5 * timeout);
				method = new GetMethod(
						"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi");

				NameValuePair db = new NameValuePair("db", "pubmed");
				NameValuePair retmode = new NameValuePair("retmode", "xml");
				NameValuePair rettype = new NameValuePair("rettype", "full");
				NameValuePair id = new NameValuePair("id", StringUtils.join(pubmedIDs.iterator(), ","));
				method.setQueryString(new NameValuePair[] { db, retmode,
						rettype, id });
				// Execute the method.
				int statusCode = client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {
					throw new RuntimeException(
							"Chiamata al webservice fallita: "
									+ method.getStatusLine());
				}

		    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        factory.setValidating(false);
		        factory.setIgnoringComments(true);
		        factory.setIgnoringElementContentWhitespace(true);

		        DocumentBuilder builder = factory.newDocumentBuilder();
		        Document inDoc = builder.parse(method.getResponseBodyAsStream());

		        Element xmlRoot = inDoc.getDocumentElement();
		        List<Element> pubArticles = XMLUtils
		                .getElementList(xmlRoot, "PubmedArticle");

		        for (Element xmlArticle : pubArticles)
		        {
					PubmedItem pubmedItem = null;
					try {
						pubmedItem = new PubmedItem(xmlArticle);
						results.add(pubmedItem);
					} catch (Exception e) {
						throw new RuntimeException(
								"PubmedID non valido o inesistente: "+e.getMessage(), e);
					}
		        }

				return results;
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
								+ "/config/crosswalks/demo/pubmed.xml");
				stream = new FileInputStream(file);

		    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        factory.setValidating(false);
		        factory.setIgnoringComments(true);
		        factory.setIgnoringElementContentWhitespace(true);

		        DocumentBuilder builder = factory.newDocumentBuilder();
		        Document inDoc = builder.parse(stream);

		        Element xmlRoot = inDoc.getDocumentElement();
		        List<Element> pubArticles = XMLUtils
		                .getElementList(xmlRoot, "PubmedArticle");

		        for (Element xmlArticle : pubArticles)
		        {
					PubmedItem pubmedItem = null;
					try {
						pubmedItem = new PubmedItem(xmlArticle);
						results.add(pubmedItem);
					} catch (Exception e) {
						throw new RuntimeException(
								"PubmedID non valido o inesistente: "+e.getMessage(), e);
					}
		        }

				return results;
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			} finally {
				if (stream != null) {
					stream.close();
				}
			}
		}
	}

	public List<PubmedItem> search(String doi, String pmid) throws HttpException, IOException {
		StringBuffer query = new StringBuffer();
		if (StringUtils.isNotBlank(doi)) {
			query.append(doi);
			query.append("[AID]");
		}
		if (StringUtils.isNotBlank(pmid)) {
			// [FAU]
			if (query.length() > 0)
				query.append(" OR ");
			query.append(pmid).append("[PMID]");
		}
		return search(query.toString());
	}
}
