/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.liveimportclient.service;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link LiveImportClient}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science dot com)
 */
public class LiveImportClientImpl implements LiveImportClient {

    private final static Logger log = LogManager.getLogger();

    public static final String URI_PARAMETERS = "uriParameters";
    public static final String HEADER_PARAMETERS = "headerParameters";

    private CloseableHttpClient httpClient;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public String executeHttpGetRequest(int timeout, String URL, Map<String, Map<String, String>> params) {
        HttpGet method = null;
        RequestConfig config = RequestConfig.custom()
            .setConnectionRequestTimeout(timeout)
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();
        try (CloseableHttpClient httpClient = Optional.ofNullable(this.httpClient)
                                        .orElse(DSpaceHttpClientFactory.getInstance().buildWithRequestConfig(config))) {
            String uri = buildUrl(URL, params.get(URI_PARAMETERS));
            method = new HttpGet(uri);

            Map<String, String> headerParams = params.get(HEADER_PARAMETERS);
            if (MapUtils.isNotEmpty(headerParams)) {
                for (String param : headerParams.keySet()) {
                    method.setHeader(param, headerParams.get(param));
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Performing GET request to \"" + uri + "\"...");
            }
            HttpResponse httpResponse = httpClient.execute(method);
            if (isNotSuccessfull(httpResponse)) {
                throw new RuntimeException("The request failed with: " + getStatusCode(httpResponse) + " code, reason= "
                                           + httpResponse.getStatusLine().getReasonPhrase());
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

    @Override
    public String executeHttpPostRequest(String URL, Map<String, Map<String, String>> params, String entry) {
        HttpPost method = null;
        RequestConfig config = RequestConfig.custom().build();
        try (CloseableHttpClient httpClient = Optional.ofNullable(this.httpClient)
                .orElse(DSpaceHttpClientFactory.getInstance().buildWithRequestConfig(config))) {

            String uri = buildUrl(URL, params.get(URI_PARAMETERS));
            method = new HttpPost(uri);
            if (StringUtils.isNotBlank(entry)) {
                method.setEntity(new StringEntity(entry));
            }
            setHeaderParams(method, params);

            if (log.isDebugEnabled()) {
                log.debug("Performing POST request to \"" + uri + "\"..." );
            }
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

    /**
     * Allows to set the header parameters to the HTTP Post method
     * 
     * @param method  HttpPost method
     * @param params  This map contains the header params to be included in the request.
     */
    private void setHeaderParams(HttpPost method, Map<String, Map<String, String>> params) {
        Map<String, String> headerParams = params.get(HEADER_PARAMETERS);
        if (MapUtils.isNotEmpty(headerParams)) {
            for (String param : headerParams.keySet()) {
                method.setHeader(param, headerParams.get(param));
            }
        }
    }

    /**
     * This method allows you to add the parameters contained in the requestParams map to the URL
     * 
     * @param URL                   URL
     * @param requestParams         This map contains the parameters to be included in the request.
     *                              Each parameter will be added to the url?(key=value)
     * @return
     * @throws URISyntaxException
     */
    private String buildUrl(String URL, Map<String, String> requestParams) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(URL);
        if (MapUtils.isNotEmpty(requestParams)) {
            for (String param : requestParams.keySet()) {
                uriBuilder.setParameter(param, requestParams.get(param));
            }
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
