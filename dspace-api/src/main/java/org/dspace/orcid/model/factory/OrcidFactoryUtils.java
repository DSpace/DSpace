/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

/**
 * Utility class for Orcid factory classes. This is used to parse the
 * configuration of ORCID entities defined in orcid.cfg (for example see
 * contributors and external ids configuration).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class OrcidFactoryUtils {

    private OrcidFactoryUtils() {

    }

    /**
     * Parse the given configurations value and returns a map with metadata fields
     * as keys and types/sources as values. The expected configuration syntax is a
     * list of values field::type separated by commas.
     *
     * @param  configurations the configurations to parse
     * @return                the configurations parsing result as map
     */
    public static Map<String, String> parseConfigurations(String configurations) {
        Map<String, String> configurationMap = new HashMap<String, String>();
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
        if (StringUtils.isNotBlank(clientSecret) && StringUtils.isNotBlank(clientId)
                && StringUtils.isNotBlank(oauthUrl)) {
            String authenticationParameters = "?client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&scope=/read-public&grant_type=client_credentials";
            HttpPost httpPost = new HttpPost(oauthUrl + authenticationParameters);
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

            HttpResponse response;
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                response = httpClient.execute(httpPost);
            }
            JSONObject responseObject = null;
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                try (InputStream is = response.getEntity().getContent();
                     BufferedReader streamReader = new BufferedReader(new InputStreamReader(is,
                             StandardCharsets.UTF_8))) {
                    String inputStr;
                    while ((inputStr = streamReader.readLine()) != null && responseObject == null) {
                        if (inputStr.startsWith("{") && inputStr.endsWith("}") && inputStr.contains("access_token")) {
                            responseObject = new JSONObject(inputStr);
                        }
                    }
                }
            }
            if (responseObject != null && responseObject.has("access_token")) {
                return Optional.of((String) responseObject.get("access_token"));
            }
        }
        // Return empty by default
        return Optional.empty();
    }
}
