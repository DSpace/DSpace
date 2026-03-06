/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.app.sherpa.v2.SHERPAPublisherResponse;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.app.sherpa.v2.SHERPAUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

/**
 * SHERPAService is responsible for making the HTTP call to the Jisc Open Policy Finder API
 * (formerly SHERPA/RoMEO v2) for SHERPASubmitService.
 * Note, this service is ported from DSpace 6 for the ability to search policies by ISSN
 * There are also new DataProvider implementations provided for use as 'external sources'
 * of journal and publisher data
 * @see org.dspace.external.provider.impl.SHERPAv2JournalDataProvider
 * @see org.dspace.external.provider.impl.SHERPAv2PublisherDataProvider
 * @author Kim Shepherd
 */
public class SHERPAService {

    private int maxNumberOfTries;
    private long sleepBetweenTimeouts;
    private int timeout = 5000;
    private String endpoint = null;
    private String apiKey = null;

    /** log4j category */
    private static final Logger log = LogManager.getLogger(SHERPAService.class);

    @Autowired
    ConfigurationService configurationService;

    /**
     * Complete initialization of the Bean.
     */
    @SuppressWarnings("unused")
    @PostConstruct
    private void init() {
        // Get endpoint and API key from configuration, with fallback to legacy property names
        String newUrl = configurationService.getProperty("openpolicyfinder.url");
        String legacyUrl = configurationService.getProperty("sherpa.romeo.url");
        if (newUrl != null) {
            endpoint = newUrl;
        } else if (legacyUrl != null) {
            endpoint = legacyUrl;
            log.warn("Configuration property 'sherpa.romeo.url' is deprecated. "
                + "Please use 'openpolicyfinder.url' instead.");
        } else {
            endpoint = "https://api.openpolicyfinder.jisc.ac.uk/retrieve";
        }

        String newApiKey = configurationService.getProperty("openpolicyfinder.apikey");
        String legacyApiKey = configurationService.getProperty("sherpa.romeo.apikey");
        if (newApiKey != null) {
            apiKey = newApiKey;
        } else if (legacyApiKey != null) {
            apiKey = legacyApiKey;
            log.warn("Configuration property 'sherpa.romeo.apikey' is deprecated. "
                + "Please use 'openpolicyfinder.apikey' instead.");
        }
    }

    /**
     * Search the Open Policy Finder API for journal policy data using the supplied ISSN.
     * If the API key is missing, or the HTTP response is non-OK or does not complete
     * successfully, a simple error response will be returned.
     * Otherwise, the response body will be passed to SHERPAResponse for parsing as JSON
     * and the final result returned to the calling method
     * @param query ISSN string to pass in an "issn equals" API query
     * @return      SHERPAResponse containing an error or journal policies
     */
    @Cacheable(key = "#query", condition = "#query != null", cacheNames = "sherpa.searchByJournalISSN")
    public SHERPAResponse searchByJournalISSN(String query) {
        return performRequest("publication", "issn", "equals", query, 0, 1);
    }

