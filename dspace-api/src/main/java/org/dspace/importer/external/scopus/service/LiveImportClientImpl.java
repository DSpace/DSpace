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
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link LiveImportClient}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science dot com)
 */
public class LiveImportClientImpl implements LiveImportClient {

    private final static Logger log = LogManager.getLogger();

    private CloseableHttpClient httpClient;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public String executeHttpGetRequest(int timeout, String URL, Map<String, String> requestParams) {
        HttpGet method = null;
        try (CloseableHttpClient httpClient = Optional.ofNullable(this.httpClient)
                                                      .orElseGet(HttpClients::createDefault)) {

            Builder requestConfigBuilder = RequestConfig.custom();
            requestConfigBuilder.setConnectionRequestTimeout(timeout);
            RequestConfig defaultRequestConfig = requestConfigBuilder.build();

            method = new HttpGet(getSearchUrl(URL, requestParams));
            method.setConfig(defaultRequestConfig);

            configureProxy(method, defaultRequestConfig);

            HttpResponse httpResponse = httpClient.execute(method);
            if (isNotSuccessfull(httpResponse)) {
                throw new RuntimeException();
            }
            InputStream inputStream = httpResponse.getEntity().getContent();
            return IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
        } finally {
            if (Objects.nonNull(method)) {
                method.releaseConnection();
            }
        }
        return StringUtils.EMPTY;
    }

    private void configureProxy(HttpGet method, RequestConfig defaultRequestConfig) {
        String proxyHost = configurationService.getProperty("http.proxy.host");
        String proxyPort = configurationService.getProperty("http.proxy.port");
        if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
            RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig)
                    .setProxy(new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http"))
                    .build();
            method.setConfig(requestConfig);
        }
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

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

}