/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices.wos;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.Logger;

/**
 * This class deals with logic management to connect to the WOS outdoor service
 *
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 */
public class WOSRestConnector {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(WOSRestConnector.class);

    private String apiKey;
    private String wosUrl;
    private HttpClient httpClient;

    @PostConstruct
    private void setup() {
        this.httpClient = HttpClientBuilder.create()
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();
    }

    public InputStream get(String id) {
        try {
            return sendRequestToWOS(id);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private InputStream sendRequestToWOS(String id)
            throws UnsupportedEncodingException, IOException, ClientProtocolException {
        HttpGet httpPost = new HttpGet(wosUrl.concat("DO=(").concat(id).concat(")&count=10&firstRecord=1"));
        httpPost.setHeader("Accept-Encoding", "gzip, deflate, br");
        httpPost.setHeader("Connection", "keep-alive");
        httpPost.setHeader("X-ApiKey", apiKey);
        httpPost.setHeader("Accept", "application/json");

        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            return null;
        }
        return response.getEntity().getContent();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWosUrl() {
        return wosUrl;
    }

    public void setWosUrl(String wosUrl) {
        this.wosUrl = wosUrl;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

}