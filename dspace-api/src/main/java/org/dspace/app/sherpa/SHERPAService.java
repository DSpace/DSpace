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
import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.sherpa.v2.SHERPAPublisherResponse;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.app.sherpa.v2.SHERPAUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

/**
 * SHERPAService is responsible for making the HTTP call to the SHERPA v2 API
 * for SHERPASubmitService.
 * Note, this service is ported from DSpace 6 for the ability to search policies by ISSN
 * There are also new DataProvider implementations provided for use as 'external sources'
 * of journal and publisher data
 * @see org.dspace.external.provider.impl.SHERPAv2JournalDataProvider
 * @see org.dspace.external.provider.impl.SHERPAv2PublisherDataProvider
 * @author Kim Shepherd
 */
public class SHERPAService {

    private CloseableHttpClient client = null;

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
     * Create a new HTTP builder with sensible defaults in constructor
     */
    public SHERPAService() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        // httpclient 4.3+ doesn't appear to have any sensible defaults any more. Setting conservative defaults as
        // not to hammer the SHERPA service too much.
        client = builder
            .disableAutomaticRetries()
            .setMaxConnTotal(5)
            .build();
    }

    /**
     * Complete initialization of the Bean.
     */
    @SuppressWarnings("unused")
    @PostConstruct
    private void init() {
        // Get endoint and API key from configuration
        endpoint = configurationService.getProperty("sherpa.romeo.url",
            "https://v2.sherpa.ac.uk/cgi/retrieve");
        apiKey = configurationService.getProperty("sherpa.romeo.apikey");
    }

    /**
     * Search the SHERPA v2 API for a journal policy data using the supplied ISSN.
     * If the API key is missing, or the HTTP response is non-OK or does not complete
     * successfully, a simple error response will be returned.
     * Otherwise, the response body will be passed to SHERPAResponse for parsing as JSON
     * and the final result returned to the calling method
     * @param query ISSN string to pass in an "issn equals" API query
     * @return      SHERPAResponse containing an error or journal policies
     */
    @Cacheable(key = "#query", cacheNames = "sherpa.searchByJournalISSN")
    public SHERPAResponse searchByJournalISSN(String query) {
        return performRequest("publication", "issn", "equals", query, 0, 1);
    }

    /**
     * Perform an API request to the SHERPA v2 API - this could be a search or a get for any entity type
     * but the return object here must be a SHERPAPublisherResponse not the journal-centric SHERPAResponse
     * For more information about the type, field and predicate arguments, see the SHERPA v2 API documentation
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
        // API Key is *required* for v2 API calls
        if (null == apiKey) {
            log.error("SHERPA ROMeO API Key missing: please register for an API key and set sherpa.romeo.apikey");
            return new SHERPAPublisherResponse("SHERPA/RoMEO configuration invalid or missing");
        }

        HttpGet method = null;
        SHERPAPublisherResponse sherpaResponse = null;
        int numberOfTries = 0;

        while (numberOfTries < maxNumberOfTries && sherpaResponse == null) {
            numberOfTries++;

            log.debug(String.format(
                "Trying to contact SHERPA/RoMEO - attempt %d of %d; timeout is %d; sleep between timeouts is %d",
                numberOfTries,
                maxNumberOfTries,
                timeout,
                sleepBetweenTimeouts));

            try {
                Thread.sleep(sleepBetweenTimeouts);

                // Construct a default HTTP method (first result)
                method = constructHttpGet(type, field, predicate, value, start, limit);

                // Execute the method
                HttpResponse response = client.execute(method);
                int statusCode = response.getStatusLine().getStatusCode();

                log.debug(response.getStatusLine().getStatusCode() + ": "
                    + response.getStatusLine().getReasonPhrase());

                if (statusCode != HttpStatus.SC_OK) {
                    sherpaResponse = new SHERPAPublisherResponse("SHERPA/RoMEO return not OK status: "
                        + statusCode);
                    String errorBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                    log.error("Error from SHERPA HTTP request: " + errorBody);
                }

                HttpEntity responseBody = response.getEntity();

                // If the response body is valid, pass to SHERPAResponse for parsing as JSON
                if (null != responseBody) {
                    log.debug("Non-null SHERPA resonse received for query of " + value);
                    InputStream content = null;
                    try {
                        content = responseBody.getContent();
                        sherpaResponse =
                            new SHERPAPublisherResponse(content, SHERPAPublisherResponse.SHERPAFormat.JSON);
                    } catch (IOException e) {
                        log.error("Encountered exception while contacting SHERPA/RoMEO: " + e.getMessage(), e);
                    } finally {
                        if (content != null) {
                            content.close();
                        }
                    }
                } else {
                    log.debug("Empty SHERPA response body for query on " + value);
                    sherpaResponse = new SHERPAPublisherResponse("SHERPA/RoMEO returned no response");
                }
            } catch (URISyntaxException e) {
                String errorMessage = "Error building SHERPA v2 API URI: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAPublisherResponse(errorMessage);
            } catch (IOException e) {
                String errorMessage = "Encountered exception while contacting SHERPA/RoMEO: " + e.getMessage();
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
            log.debug("SHERPA response is still null");
            sherpaResponse = new SHERPAPublisherResponse(
                "Error processing the SHERPA/RoMEO answer");
        }

        // Return the final response
        return sherpaResponse;
    }

    /**
     * Perform an API request to the SHERPA v2 API - this could be a search or a get for any entity type
     * For more information about the type, field and predicate arguments, see the SHERPA v2 API documentation
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
        // API Key is *required* for v2 API calls
        if (null == apiKey) {
            log.error("SHERPA ROMeO API Key missing: please register for an API key and set sherpa.romeo.apikey");
            return new SHERPAResponse("SHERPA/RoMEO configuration invalid or missing");
        }

        HttpGet method = null;
        SHERPAResponse sherpaResponse = null;
        int numberOfTries = 0;

        while (numberOfTries < maxNumberOfTries && sherpaResponse == null) {
            numberOfTries++;

            log.debug(String.format(
                "Trying to contact SHERPA/RoMEO - attempt %d of %d; timeout is %d; sleep between timeouts is %d",
                numberOfTries,
                maxNumberOfTries,
                timeout,
                sleepBetweenTimeouts));

            try {
                Thread.sleep(sleepBetweenTimeouts);

                // Construct a default HTTP method (first result)
                method = constructHttpGet(type, field, predicate, value, start, limit);

                // Execute the method
                HttpResponse response = client.execute(method);
                int statusCode = response.getStatusLine().getStatusCode();

                log.debug(response.getStatusLine().getStatusCode() + ": "
                    + response.getStatusLine().getReasonPhrase());

                if (statusCode != HttpStatus.SC_OK) {
                    sherpaResponse = new SHERPAResponse("SHERPA/RoMEO return not OK status: "
                        + statusCode);
                    String errorBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                    log.error("Error from SHERPA HTTP request: " + errorBody);
                }

                HttpEntity responseBody = response.getEntity();

                // If the response body is valid, pass to SHERPAResponse for parsing as JSON
                if (null != responseBody) {
                    log.debug("Non-null SHERPA resonse received for query of " + value);
                    InputStream content = null;
                    try {
                        content = responseBody.getContent();
                        sherpaResponse = new SHERPAResponse(content, SHERPAResponse.SHERPAFormat.JSON);
                    } catch (IOException e) {
                        log.error("Encountered exception while contacting SHERPA/RoMEO: " + e.getMessage(), e);
                    } finally {
                        if (content != null) {
                            content.close();
                        }
                    }
                } else {
                    log.debug("Empty SHERPA response body for query on " + value);
                    sherpaResponse = new SHERPAResponse("SHERPA/RoMEO returned no response");
                }
            } catch (URISyntaxException e) {
                String errorMessage = "Error building SHERPA v2 API URI: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAResponse(errorMessage);
            } catch (IOException e) {
                String errorMessage = "Encountered exception while contacting SHERPA/RoMEO: " + e.getMessage();
                log.error(errorMessage, e);
                sherpaResponse = new SHERPAResponse(errorMessage);
            }  catch (InterruptedException e) {
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
            log.debug("SHERPA response is still null");
            sherpaResponse = new SHERPAResponse(
                "Error processing the SHERPA/RoMEO answer");
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
        if (StringUtils.isNotBlank(apiKey)) {
            uriBuilder.addParameter("api-key", apiKey);
        }

        log.debug("SHERPA API URL: " + uriBuilder.toString());

        // Create HTTP GET object
        HttpGet method = new HttpGet(uriBuilder.build());

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
     * Prepare the API query for execution by the HTTP client
     * @param query     ISSN query string
     * @param endpoint  API endpoint (base URL)
     * @param apiKey    API key parameter
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
            log.warn("No ISSN supplied as query string for SHERPA service search");
        }
        uriBuilder.addParameter("filter", "[[\"issn\",\"equals\",\"" + query + "\"]]");
        uriBuilder.addParameter("format", "Json");
        if (StringUtils.isNotBlank(apiKey)) {
            uriBuilder.addParameter("api-key", apiKey);
        }
        log.debug("Would search SHERPA endpoint with " + uriBuilder.toString());

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