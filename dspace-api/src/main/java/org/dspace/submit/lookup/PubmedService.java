/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.Record;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.dspace.app.util.XMLUtils;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class PubmedService
{

    private static final Logger log = Logger.getLogger(PubmedService.class);

    protected int timeout = 1000;

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public Record getByPubmedID(String pubmedid) throws HttpException,
            IOException, ParserConfigurationException, SAXException
    {
        List<String> ids = new ArrayList<String>();
        ids.add(pubmedid.trim());
        List<Record> items = getByPubmedIDs(ids);
        if (items != null && items.size() > 0)
        {
            return items.get(0);
        }
        return null;
    }

    public List<Record> search(String title, String author, int year)
            throws HttpException, IOException
    {
        StringBuffer query = new StringBuffer();
        if (StringUtils.isNotBlank(title))
        {
            query.append("((").append(title).append("[TI]) OR (");
            // [TI] does not always work, book chapter title
            query.append("(").append(title).append("[book]))");
        }
        if (StringUtils.isNotBlank(author))
        {
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

    public List<Record> search(String query) throws IOException, HttpException
    {
        List<Record> results = new ArrayList<>();
        if (!ConfigurationManager.getBooleanProperty(SubmissionLookupService.CFG_MODULE, "remoteservice.demo"))
        {
            HttpGet method = null;
            try
            {
                HttpClient client = new DefaultHttpClient();
                client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

                URIBuilder uriBuilder = new URIBuilder(
                        "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi");
                uriBuilder.addParameter("db", "pubmed");
                uriBuilder.addParameter("datetype", "edat");
                uriBuilder.addParameter("retmax", "10");
                uriBuilder.addParameter("term", query);
                method = new HttpGet(uriBuilder.build());

                // Execute the method.
                HttpResponse response = client.execute(method);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();

                if (statusCode != HttpStatus.SC_OK)
                {
                    throw new RuntimeException("WS call failed: "
                            + statusLine);
                }

                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                factory.setValidating(false);
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(true);

                DocumentBuilder builder;
                try
                {
                    builder = factory.newDocumentBuilder();

                    Document inDoc = builder.parse(response.getEntity().getContent());

                    Element xmlRoot = inDoc.getDocumentElement();
                    Element idList = XMLUtils.getSingleElement(xmlRoot,
                            "IdList");
                    List<String> pubmedIDs = XMLUtils.getElementValueList(
                            idList, "Id");
                    results = getByPubmedIDs(pubmedIDs);
                }
                catch (ParserConfigurationException e1)
                {
                    log.error(e1.getMessage(), e1);
                }
                catch (SAXException e1)
                {
                    log.error(e1.getMessage(), e1);
                }
            }
            catch (Exception e1)
            {
                log.error(e1.getMessage(), e1);
            }
            finally
            {
                if (method != null)
                {
                    method.releaseConnection();
                }
            }
        }
        else
        {
            InputStream stream = null;
            try
            {
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
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            finally
            {
                if (stream != null)
                {
                    try
                    {
                        stream.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return results;
    }

    public List<Record> getByPubmedIDs(List<String> pubmedIDs)
            throws HttpException, IOException, ParserConfigurationException,
            SAXException
    {
    	List<Record> results = new ArrayList<Record>();
    	HttpGet method = null;
    	try
    	{
    		HttpClient client = new DefaultHttpClient();
    		client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5 * timeout);

            try {
                URIBuilder uriBuilder = new URIBuilder(
                        "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi");
                uriBuilder.addParameter("db", "pubmed");
                uriBuilder.addParameter("retmode", "xml");
                uriBuilder.addParameter("rettype", "full");
                uriBuilder.addParameter("id", StringUtils.join(
                        pubmedIDs.iterator(), ","));
                method = new HttpGet(uriBuilder.build());
            } catch (URISyntaxException ex)
            {
                throw new RuntimeException("Request not sent", ex);
            }

            // Execute the method.
    		HttpResponse response = client.execute(method);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

    		if (statusCode != HttpStatus.SC_OK)
    		{
    			throw new RuntimeException("WS call failed: " + statusLine);
    		}

    		DocumentBuilderFactory factory = DocumentBuilderFactory
    				.newInstance();
    		factory.setValidating(false);
    		factory.setIgnoringComments(true);
    		factory.setIgnoringElementContentWhitespace(true);

    		DocumentBuilder builder = factory.newDocumentBuilder();
    		Document inDoc = builder
    				.parse(response.getEntity().getContent());

    		Element xmlRoot = inDoc.getDocumentElement();
    		List<Element> pubArticles = XMLUtils.getElementList(xmlRoot,
    				"PubmedArticle");

    		for (Element xmlArticle : pubArticles)
    		{
    			Record pubmedItem = null;
    			try
    			{
    				pubmedItem = PubmedUtils
    						.convertPubmedDomToRecord(xmlArticle);
    				results.add(pubmedItem);
    			}
    			catch (Exception e)
    			{
    				throw new RuntimeException(
    						"PubmedID is not valid or not exist: "
    								+ e.getMessage(), e);
    			}
    		}

    		return results;
    	}
    	finally
    	{
    		if (method != null)
    		{
    			method.releaseConnection();
    		}
    	}
    }

    public List<Record> search(String doi, String pmid) throws HttpException,
            IOException
    {
        StringBuffer query = new StringBuffer();
        if (StringUtils.isNotBlank(doi))
        {
            query.append(doi);
            query.append("[AID]");
        }
        if (StringUtils.isNotBlank(pmid))
        {
            // [FAU]
            if (query.length() > 0)
                query.append(" OR ");
            query.append(pmid).append("[PMID]");
        }
        return search(query.toString());
    }
}
