/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

import gr.ekt.bte.core.Record;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

/**
 * @author rfazio
 */
public class ADSService {
    private static final String END_POINT = "https://api.adsabs.harvard.edu/v1/search/query";
    private static final String RESULT_FIELD_LIST = "abstract,ack,aff,alternate_bibcode,alternate_title,arxiv_class," +
            "author,bibcode,bibgroup,bibstem,citation_count,copyright,database,doi,doctype,first_author,grant,id," +
            "indexstamp,issue,keyword,lang,orcid_pub,orcid_user,orcid_other,page,property,pub,pubdate,read_count," +
            "title,vizier,volume,year";
    private static Logger log = Logger.getLogger(ADSService.class);


    private int timeout = 1000;

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Record getADSBibcode(String bibcode, String token) throws HttpException,
            IOException, ParserConfigurationException, SAXException, URISyntaxException {
        List<String> ids = new ArrayList<String>();
        ids.add(bibcode.trim());
        List<Record> items = getByADSBibcodes(ids, token);
        if (items != null && items.size() > 0) {
            return items.get(0);
        }
        return null;
    }

    public List<Record> search(String title, String author, int year, String token) {
        String query = "";

        if (StringUtils.isNotBlank(title)) {

            query += "title:" + title;
        }
        if (StringUtils.isNotBlank(author)) {
            String splitRegex = "(\\s*,\\s+|\\s*;\\s+|\\s*;+|\\s*,+|\\s+)";
            String[] authors = author.split(splitRegex);
            // [FAU]
            if (StringUtils.isNotBlank(query)) {
                query = "author:";
            } else {
                query += "&fq=author:";
            }
            int x = 0;
            for (String auth : authors) {
                x++;
                query += auth;
                if (x < authors.length) {
                    query += " AND ";
                }
            }
        }
        if (year != -1) {
            // [DP]
            if (StringUtils.isNotBlank(query)) {
                query = "year:";
            } else {
                query += "&fq=year:";
            }
            query += year;
        }
        return search(query.toString(), token);
    }

    public List<Record> search(String query, String token) {
        List<Record> adsResults = new ArrayList<>();
        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
        String proxyPort = ConfigurationManager.getProperty("http.proxy.port");
        if (!ConfigurationManager.getBooleanProperty(SubmissionLookupService.CFG_MODULE, "remoteservice.demo")) {
            HttpGet method = null;
            HttpHost proxy = null;
            try {
                HttpClient client = new DefaultHttpClient();
                client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);


                URIBuilder uriBuilder = new URIBuilder(
                        END_POINT);
                uriBuilder.addParameter("q", query);
                uriBuilder.addParameter("rows", "1000");
                uriBuilder.addParameter("fl", RESULT_FIELD_LIST);

                method = new HttpGet(uriBuilder.build());
                method.setHeader("Authorization", "Bearer:" + token);
                if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
                    proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                    client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                }
                // Execute the method.
                HttpResponse response = client.execute(method);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                InputStream is = response.getEntity().getContent();
                StringWriter writer = new StringWriter();
                IOUtils.copy(is, writer, "UTF-8");
                String source = writer.toString();
                JSONObject obj = new JSONObject(source);
                JSONObject res = obj.getJSONObject("response");
                JSONArray docs = res.getJSONArray("docs");

                for (int i = 0; i < docs.length(); i++) {
                    Record rec = ADSUtils.convertADSDocumentToRecord(docs.getJSONObject(i));
                    if (rec != null) {
                        adsResults.add(rec);
                    }
                }

                //SolrResponse solrResponse = SolrResponse.deserialize(response.toString());

                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException("WS call failed: "
                            + statusLine);
                }
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }

        } else {
            InputStream stream = null;

            File file = new File(
                    ConfigurationManager.getProperty("dspace.dir")
                            + "/config/crosswalks/demo/ads-search.xml");
        }
        return adsResults;
    }


    public List<Record> getByADSBibcodes(List<String> bibcodes, String token) throws URISyntaxException {

        StringBuffer query = new StringBuffer();
        query.append("q=");
        int x = 0;
        for (String bibcode : bibcodes) {
            x++;
            query.append("bibcode:").append(bibcode);
            if (x < bibcodes.size()) {
                query.append(" OR ");
            }
        }

        return search(query.toString(), token);
    }


    public List<Record> search(String doi, String pmid, String token) throws URISyntaxException {
        StringBuffer query = new StringBuffer();
        if (StringUtils.isNotBlank(doi)) {
            query.append("DOI:");
            query.append(doi);
        }
        if (StringUtils.isNotBlank(pmid)) {
            if (query.length() > 0) {
                query.append(" OR ");
            }
            query.append("EXT_ID:").append(pmid);
        }
        return search(query.toString(), token);
    }
}
