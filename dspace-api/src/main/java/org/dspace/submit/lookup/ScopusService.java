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
public class ScopusService
{

    private static final Logger log = Logger.getLogger(ScopusService.class);

    private int timeout = 1000;

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
    
    private String apiKey = ConfigurationManager.getProperty("submission.lookup.scopus.apikey");

    public List<Record> search(String title, String author, int year)
            throws HttpException, IOException
    {
        StringBuffer query = new StringBuffer();
        if (StringUtils.isNotBlank(title))
        {
        	query.append("title(").append(title).append("");
        }
        if (StringUtils.isNotBlank(author))
        {
            // [FAU]
            if (query.length() > 0)
                query.append(" AND ");
            query.append("AUTH(").append(author).append(")");
        }
        if (year != -1)
        {
            // [DP]
            if (query.length() > 0)
                query.append(" AND ");
            query.append("PUBYEAR IS ").append(year);
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
                        "http://api.elsevier.com/content/search/scopus");
                uriBuilder.addParameter("apiKey", apiKey);
                uriBuilder.addParameter("view", "COMPLETE");
                uriBuilder.addParameter("query", query);
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
            		List<Element> pubArticles = XMLUtils.getElementList(xmlRoot,
            				"entry");

            		for (Element xmlArticle : pubArticles)
            		{
            			Record scopusItem = null;
            			try
            			{
            				scopusItem = ScopusUtils
            						.convertScopusDomToRecord(xmlArticle);
            				results.add(scopusItem);
            			}
            			catch (Exception e)
            			{
            				throw new RuntimeException(
            						"EID is not valid or not exist: "
            								+ e.getMessage(), e);
            			}
            		}

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
                                + "/config/crosswalks/demo/scopus-search.xml");
                stream = new FileInputStream(file);
                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                factory.setValidating(false);
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(true);

                DocumentBuilder builder = factory.newDocumentBuilder();
                Document inDoc = builder.parse(stream);

                Element xmlRoot = inDoc.getDocumentElement();
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

    public List<Record> search(String doi, String eid) throws HttpException,
            IOException
    {
        StringBuffer query = new StringBuffer();
        if (StringUtils.isNotBlank(doi))
        {
            query.append("DOI(").append(doi).append(")");
            
        }
        if (StringUtils.isNotBlank(eid))
        {
            // [FAU]
            if (query.length() > 0)
                query.append(" OR ");
            query.append("eid(").append(eid).append(")");
        }
        return search(query.toString());
    }
}
