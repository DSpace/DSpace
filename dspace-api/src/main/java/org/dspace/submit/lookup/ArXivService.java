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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.apache.http.params.HttpParams;
import org.dspace.app.util.XMLUtils;
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

    /**
     * How long to wait for a connection to be established.
     *
     * @param timeout milliseconds
     */
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

    protected List<Record> search(String query, String arxivid, int max_result)
    		throws IOException, HttpException
    		{
    	List<Record> results = new ArrayList<Record>();
    	HttpGet method = null;
    	try
    	{
    		HttpClient client = new DefaultHttpClient();
            HttpParams params = client.getParams();
            params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

            try {
                URIBuilder uriBuilder = new URIBuilder("http://export.arxiv.org/api/query");
                uriBuilder.addParameter("id_list", arxivid);
                uriBuilder.addParameter("search_query", query);
                uriBuilder.addParameter("max_results", String.valueOf(max_result));
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
    				throw new RuntimeException("arXiv query is not valid");
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
