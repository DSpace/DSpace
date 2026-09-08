/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Utility class for Orcid factory classes. This is used to parse the
 * configuration of ORCID entities defined in orcid.cfg (for example see
 * contributors and external ids configuration).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public final class OrcidFactoryUtils {

    private static final Logger log = LogManager.getLogger(OrcidFactoryUtils.class);

    private OrcidFactoryUtils() { }

    /**
     * Parse the given configurations value and returns a map with metadata fields
     * as keys and types/sources as values. The expected configuration syntax is a
     * list of values field::type separated by commas.
     *
     * @param  configurations the configurations to parse
     * @return                the configurations parsing result as map
     */
    public static Map<String, String> parseConfigurations(String configurations) {
        Map<String, String> configurationMap = new HashMap<>();
        if (StringUtils.isBlank(configurations)) {
            return configurationMap;
        }

        for (String configuration : configurations.split(",")) {
            String[] configurationSections = parseConfiguration(configuration);
            configurationMap.put(configurationSections[0], configurationSections[1]);
        }
        return configurationMap;
    }

    /**
     * Parse the given configuration value and returns it's section. The expected
     * configuration syntax is field::type.
     *
     * @param  configuration         the configuration to parse
     * @return                       the configuration sections
     * @throws IllegalStateException if the given configuration is not valid
     */
    private static String[] parseConfiguration(String configuration) {
        String[] configurations = configuration.split("::");
        if (configurations.length != 2) {
            throw new IllegalStateException(
                "The configuration '" + configuration + "' is not valid. Expected field::type");
        }
        return configurations;
    }

    /**
     * Retrieve access token from ORCID, given a client ID, client secret and OAuth URL
     *
     * @param clientId ORCID client ID
     * @param clientSecret ORCID client secret
     * @param oauthUrl ORCID oauth redirect URL
     * @return response object as Optional string
     * @throws IOException if any errors are encountered making the connection or reading a response
     */
    public static Optional<String> retrieveAccessToken(String clientId, String clientSecret, String oauthUrl)
            throws IOException {
        if (StringUtils.isBlank(clientSecret) || StringUtils.isBlank(clientId) || StringUtils.isBlank(oauthUrl)) {
            String missingParams = (StringUtils.isBlank(clientId) ? "clientId " : "") +
                                   (StringUtils.isBlank(clientSecret) ? "clientSecret " : "") +
                                   (StringUtils.isBlank(oauthUrl) ? "oauthUrl" : "");
            log.error("Cannot retrieve ORCID access token: missing required parameters:{} ", missingParams.trim());
            return Optional.empty();
        }

        HttpPost httpPost = new HttpPost(oauthUrl);

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(UTF_8));
        addHeaders(httpPost, encodedAuth);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        params.add(new BasicNameValuePair("scope", "/read-public"));
        httpPost.setEntity(new UrlEncodedFormEntity(params, UTF_8));

        try (CloseableHttpClient httpClient = DSpaceHttpClientFactory.getInstance().build()) {
            log.debug("Sending ORCID token request to {}", oauthUrl);
            HttpResponse response = httpClient.execute(httpPost);
            if (!isSuccessful(response)) {
                log.error("Failed to retrieve ORCID access token");
                return Optional.empty();
            }
            // Parsing JSON response
            try (InputStream is = response.getEntity().getContent()) {
                JSONObject responseObject = new JSONObject(new JSONTokener(is));
                if (responseObject.has("access_token")) {
                    String token = responseObject.getString("access_token");
                    log.debug("Successfully retrieved ORCID access token");
                    return Optional.of(token);
                } else {
                    log.error("ORCID response missing access_token field:{} ", responseObject);
                    return Optional.empty();
                }
            }
        }
    }

    private static void addHeaders(HttpPost httpPost, String encodedAuth) {
        httpPost.addHeader("Authorization", "Basic " + encodedAuth);
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
    }

    private static boolean isSuccessful(HttpResponse response) {
        if (response == null) {
            log.error("ORCID API request failed: null response received");
            return false;
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            var errorMsg = "ORCID API request failed with status code {}: {}";
            log.error(errorMsg, statusCode, response.getStatusLine().getReasonPhrase());
            return false;
        }
        return true;
    }

}
