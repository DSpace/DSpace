/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.Record;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.XMLUtils;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.jdom.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class CrossRefService
{

    private static final Logger log = Logger.getLogger(CrossRefService.class);

    private int timeout = 1000;

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public List<Record> search(Context context, Set<String> dois, String apiKey)
            throws HttpException, IOException, JDOMException,
            ParserConfigurationException, SAXException
    {
        List<Record> results = new ArrayList<Record>();
        if (dois != null && dois.size() > 0)
        {
            for (String record : dois)
            {
            	try
            	{
            		GetMethod method = null;
            		try
            		{
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

            			if (statusCode != HttpStatus.SC_OK)
            			{
            				throw new RuntimeException("Http call failed: "
            						+ method.getStatusLine());
            			}

            			Record crossitem;
            			try
            			{
            				DocumentBuilderFactory factory = DocumentBuilderFactory
            						.newInstance();
            				factory.setValidating(false);
            				factory.setIgnoringComments(true);
            				factory.setIgnoringElementContentWhitespace(true);

            				DocumentBuilder db = factory
            						.newDocumentBuilder();
            				Document inDoc = db.parse(method
            						.getResponseBodyAsStream());

            				Element xmlRoot = inDoc.getDocumentElement();
            				Element queryResult = XMLUtils.getSingleElement(xmlRoot, "query_result");
            				Element body = XMLUtils.getSingleElement(queryResult, "body");
            				Element dataRoot = XMLUtils.getSingleElement(body, "query");

            				crossitem = CrossRefUtils
            						.convertCrossRefDomToRecord(dataRoot);
            				results.add(crossitem);
            			}
            			catch (Exception e)
            			{
            				log.warn(LogManager
            						.getHeader(
            								context,
            								"retrieveRecordDOI",
            								record
            								+ " DOI is not valid or not exist: "
            								+ e.getMessage()));
            			}
            		}
            		finally
            		{
            			if (method != null)
            			{
            				method.releaseConnection();
            			}
            		}
            	}
            	catch (RuntimeException rt)
            	{
            		log.error(rt.getMessage(), rt);
            	}
            }
        }
        return results;
    }

    public NameValuePair[] buildQueryPart(String title, String author,
            int year, int count)
    {
        StringBuffer sb = new StringBuffer();
        if (StringUtils.isNotBlank(title))
        {
            sb.append(title);
        }
        sb.append(" ");
        if (StringUtils.isNotBlank(author))
        {
            sb.append(author);
        }
        String q = sb.toString().trim();
        NameValuePair qParam = new NameValuePair("q", title);
        NameValuePair yearParam = new NameValuePair("year",
                year != -1 ? String.valueOf(year) : "");
        NameValuePair countParam = new NameValuePair("rows",
                count != -1 ? String.valueOf(count) : "");

        NameValuePair[] query = new NameValuePair[] { qParam, yearParam,
                countParam };
        return query;
    }

    public List<Record> search(Context context, String title, String authors,
            int year, int count, String apiKey) throws IOException, HttpException
    {
        GetMethod method = null;
        try
        {
            NameValuePair[] query = buildQueryPart(title, authors, year, count);
            HttpClient client = new HttpClient();
            client.setTimeout(timeout);
            method = new GetMethod("http://search.labs.crossref.org/dois");

            method.setQueryString(query);
            // Execute the method.
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK)
            {
                throw new RuntimeException("Http call failed:: "
                        + method.getStatusLine());
            }

            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Map>>()
            {
            }.getType();
            List<Map> json = gson.fromJson(method.getResponseBodyAsString(),
                    listType);
            Set<String> dois = new HashSet<String>();
            for (Map r : json)
            {
                dois.add(SubmissionLookupUtils.normalizeDOI((String) r
                        .get("doi")));
            }
            method.releaseConnection();

            return search(context, dois, apiKey);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        finally
        {
            if (method != null)
            {
                method.releaseConnection();
            }
        }
    }
}
