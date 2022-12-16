/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.xml.bind.JAXBException;

import eu.openaire.jaxb.helper.OpenAIREHandler;
import eu.openaire.jaxb.model.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.Util;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * based on OrcidRestConnector it's a rest connector for OpenAIRE API providing
 * ways to perform searches and token grabbing
 * 
 * @author paulo-graca
 *
 */
public class OpenAIRERestConnector {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(OpenAIRERestConnector.class);

    /**
     * OpenAIRE API Url
     *  and can be configured with: openaire.api.url
     */
    private String url = "https://api.openaire.eu";

    /**
     * Boolean with token usage definition true if we want to use a token
     *  and can be configured with: openaire.token.enabled
     */
    boolean tokenEnabled = false;

    /**
     * OpenAIRE Authorization and Authentication Token Service URL
     *  and can be configured with: openaire.token.url
     */
    private String tokenServiceUrl;

    /**
     * OpenAIRE clientId
     *  and can be configured with: openaire.token.clientId
     */
    private String clientId;

    /**
     * OpenAIRERest access token
     */
    private OpenAIRERestToken accessToken;

    /**
     * OpenAIRE clientSecret
     *  and can be configured with: openaire.token.clientSecret
     */
    private String clientSecret;


    public OpenAIRERestConnector(String url) {
        this.url = url;
    }


    /**
     * This method grabs an accessToken an sets the expiration time Based.<br/>
     * Based on https://develop.openaire.eu/basic.html
     * 
     * @throws IOException
     */
    public OpenAIRERestToken grabNewAccessToken() throws IOException {

        if (StringUtils.isBlank(tokenServiceUrl) || StringUtils.isBlank(clientId)
                || StringUtils.isBlank(clientSecret)) {
            throw new IOException("Cannot grab OpenAIRE token with nulls service url, client id or secret");
        }

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);

        HttpPost httpPost = new HttpPost(tokenServiceUrl);
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("User-Agent", "DSpace/" + Util.getSourceVersion());
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(1);
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse getResponse = httpClient.execute(httpPost);

        JSONObject responseObject = null;
        try (InputStream is = getResponse.getEntity().getContent();
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String inputStr;
            // verify if we have basic json
            while ((inputStr = streamReader.readLine()) != null && responseObject == null) {
                if (inputStr.startsWith("{") && inputStr.endsWith("}") && inputStr.contains("access_token")
                        && inputStr.contains("expires_in")) {
                    try {
                        responseObject = new JSONObject(inputStr);
                    } catch (Exception e) {
                        // Not as valid as I'd hoped, move along
                        responseObject = null;
                    }
                }
            }
        }
        if (responseObject == null || !responseObject.has("access_token") || !responseObject.has("expires_in")) {
            throw new IOException("Unable to grab the access token using provided service url, client id and secret");
        }

