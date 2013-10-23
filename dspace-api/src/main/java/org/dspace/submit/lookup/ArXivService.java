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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class ArXivService
{
    private int timeout = 1000;

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public List<Record> getByDOIs(Set<String> dois) throws HttpException,
            IOException
    {
        if (dois != null && dois.size() > 0)
        {
            String doisQuery = StringUtils.join(dois.iterator(), " OR ");
            return search(doisQuery, null, 100);
        }
        return null;
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
        return search(query.toString(), "", 10);
    }

    private List<Record> search(String query, String arxivid, int max_result)
            throws IOException, HttpException
    {
        List<Record> results = new ArrayList<Record>();
        if (!ConfigurationManager.getBooleanProperty(SubmissionLookupService.CFG_MODULE, "remoteservice.demo"))
        {
            GetMethod method = null;
            try
            {
                HttpClient client = new HttpClient();
                client.setTimeout(timeout);
                method = new GetMethod("http://export.arxiv.org/api/query");
                NameValuePair id = new NameValuePair("id_list", arxivid);
                NameValuePair queryParam = new NameValuePair("search_query",
                        query);
                NameValuePair count = new NameValuePair("max_results",
                        String.valueOf(max_result));
                method.setQueryString(new NameValuePair[] { id, queryParam,
                        count });
                // Execute the method.
                int statusCode = client.executeMethod(method);

                if (statusCode != HttpStatus.SC_OK)
                {
                    if (statusCode == HttpStatus.SC_BAD_REQUEST)
                        throw new RuntimeException("arXiv query is not valid");
                    else
                        throw new RuntimeException("Http call failed: "
                                + method.getStatusLine());
                }

                try
                {
                    DocumentBuilderFactory factory = DocumentBuilderFactory
                            .newInstance();
                    factory.setValidating(false);
                    factory.setIgnoringComments(true);
                    factory.setIgnoringElementContentWhitespace(true);

                    DocumentBuilder db = factory.newDocumentBuilder();
                    Document inDoc = db.parse(method.getResponseBodyAsStream());

                    Element xmlRoot = inDoc.getDocumentElement();
                    List<Element> dataRoots = XMLUtils.getElementList(xmlRoot,
                            "entry");

                    for (Element dataRoot : dataRoots)
                    {
                        Record crossitem = ArxivUtils
                                .convertArxixDomToRecord(dataRoot);
                        if (crossitem != null)
                        {
                            results.add(crossitem);
                        }
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(
                            "ArXiv identifier is not valid or not exist");
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
        else
        {
            InputStream stream = null;
            try
            {
                File file = new File(
                        ConfigurationManager.getProperty("dspace.dir")
                                + "/config/crosswalks/demo/arxiv.xml");
                stream = new FileInputStream(file);
                try
                {
                    DocumentBuilderFactory factory = DocumentBuilderFactory
                            .newInstance();
                    factory.setValidating(false);
                    factory.setIgnoringComments(true);
                    factory.setIgnoringElementContentWhitespace(true);
                    DocumentBuilder db = factory.newDocumentBuilder();
                    Document inDoc = db.parse(stream);

                    Element xmlRoot = inDoc.getDocumentElement();
                    List<Element> dataRoots = XMLUtils.getElementList(xmlRoot,
                            "entry");
                    for (Element dataRoot : dataRoots)
                    {
                        Record crossitem = ArxivUtils
                                .convertArxixDomToRecord(dataRoot);

                        if (crossitem != null)
                        {
                            results.add(crossitem);
                        }
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(
                            "ArXiv identifier is not valid or not exist");
                }
            }
            finally
            {
                if (stream != null)
                {
                    stream.close();
                }
            }
        }
        return results;
    }

    public Record getByArXivIDs(String raw) throws HttpException, IOException
    {
        if (StringUtils.isNotBlank(raw))
        {
            raw = raw.trim();
            if (raw.startsWith("http://arxiv.org/abs/"))
            {
                raw = raw.substring("http://arxiv.org/abs/".length());
            }
            else if (raw.toLowerCase().startsWith("arxiv:"))
            {
                raw = raw.substring("arxiv:".length());
            }
            List<Record> result = search("", raw, 1);
            if (result != null && result.size() > 0)
            {
                return result.get(0);
            }
        }
        return null;
    }
}
