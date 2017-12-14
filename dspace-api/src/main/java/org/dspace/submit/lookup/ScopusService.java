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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
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

    private static final String ENDPOINT_SEARCH_SCOPUS = "http://api.elsevier.com/content/search/scopus";
    //private static final String ENDPOINT_SEARCH_SCOPUS = "http://localhost:9999/content/search/scopus";

    private static final Logger log = Logger.getLogger(ScopusService.class);

    private int timeout = 1000;

    int itemPerPage = 25;

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

        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
        String proxyPort = ConfigurationManager.getProperty("http.proxy.port");
        String apiKey = ConfigurationManager.getProperty("submission.lookup.scopus.apikey");
        
        List<Record> results = new ArrayList<>();
        if (!ConfigurationManager.getBooleanProperty(SubmissionLookupService.CFG_MODULE, "remoteservice.demo"))
        {
            GetMethod method = null;
            try
            {
                HttpClient client = new HttpClient();
                client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
                if (StringUtils.isNotBlank(proxyHost)
                        && StringUtils.isNotBlank(proxyPort))
                {
                    HostConfiguration hostCfg = client.getHostConfiguration();
                    hostCfg.setProxy(proxyHost, Integer.parseInt(proxyPort));
                    client.setHostConfiguration(hostCfg);
                }
                
                int start =0;
                boolean lastPageReached= false;
                while(!lastPageReached){
                        // open session
		                method = new GetMethod(ENDPOINT_SEARCH_SCOPUS + "?httpAccept=application/xml&apiKey="+ apiKey +"&view=COMPLETE&start="+start+"&query="+URLEncoder.encode(query));
		
		                // Execute the method.
		                int statusCode = client.executeMethod(method);
		                
		                if (statusCode != HttpStatus.SC_OK)
		                {
		                    throw new RuntimeException("WS call failed: "
		                            + statusCode);
		                }
		
		                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		                factory.setValidating(false);
		                factory.setIgnoringComments(true);
		                factory.setIgnoringElementContentWhitespace(true);

		                DocumentBuilder builder;
		                try {
		                    builder = factory.newDocumentBuilder();

		                    InputStream responseBodyAsStream = method.getResponseBodyAsStream();
		                    
		                    Document inDoc = builder.parse(responseBodyAsStream);

		                    Element xmlRoot = inDoc.getDocumentElement();
		                    
		                    List<Element> pages = XMLUtils.getElementList(xmlRoot,
		            				"link");
		                    lastPageReached=true;
		                    for(Element page: pages){
		                    	String refPage = page.getAttribute("ref");
		                    	if(StringUtils.equalsIgnoreCase(refPage, "next")){
		                    		lastPageReached= false;
		                    		break;
		                    	}
		                    }
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
		                
		                start+=itemPerPage;
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
            query.append("EID(").append(eid).append(")");
        }
        return search(query.toString());
    }
}
