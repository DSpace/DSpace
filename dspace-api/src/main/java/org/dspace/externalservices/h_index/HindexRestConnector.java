/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices.h_index;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
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
 * This class deals with logic management to connect to the H-Index outdoor service
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class HindexRestConnector {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(HindexRestConnector.class);

    private String url;
    private String apiKey;
    private String insttoken;
    private Boolean enhanced;

    private HttpClient httpClient;

    @PostConstruct
    private void setup() {
        this.httpClient = HttpClientBuilder.create()
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();
    }

    public InputStream get(String id) {
        try {
            return sendRequest(id);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private InputStream sendRequest(String id)
            throws UnsupportedEncodingException, IOException, ClientProtocolException {
        StringBuilder requestUrl = new StringBuilder(url);
        requestUrl.append(id);
        if (!Objects.isNull(enhanced) && enhanced == true) {
            requestUrl.append("?view=ENHANCED");
        } else {
            System.out.println("The ENHANCED param must be valued with true");
            return null;
        }
        HttpGet httpGet = new HttpGet(requestUrl.toString());
        httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
        httpGet.setHeader("Connection", "keep-alive");
        httpGet.setHeader("X-ELS-APIKey", apiKey);
        httpGet.setHeader("X-ELS-Insttoken", insttoken);
        httpGet.setHeader("Accept", "application/json");

        HttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            return null;
        }
        return response.getEntity().getContent();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getInsttoken() {
        return insttoken;
    }

    public void setInsttoken(String insttoken) {
        this.insttoken = insttoken;
    }

    public boolean isEnhanced() {
        return enhanced;
    }

    public void setEnhanced(Boolean enhanced) {
        this.enhanced = enhanced;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}