    /**
     * Perform an API request to the Open Policy Finder API - this could be a search or a get for any entity type
     * but the return object here must be a SHERPAPublisherResponse not the journal-centric SHERPAResponse
     * For more information about the type, field and predicate arguments, see the API documentation
     * @param type          entity type eg "publisher"
     * @param field         field eg "issn" or "title"
     * @param predicate     predicate eg "equals" or "contains-word"
     * @param value         the actual value to search for (eg an ISSN or partial title)
     * @param start         start / offset of search results
     * @param limit         maximum search results to return
     * @return              SHERPAPublisherResponse object
     */
    public SHERPAPublisherResponse performPublisherRequest(String type, String field, String predicate, String value,
                                                           int start, int limit) {
        // API Key is *required* for API calls
        if (null == apiKey) {
            log.error("Open Policy Finder API Key missing: "
                + "please register for an API key and set openpolicyfinder.apikey");
            return new SHERPAPublisherResponse("Open Policy Finder configuration invalid or missing");
        }

        HttpGet method = null;
        SHERPAPublisherResponse sherpaResponse = null;
        int numberOfTries = 0;

        while (numberOfTries < maxNumberOfTries && sherpaResponse == null) {
            numberOfTries++;

            log.debug(String.format(
                "Trying to contact Open Policy Finder - attempt %d of %d; timeout is %d; "
                    + "sleep between timeouts is %d",
                numberOfTries,
                maxNumberOfTries,
                timeout,
                sleepBetweenTimeouts));

            try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().buildWithoutAutomaticRetries(5)) {
                Thread.sleep(sleepBetweenTimeouts);

                // Construct a default HTTP method (first result)
                method = constructHttpGet(type, field, predicate, value, start, limit);

                // Execute the method
                try (CloseableHttpResponse response = client.execute(method)) {
                    int statusCode = response.getStatusLine().getStatusCode();

                    log.debug(response.getStatusLine().getStatusCode() + ": "
                            + response.getStatusLine().getReasonPhrase());

                    if (statusCode != HttpStatus.SC_OK) {
                        sherpaResponse = new SHERPAPublisherResponse(
                            "Open Policy Finder return not OK status: " + statusCode);
                        String errorBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                        log.error("Error from Open Policy Finder HTTP request: " + errorBody);
                    }

                    HttpEntity responseBody = response.getEntity();

                    // If the response body is valid, pass to SHERPAResponse for parsing as JSON
                    if (null != responseBody) {
                        log.debug("Non-null response received for query of " + value);
                        InputStream content = null;
                        try {
                            content = responseBody.getContent();
                            sherpaResponse =
                                    new SHERPAPublisherResponse(content, SHERPAPublisherResponse.SHERPAFormat.JSON);
                        } catch (IOException e) {
                            log.error("Encountered exception while contacting Open Policy Finder: "
                                + e.getMessage(), e);
                        } finally {
                            if (content != null) {
                                content.close();
                            }
                        }
                    } else {
                        log.debug("Empty response body for query on " + value);
                        sherpaResponse = new SHERPAPublisherResponse("Open Policy Finder returned no response");
                    }
                }
            } catch (URISyntaxException e) {
                String errorMessage = "Error building Open Policy Finder API URI: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAPublisherResponse(errorMessage);
            } catch (IOException e) {
                String errorMessage = "Encountered exception while contacting Open Policy Finder: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAPublisherResponse(errorMessage);
            }  catch (InterruptedException e) {
                String errorMessage = "Encountered exception while sleeping thread: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAPublisherResponse(errorMessage);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
        }

        if (sherpaResponse == null) {
            log.debug("Response is still null");
            sherpaResponse = new SHERPAPublisherResponse(
                "Error processing the Open Policy Finder answer");
        }

        // Return the final response
        return sherpaResponse;
    }

