/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import gr.ekt.bte.core.Record;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 * @author Philipp Rumpf
 */
public class ISBNService
{
	private int timeout = 1000;

	/**
	 * How long to wait for a connection to be established.
	 *
	 * @param timeout milliseconds
	 */
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

	public List<Record> searchByTerm(String title, String author, int year)
			throws HttpException, IOException
	{
		StringBuffer query = new StringBuffer();
		if (StringUtils.isNotBlank(title))
		{
			query.append("ti:\"").append(title).append("\"");
		}
		if (StringUtils.isNotBlank(author))
		{
			// [FAU]
			if (query.length() > 0)
				query.append(" AND ");
			query.append("au:\"").append(author).append("\"");
		}
		return search(query.toString(), "", 10, "", "");
	}

	private List<Record> search_marcxml(String query, String isbn, int max_result, String baseurl, String field)
			throws HttpException, IOException
	{
		List<Record> results = new ArrayList<Record>();
		CloseableHttpClient client = null;
		HttpGet method = null;
		try
		{
			client = new DefaultHttpClient();
			HttpParams params = client.getParams();
			params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

			try {
				URIBuilder uriBuilder = new URIBuilder(baseurl);
				uriBuilder.addParameter("version", "1.1");
				uriBuilder.addParameter("recordSchema", "marcxml");
				uriBuilder.addParameter("operation", "searchRetrieve");
				uriBuilder.addParameter("query", (query == null) ? (field + "=" + isbn) : query);
				uriBuilder.addParameter("maximumRecords", String.valueOf(max_result * 10));

				method = new HttpGet(uriBuilder.build());
			} catch (URISyntaxException ex)
			{
				throw new HttpException(ex.getMessage());
			}

			// Execute the method.
			HttpResponse response = client.execute(method);
			StatusLine responseStatus = response.getStatusLine();
			int statusCode = responseStatus.getStatusCode();

			if (statusCode != HttpStatus.SC_OK)
			{
				if (statusCode == HttpStatus.SC_BAD_REQUEST)
					throw new RuntimeException("ISBN query is not valid");
				else
					throw new RuntimeException("Http call failed: "
							+ responseStatus);
			}

			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				factory.setIgnoringElementContentWhitespace(true);

				DocumentBuilder db = factory.newDocumentBuilder();
				Document inDoc = db.parse(response.getEntity().getContent());

				Element xmlRoot = inDoc.getDocumentElement();
				NodeList records = xmlRoot.getElementsByTagName("record");
				for (int i = 0; i < records.getLength(); i++)
				{
					Element recordel = (Element) records.item(i);
					if (recordel == null)
						continue;
					Record crossitem = ISBNUtils
							.convertISBNDomToRecord(recordel, isbn);
					if (crossitem != null)
					{
						results.add(crossitem);
					}
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(
						"ISBN identifier is not valid or not exist");
			}
		}
		finally
		{
			if (method != null)
			{			    
				method.releaseConnection();
			}
			if (client != null) 
			{
			    client.close();
			}
		}

		return results;
	}

	private List<Record> search(String query, String isbn, int max_result, String url, String name)
			throws IOException, HttpException
	{
		return search_marcxml(query, isbn, max_result, url, name);
	}

	public Record getByISBN(String raw, String url, String name) throws HttpException, IOException
	{
		if (StringUtils.isNotBlank(raw))
		{
			raw = raw.trim();
			raw = raw.replaceAll("[- ]", "");
			List<Record> result = search(null, raw, 1, url, name);
			if (result != null && result.size() > 0)
			{
				return result.get(0);
			}
		}
		return null;
	}
}
