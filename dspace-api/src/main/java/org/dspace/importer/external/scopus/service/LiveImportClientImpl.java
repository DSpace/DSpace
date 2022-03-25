/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scopus.service;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link LiveImportClient}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science dot com)
 */
public class LiveImportClientImpl implements LiveImportClient {

    private static final Logger log = Logger.getLogger(LiveImportClientImpl.class);

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public InputStream executeHttpGetRequest(int timeout, String URL, Map<String, String> requestParams) {
        HttpGet method = null;
        String proxyHost = configurationService.getProperty("http.proxy.host");
        String proxyPort = configurationService.getProperty("http.proxy.port");
        try {
            HttpClientBuilder hcBuilder = HttpClients.custom();
            Builder requestConfigBuilder = RequestConfig.custom();
            requestConfigBuilder.setConnectionRequestTimeout(timeout);

            if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
                HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
                DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                hcBuilder.setRoutePlanner(routePlanner);
            }

            method = new HttpGet(getSearchUrl(URL, requestParams));
            method.setConfig(requestConfigBuilder.build());

            HttpClient client = hcBuilder.build();
            HttpResponse httpResponse = client.execute(method);
            if (isNotSuccessfull(httpResponse)) {
                throw new RuntimeException();
            }
            return httpResponse.getEntity().getContent();
        } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
        } finally {
            if (Objects.nonNull(method)) {
                method.releaseConnection();
            }
        }
        return null;
    }

    private String getSearchUrl(String URL, Map<String, String> requestParams) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(URL);
        for (String param : requestParams.keySet()) {
            uriBuilder.setParameter(param, requestParams.get(param));
        }
        return uriBuilder.toString();
    }

    private boolean isNotSuccessfull(HttpResponse response) {
        int statusCode = getStatusCode(response);
        return statusCode < 200 || statusCode > 299;
    }

    private int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

}