        return new OpenAIRERestToken(responseObject.get("access_token").toString(),
                Long.valueOf(responseObject.get("expires_in").toString()));

    }

    /**
     * Perform a GET request to the OpenAIRE API
     * 
     * @param file
     * @param accessToken
     * @return an InputStream with a Result
     */
    public InputStream get(String file, String accessToken) {
        HttpResponse getResponse = null;
        InputStream result = null;
        file = trimSlashes(file);

        try {
            URL fullPath = new URL(url + '/' + file);

            log.debug("Requesting: " + fullPath.toString());

            HttpGet httpGet = new HttpGet(fullPath.toURI());
            if (StringUtils.isNotBlank(accessToken)) {
                httpGet.addHeader("Authorization", "Bearer " + accessToken);
            }

            HttpClient httpClient = HttpClientBuilder.create().build();
            getResponse = httpClient.execute(httpGet);

            StatusLine status = getResponse.getStatusLine();

            // registering errors
            switch (status.getStatusCode()) {
                case HttpStatus.SC_NOT_FOUND:
                    // 404 - Not found
                case HttpStatus.SC_FORBIDDEN:
                    // 403 - Invalid Access Token
                case 429:
                    // 429 - Rate limit abuse for unauthenticated user
                    Header[] limitUsed = getResponse.getHeaders("x-ratelimit-used");
                    Header[] limitMax = getResponse.getHeaders("x-ratelimit-limit");

                    if (limitUsed.length > 0) {
                        String limitMsg = limitUsed[0].getValue();
                        if (limitMax.length > 0) {
                            limitMsg = limitMsg.concat(" of " + limitMax[0].getValue());
                        }
                        getGotError(
                                new NoHttpResponseException(status.getReasonPhrase() + " with usage limit " + limitMsg),
                                url + '/' + file);
                    } else {
                        // 429 - Rate limit abuse
                        getGotError(new NoHttpResponseException(status.getReasonPhrase()), url + '/' + file);
                    }
                    break;
                default:
                    // 200 or other
                    break;
            }

            // do not close this httpClient
            result = getResponse.getEntity().getContent();
        } catch (MalformedURLException e1) {
            getGotError(e1, url + '/' + file);
        } catch (Exception e) {
            getGotError(e, url + '/' + file);
        }

        return result;
    }

    /**
     * Perform an OpenAIRE Project Search By Keywords
     * 
     * @param page
     * @param size
     * @param keywords
     * @return OpenAIRE Response
     */
    public Response searchProjectByKeywords(int page, int size, String... keywords) {
        String path = "search/projects?keywords=" + String.join("+", keywords);
        return search(path, page, size);
    }

    /**
     * Perform an OpenAIRE Project Search By ID and by Funder
     * 
     * @param projectID
     * @param projectFunder
     * @param page
     * @param size
     * @return OpenAIRE Response
     */
    public Response searchProjectByIDAndFunder(String projectID, String projectFunder, int page, int size) {
        String path = "search/projects?grantID=" + projectID + "&funder=" + projectFunder;
        return search(path, page, size);
    }

    /**
     * Perform an OpenAIRE Search request
     * 
     * @param path
     * @param page
     * @param size
     * @return OpenAIRE Response
     */
    public Response search(String path, int page, int size) {
        String[] queryStringPagination = { "page=" + page, "size=" + size };

        String queryString = path + ((path.indexOf("?") > 0) ? "&" : "?") + String.join("&", queryStringPagination);

        InputStream result = null;
        if (tokenEnabled) {
            try {
                if (accessToken == null) {
                    accessToken = this.grabNewAccessToken();
                } else if (!accessToken.isValidToken()) {
                    accessToken = this.grabNewAccessToken();
                }

                result = get(queryString, accessToken.getToken());
            } catch (IOException e) {
                log.error("Error grabbing the token: " + e.getMessage());
                getGotError(e, path);
            }
        } else {
            result = get(queryString, null);
        }

        if (result != null) {
            try {
                return OpenAIREHandler.unmarshal(result);
            } catch (JAXBException e) {
                log.error("Error extracting result from request: " + queryString);
                getGotError(e, path);
            }
        }
        return null;
    }

    /**
     * trim slashes from the path
     * 
     * @param path
     * @return string path without trailing slashes
     */
    public static String trimSlashes(String path) {
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    /**
     * stores clientId to grab the token
     * 
     * @param clientId
     */
    @Autowired(required = false)
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * stores tokenServiceUrl to grab the token
     * 
     * @param tokenServiceUrl
     */
    @Autowired(required = false)
    public void setTokenServiceUrl(String tokenServiceUrl) {
        this.tokenServiceUrl = tokenServiceUrl;
    }

    /**
     * stores clientSecret to grab the token
     * 
     * @param clientSecret
     */
    @Autowired(required = false)
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * tokenUsage true to enable the usage of an access token
     * 
     * @param tokenEnabled true/false
     */
    @Autowired(required = false)
    public void setTokenEnabled(boolean tokenEnabled) {
        this.tokenEnabled = tokenEnabled;
    }

    protected void getGotError(Exception e, String fullPath) {
        log.error("Error in rest connector for path: " + fullPath, e);
    }
}
