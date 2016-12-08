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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import org.apache.log4j.Logger;
import org.dspace.app.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Keiji Suzuki
 */
public class CiNiiService
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(CiNiiService.class);

    protected int timeout = 1000;

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public Record getByCiNiiID(String id, String appId) throws HttpException,
            IOException
    {
            return search(id, appId);
    }

    public List<Record> searchByTerm(String title, String author, int year, 
            int maxResults, String appId)
            throws HttpException, IOException
    {
        List<Record> records = new ArrayList<Record>();

        List<String> ids = getCiNiiIDs(title, author, year, maxResults, appId);
        if (ids != null && ids.size() > 0)
        {
            for (String id : ids)
            {
                Record record = search(id, appId);
                if (record != null)
                {
                    records.add(record);
                }
            }
        }

        return records;
    }

    /**
     * Get metadata by searching CiNii RDF API with CiNii NAID
     *
     * @param id
     *     CiNii NAID to search by
     * @param appId
     *     registered application identifier for the API
     * @return record metadata
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws HttpException
     *     Represents a XML/HTTP fault and provides access to the HTTP status code.
     */
    protected Record search(String id, String appId)
        throws IOException, HttpException
    {
        HttpGet method = null;
        try
        {
            HttpClient client = new DefaultHttpClient();
            client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
            method = new HttpGet("http://ci.nii.ac.jp/naid/"+id+".rdf?appid="+appId);
            // Execute the method.
            HttpResponse response = client.execute(method);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode != HttpStatus.SC_OK)
            {
                if (statusCode == HttpStatus.SC_BAD_REQUEST)
                    throw new RuntimeException("CiNii RDF is not valid");
                else
                    throw new RuntimeException("CiNii RDF Http call failed: "
                            + statusLine);
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

                return CiNiiUtils.convertCiNiiDomToRecord(xmlRoot);
            }
            catch (Exception e)
            {
                throw new RuntimeException(
                        "CiNii RDF identifier is not valid or not exist");
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

    /**
     * Get CiNii NAIDs by searching CiNii OpenURL API with title, author and year
     *
     * @param title
     *     record title
     * @param author
     *     record author
     * @param year
     *     record year
     * @param maxResults
     *     maximun number of results returned
     * @param appId
     *     registered application identifier for the API
     * @return matching NAIDs
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws HttpException
     *     Represents a XML/HTTP fault and provides access to the HTTP status code.
     */
    protected List<String> getCiNiiIDs(String title, String author, int year,
        int maxResults, String appId) 
        throws IOException, HttpException
    {
        // Need at least one query term
        if (title == null && author == null && year == -1)
        {
            return null;
        }

        HttpGet method = null;
        List<String> ids = new ArrayList<String>();
        try
        {
            HttpClient client = new DefaultHttpClient();
            client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
            StringBuilder query = new StringBuilder();
            query.append("format=rss&appid=").append(appId)
                 .append("&count=").append(maxResults);
            if (title != null)
            {
                query.append("&title=").append(URLEncoder.encode(title, "UTF-8"));
            }
            if (author != null)
            {
                query.append("&author=").append(URLEncoder.encode(author, "UTF-8"));
            }
            if (year != -1)
            {
                query.append("&year_from=").append(String.valueOf(year));
                query.append("&year_to=").append(String.valueOf(year));
            }
            method = new HttpGet("http://ci.nii.ac.jp/opensearch/search?"+query.toString());
            // Execute the method.
            HttpResponse response = client.execute(method);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode != HttpStatus.SC_OK)
            {
                if (statusCode == HttpStatus.SC_BAD_REQUEST)
                    throw new RuntimeException("CiNii OpenSearch query is not valid");
                else
                    throw new RuntimeException("CiNii OpenSearch call failed: "
                            + statusLine);
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
                List<Element> items = XMLUtils.getElementList(xmlRoot, "item");

                int url_len = "http://ci.nii.ac.jp/naid/".length();
                for (Element item : items)
                {
                    String about = item.getAttribute("rdf:about");
                    if (about.length() > url_len)
                    {
                        ids.add(about.substring(url_len));
                    }
                }

                return ids;
            }
            catch (Exception e)
            {
                throw new RuntimeException(
                              "CiNii OpenSearch results is not valid or not exist");
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
}
