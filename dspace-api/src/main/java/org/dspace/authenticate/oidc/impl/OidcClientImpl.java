/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.oidc.impl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.dspace.authenticate.oidc.OidcClient;
import org.dspace.authenticate.oidc.OidcClientException;
import org.dspace.authenticate.oidc.model.OidcTokenResponseDTO;
import org.dspace.services.ConfigurationService;
import org.dspace.util.ThrowingSupplier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OidcClient}.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OidcClientImpl implements OidcClient {

    @Autowired
    private ConfigurationService configurationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void setup() {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public OidcTokenResponseDTO getAccessToken(String code) throws OidcClientException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("client_id", getClientId()));
        params.add(new BasicNameValuePair("client_secret", getClientSecret()));
        params.add(new BasicNameValuePair("redirect_uri", getRedirectUrl()));

        HttpUriRequest httpUriRequest = RequestBuilder.post(getTokenEndpointUrl())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .setEntity(new UrlEncodedFormEntity(params, Charset.defaultCharset()))
            .build();

        return executeAndParseJson(httpUriRequest, OidcTokenResponseDTO.class);

    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String accessToken) throws OidcClientException {

        HttpUriRequest httpUriRequest = RequestBuilder.get(getUserInfoEndpointUrl())
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();

        return executeAndParseJson(httpUriRequest, Map.class);
    }

    private <T> T executeAndParseJson(HttpUriRequest httpUriRequest, Class<T> clazz) {

        HttpClient client = HttpClientBuilder.create().build();

        return executeAndReturns(() -> {

            HttpResponse response = client.execute(httpUriRequest);

            if (isNotSuccessfull(response)) {
                throw new OidcClientException(getStatusCode(response), formatErrorMessage(response));
            }

            return objectMapper.readValue(getContent(response), clazz);

        });

    }

    private <T> T executeAndReturns(ThrowingSupplier<T, Exception> supplier) {
        try {
            return supplier.get();
        } catch (OidcClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OidcClientException(ex);
        }
    }

    private String formatErrorMessage(HttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
        } catch (UnsupportedOperationException | IOException e) {
            return "Generic error";
        }
    }

    private boolean isNotSuccessfull(HttpResponse response) {
        int statusCode = getStatusCode(response);
        return statusCode < 200 || statusCode > 299;
    }

    private int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    private String getContent(HttpResponse response) throws UnsupportedOperationException, IOException {
        HttpEntity entity = response.getEntity();
        return entity != null ? IOUtils.toString(entity.getContent(), UTF_8.name()) : null;
    }

    private String getClientId() {
        return configurationService.getProperty("authentication-oidc.client-id");
    }

    private String getClientSecret() {
        return configurationService.getProperty("authentication-oidc.client-secret");
    }

    private String getTokenEndpointUrl() {
        return configurationService.getProperty("authentication-oidc.token-endpoint");
    }

    private String getUserInfoEndpointUrl() {
        return configurationService.getProperty("authentication-oidc.user-info-endpoint");
    }

    private String getRedirectUrl() {
        return configurationService.getProperty("authentication-oidc.redirect-url");
    }

}
