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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.submit.importer.crossref.CrossrefItem;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import flexjson.JSONDeserializer;

public class CrossRefService {
    
    private static final Logger log = Logger.getLogger(CrossRefService.class);
    
	private int timeout = 1000;
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public List<CrossrefItem> search(Context context, Set<String> dois) throws HttpException,
			IOException, JDOMException, ParserConfigurationException,
			SAXException {
		List<CrossrefItem> results = new ArrayList<CrossrefItem>();
		if (dois != null && dois.size() > 0) {
			for (String record : dois) {
				try
				{
					if (!ConfigurationManager
							.getBooleanProperty("remoteservice.demo")) {
						GetMethod method = null;
						try {
							String apiKey = ConfigurationManager
									.getProperty("crossref.api-key");
	
							HttpClient client = new HttpClient();
							client.setConnectionTimeout(timeout);
							method = new GetMethod(
									"http://www.crossref.org/openurl/");
	
							NameValuePair pid = new NameValuePair("pid", apiKey);
							NameValuePair noredirect = new NameValuePair(
									"noredirect", "true");
							NameValuePair id = new NameValuePair("id", record);
							method.setQueryString(new NameValuePair[] { pid,
									noredirect, id });
							// Execute the method.
							int statusCode = client.executeMethod(method);
	
							if (statusCode != HttpStatus.SC_OK) {
								throw new RuntimeException(
										"Chiamata http fallita: "
												+ method.getStatusLine());
							}
	
							CrossrefItem crossitem;
							try {
								crossitem = new CrossrefItem(
										method.getResponseBodyAsStream());
								results.add(crossitem);
							} catch (Exception e) {
								log.warn(LogManager
										.getHeader(
												context,
												"retrieveRecordDOI",
												record
														+ " DOI non valido o inesistente: "
														+ e.getMessage()));
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
											+ "/config/crosswalks/demo/doi.xml");
							stream = new FileInputStream(file);
							results.add(new CrossrefItem(stream));
						} finally {
							if (stream != null) {
								stream.close();
							}
						}
					}
				}
				catch (RuntimeException rt)
				{
					rt.printStackTrace();
				}
			}
		}
		return results;
	}

	public NameValuePair[] buildQueryPart(String title, String author, int year, int count) {
		StringBuffer sb = new StringBuffer();
		if (StringUtils.isNotBlank(title)) {
			sb.append(title);
		}
		sb.append(" ");
		if (StringUtils.isNotBlank(author)) {
			sb.append(author);
		}
		String q = sb.toString().trim();
		NameValuePair qParam = new NameValuePair("q", title);
		NameValuePair yearParam = new NameValuePair("year",
				year != -1?String.valueOf(year):"");
		NameValuePair countParam = new NameValuePair("rows",
				count!= -1?String.valueOf(count):"");
		
		NameValuePair[] query = new NameValuePair[] { qParam,
				yearParam, countParam };
		return query;
	}

	public List<CrossrefItem> search(Context context, String title, String authors, int year,
			int count) throws IOException, HttpException {
		List<CrossrefItem> results = new ArrayList<CrossrefItem>();
		GetMethod method = null;
		try {
			NameValuePair[] query = buildQueryPart(title, authors, year, count);
			HttpClient client = new HttpClient();
			client.setTimeout(timeout);
			method = new GetMethod("http://search.labs.crossref.org/dois");

			method.setQueryString(query);
			// Execute the method.
			int statusCode = client.executeMethod(method);

			if (statusCode != HttpStatus.SC_OK) {
				throw new RuntimeException("Chiamata http fallita: "
						+ method.getStatusLine());
			}

			JSONDeserializer<List<Map>> des = new JSONDeserializer<List<Map>>();
			List<Map> json = des.deserialize(method.getResponseBodyAsString());
			Set<String> dois = new HashSet<String>();
			for (Map r : json) {
				dois.add(SubmissionLookupUtils.normalizeDOI((String) r.get("doi")));
			}
			method.releaseConnection();

			return search(context, dois);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
	}
}
