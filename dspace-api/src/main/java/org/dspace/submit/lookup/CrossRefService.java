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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class CrossRefService
{

    private static final Logger log = Logger.getLogger(CrossRefService.class);

    protected int timeout = 1000;

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
            		HttpGet method = null;
            		try
            		{
            			HttpClient client = new DefaultHttpClient();
                        client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

                        try {
                        URIBuilder uriBuilder = new URIBuilder(
            					"http://www.crossref.org/openurl/");
                        uriBuilder.addParameter("pid", apiKey);
                        uriBuilder.addParameter("noredirect", "true");
                        uriBuilder.addParameter("id", record);
                        method = new HttpGet(uriBuilder.build());
                        } catch (URISyntaxException ex) {
                            throw new HttpException("Request not sent", ex);
                        }

                        // Execute the method.
            			HttpResponse response = client.execute(method);
                        StatusLine statusLine = response.getStatusLine();
                        int statusCode = statusLine.getStatusCode();

            			if (statusCode != HttpStatus.SC_OK)
            			{
            				throw new RuntimeException("Http call failed: "
            						+ statusLine);
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
            				Document inDoc = db.parse(response.getEntity().getContent());

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

    public List<Record> search(Context context, String title, String authors,
            int year, int count, String apiKey) throws IOException, HttpException
    {
        HttpGet method = null;
        try
        {
            HttpClient client = new DefaultHttpClient();
            client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

            URIBuilder uriBuilder = new URIBuilder("http://search.labs.crossref.org/dois");

            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotBlank(title))
            {
                sb.append(title);
            }
            sb.append(" ");
            if (StringUtils.isNotBlank(authors))
            {
                sb.append(authors);
            }
            String q = sb.toString().trim();
            uriBuilder.addParameter("q", q);

            uriBuilder.addParameter("year", year != -1 ? String.valueOf(year) : "");
            uriBuilder.addParameter("rows", count != -1 ? String.valueOf(count) : "");
            method = new HttpGet(uriBuilder.build());

            // Execute the method.
            HttpResponse response = client.execute(method);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode != HttpStatus.SC_OK)
            {
                throw new RuntimeException("Http call failed:: "
                        + statusLine);
            }

            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Map>>()
            {
            }.getType();
            List<Map> json = gson.fromJson(
                    IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
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