    /**
     * Perform an API request to the Open Policy Finder API - this could be a search or a get for any entity type
     * For more information about the type, field and predicate arguments, see the API documentation
     * @param type          entity type eg "publication" or "publisher"
     * @param field         field eg "issn" or "title"
     * @param predicate     predicate eg "equals" or "contains-word"
     * @param value         the actual value to search for (eg an ISSN or partial title)
     * @param start         start / offset of search results
     * @param limit         maximum search results to return
     * @return              SHERPAResponse object
     */
    public SHERPAResponse performRequest(String type, String field, String predicate, String value,
                                         int start, int limit) {
        // API Key is *required* for API calls
        if (null == apiKey) {
            log.error("Open Policy Finder API Key missing: "
                + "please register for an API key and set openpolicyfinder.apikey");
            return new SHERPAResponse("Open Policy Finder configuration invalid or missing");
        }

        HttpGet method = null;
        SHERPAResponse sherpaResponse = null;
        int numberOfTries = 0;

        while (numberOfTries < maxNumberOfTries && sherpaResponse == null) {
            numberOfTries++;

            log.debug(String.format(
                "Trying to contact Open Policy Finder - attempt %d of %d; timeout is %d; "
                    + "sleep between timeouts is %d",
                numberOfTries,
                maxNumberOfTries,
                timeout,
                sleepBetweenTimeouts));

            try (CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().buildWithoutAutomaticRetries(5)) {
                Thread.sleep(sleepBetweenTimeouts);

                // Construct a default HTTP method (first result)
                method = constructHttpGet(type, field, predicate, value, start, limit);

                // Execute the method
                try (CloseableHttpResponse response = client.execute(method)) {
                    int statusCode = response.getStatusLine().getStatusCode();

                    log.debug(response.getStatusLine().getStatusCode() + ": "
                            + response.getStatusLine().getReasonPhrase());

                    if (statusCode != HttpStatus.SC_OK) {
                        sherpaResponse = new SHERPAResponse(
                            "Open Policy Finder return not OK status: " + statusCode);
                        String errorBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                        log.error("Error from Open Policy Finder HTTP request: " + errorBody);
                    }

                    HttpEntity responseBody = response.getEntity();

                    // If the response body is valid, pass to SHERPAResponse for parsing as JSON
                    if (null != responseBody) {
                        log.debug("Non-null response received for query of " + value);
                        InputStream content = null;
                        try {
                            content = responseBody.getContent();
                            sherpaResponse = new SHERPAResponse(content, SHERPAResponse.SHERPAFormat.JSON);
                        } catch (IOException e) {
                            log.error("Encountered exception while contacting Open Policy Finder: "
                                + e.getMessage(), e);
                        } finally {
                            if (content != null) {
                                content.close();
                            }
                        }
                    } else {
                        log.debug("Empty response body for query on " + value);
                        sherpaResponse = new SHERPAResponse("Open Policy Finder returned no response");
                    }
                }
            } catch (URISyntaxException e) {
                String errorMessage = "Error building Open Policy Finder API URI: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAResponse(errorMessage);
            } catch (IOException e) {
                String errorMessage = "Encountered exception while contacting Open Policy Finder: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAResponse(errorMessage);
            } catch (InterruptedException e) {
                String errorMessage = "Encountered exception while sleeping thread: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAResponse(errorMessage);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
        }

        if (sherpaResponse == null) {
            log.debug("Response is still null");
            sherpaResponse = new SHERPAResponse(
                "Error processing the Open Policy Finder answer");
        }

        // Return the final response
        return sherpaResponse;
    }

    /**
     * Construct HTTP GET object for a "field,predicate,value" query with default start, limit
     * eg. "title","contains-word","Lancet" or "issn","equals","1234-1234"
     * @param field the field (issn, title, etc)
     * @param predicate the predicate (contains-word, equals, etc - see API docs)
     * @param value the query value itself
     * @return HttpGet method which can then be executed by the client
     * @throws URISyntaxException if the URL build fails
     */
    public HttpGet constructHttpGet(String type, String field, String predicate, String value)
        throws URISyntaxException {
        return constructHttpGet(type, field, predicate, value, 0, 1);
    }

    /**
     * Construct HTTP GET object for a "field,predicate,value" query
     * eg. "title","contains-word","Lancet" or "issn","equals","1234-1234"
     * @param field the field (issn, title, etc)
     * @param predicate the predicate (contains-word, equals, etc - see API docs)
     * @param value the query value itself
     * @param start row offset
     * @param limit number of results to return
     * @return HttpGet object to be executed by the client
     * @throws URISyntaxException
     */
    public HttpGet constructHttpGet(String type, String field, String predicate, String value, int start, int limit)
        throws URISyntaxException {
        // Sanitise query string (strip some characters) field, predicate and value
        if (null == type) {
            type = "publication";
        }
        field = SHERPAUtils.sanitiseQuery(field);
        predicate = SHERPAUtils.sanitiseQuery(predicate);
        value = SHERPAUtils.sanitiseQuery(value);
        type = SHERPAUtils.sanitiseQuery(type);

        // Build URL based on search query
        URIBuilder uriBuilder = new URIBuilder(endpoint);
        uriBuilder.addParameter("item-type", type);
        uriBuilder.addParameter("filter", "[[\"" + field + "\",\"" + predicate + "\",\"" + value + "\"]]");
        uriBuilder.addParameter("format", "Json");
        // Set optional start (offset) and limit parameters
        if (start >= 0) {
            uriBuilder.addParameter("offset", String.valueOf(start));
        }
        if (limit > 0) {
            uriBuilder.addParameter("limit", String.valueOf(limit));
        }

        log.debug("Open Policy Finder API URL: " + uriBuilder.toString());

        // Create HTTP GET object
        HttpGet method = new HttpGet(uriBuilder.build());

        // Set API key as HTTP header (required by Jisc Open Policy Finder API)
        if (StringUtils.isNotBlank(apiKey)) {
            method.addHeader("x-api-key", apiKey);
        }

        // Set connection parameters
        int timeout = 5000;
        method.setConfig(RequestConfig.custom()
            .setConnectionRequestTimeout(timeout)
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build());

        return method;
    }

    /**
     * Prepare the API query URI for validation purposes.
     * Note: the API key is sent as an HTTP header, not as a query parameter.
     * @param query     ISSN query string
     * @param endpoint  API endpoint (base URL)
     * @param apiKey    API key (unused in URI construction, kept for signature compatibility)
     * @return          URI object
     * @throws URISyntaxException
     */
    public URI prepareQuery(String query, String endpoint, String apiKey) throws URISyntaxException {
        // Sanitise query string
        query = SHERPAUtils.sanitiseQuery(query);

        // Instantiate URI builder
        URIBuilder uriBuilder = new URIBuilder(endpoint);

        // Build URI parameters from supplied values
        uriBuilder.addParameter("item-type", "publication");

        // Log warning if no query is supplied
        if (null == query) {
            log.warn("No ISSN supplied as query string for Open Policy Finder service search");
        }
        uriBuilder.addParameter("filter", "[[\"issn\",\"equals\",\"" + query + "\"]]");
        uriBuilder.addParameter("format", "Json");
        log.debug("Would search Open Policy Finder endpoint with " + uriBuilder.toString());

        // Return final built URI
        return uriBuilder.build();
    }

    public void setMaxNumberOfTries(int maxNumberOfTries) {
        this.maxNumberOfTries = maxNumberOfTries;
    }

    public void setSleepBetweenTimeouts(long sleepBetweenTimeouts) {
        this.sleepBetweenTimeouts = sleepBetweenTimeouts;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

}