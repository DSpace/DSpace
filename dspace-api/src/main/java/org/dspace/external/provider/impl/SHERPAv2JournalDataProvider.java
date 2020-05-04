/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.Logger;
import org.dspace.app.sherpa.v2.SHERPAJournal;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with SherpaJournal External
 * data lookups.
 * This provider is a refactored version of SherpaJournalDataPublisher, rewritten to work with SHERPA v2 API
 *
 * @author Kim Shepherd
 */
public class SHERPAv2JournalDataProvider implements ExternalDataProvider {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SHERPAv2JournalDataProvider.class);

    private String url;
    private String sourceIdentifier;
    private String apiKey;

    private CloseableHttpClient client = null;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * Initialise the client that we need to call the endpoint
     * @throws IOException  If something goes wrong
     */
    public void init() throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        // httpclient 4.3+ doesn't appear to have any sensible defaults any more. Setting conservative defaults as
        // not to hammer the SHERPA service too much.
        client = builder
            .disableAutomaticRetries()
            .setMaxConnTotal(5)
            .build();
    }

    /**
     * Get a single journal based on a "title equals string" query
     * @param id    The journal title which will be used as query string
     * @return external data object representing journal
     */
    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {

        HttpGet method = null;
        SHERPAResponse sherpaResponse = null;
        int timeout = 5000;
        URIBuilder uriBuilder = null;
        try {
            // Construct URI for an exact match on journal title
            uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter("item-type", "publication");
            uriBuilder.addParameter("filter", "[[\"title\",\"equals\",\"" + id + "\"]]");
            uriBuilder.addParameter("format", "Json");
            if (StringUtils.isNotBlank(apiKey)) {
                uriBuilder.addParameter("api-key", apiKey);
            }

            // Build HTTP method
            method = new HttpGet(uriBuilder.build());
            method.setConfig(RequestConfig.custom()
                                          .setConnectionRequestTimeout(timeout)
                                          .setConnectTimeout(timeout)
                                          .setSocketTimeout(timeout)
                                          .build());

            // Execute the method
            HttpResponse response = client.execute(method);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                sherpaResponse = new SHERPAResponse("SHERPA/RoMEO return not OK status: "
                                                        + statusCode);
            }

            HttpEntity responseBody = response.getEntity();

            // Get InputStream from API response and parse JSON
            if (null != responseBody) {
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
                sherpaResponse = new SHERPAResponse("SHERPA/RoMEO returned no response");
            }
        } catch (Exception e) {
            log.error("SHERPA/RoMEO query failed: ", e);
        }

        if (sherpaResponse == null) {
            sherpaResponse = new SHERPAResponse(
                "Error processing the SHERPA/RoMEO answer");
        }
        if (CollectionUtils.isNotEmpty(sherpaResponse.getJournals())) {
            SHERPAJournal sherpaJournal = sherpaResponse.getJournals().get(0);

            ExternalDataObject externalDataObject = constructExternalDataObjectFromSherpaJournal(sherpaJournal);
            return Optional.of(externalDataObject);
        }
        return null;
    }

    /**
     * Construct ExternalDataObject populated with journal metadata from the SHERPA v2 API response
     * @param sherpaJournal
     * @return external data object representing a journal
     */
    private ExternalDataObject constructExternalDataObjectFromSherpaJournal(SHERPAJournal sherpaJournal) {
        // Set up external object
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource(sourceIdentifier);
        // Set journal title in external object
        if (CollectionUtils.isNotEmpty(sherpaJournal.getTitles())) {
            String journalTitle = sherpaJournal.getTitles().get(0);
            externalDataObject.setId(sherpaJournal.getTitles().get(0));
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "title", null, null, journalTitle));
            externalDataObject.setValue(journalTitle);
            externalDataObject.setDisplayValue(journalTitle);
        }
        // Set ISSNs in external object
        if (CollectionUtils.isNotEmpty(sherpaJournal.getIssns())) {
            String issn = sherpaJournal.getIssns().get(0);
            externalDataObject.addMetadata(new MetadataValueDTO(
                    "dc", "identifier", "issn", null, issn));

        }

        return externalDataObject;
    }

    /**
     * Search SHERPA v2 API for journal results based on a 'contains word' query
     * @param query The query for the search
     * @param start The start of the search
     * @param limit The max amount of records to be returned by the search
     * @return a list of external data objects
     */
    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        // query args to add to SHERPA/RoMEO request URL
        HttpGet get = null;
        try {
            get = constructHttpGet(query, start, limit);
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                // Get response input stream and parse JSON into new SHERPAResponse object
                InputStream content = null;
                try {
                    content = response.getEntity().getContent();
                    SHERPAResponse sherpaResponse = new SHERPAResponse(content, SHERPAResponse.SHERPAFormat.JSON);
                    if (CollectionUtils.isNotEmpty(sherpaResponse.getJournals())) {
                        List<ExternalDataObject> list = sherpaResponse.getJournals().stream().map(
                            sherpaJournal -> constructExternalDataObjectFromSherpaJournal(sherpaJournal)).collect(
                            Collectors.toList());

                        // Unlike the previous API version we can request offset and limit, so no need to build a
                        // sublist from this list, we can just return the list.
                        return list;
                    }
                } catch (IOException e) {
                    log.error("Error parsing SHERPA response input stream: " + e.getMessage());
                } finally {
                    if (content != null) {
                        content.close();
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.error("SHERPA/RoMEO query failed: ", e);
            return null;
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
        return null;
    }

    /**
     * Construct HTTP GET object for a "title contains word" query using default start/limit parameters
     * @param query a word or phrase to search for in journal title
     * @return HttpGet method which can then be executed by the client
     * @throws URISyntaxException if the URL build fails
     */
    private HttpGet constructHttpGet(String query) throws URISyntaxException {
        return constructHttpGet(query, -1, -1);
    }

    /**
     * Construct HTTP GET object for a "title contains word" query
     * @param query the search query
     * @param start row offset
     * @param limit number of results to return
     * @return HttpGet object to be executed by the client
     * @throws URISyntaxException
     */
    private HttpGet constructHttpGet(String query, int start, int limit) throws URISyntaxException {
        // Build URL based on search query
        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.addParameter("item-type", "publication");
        uriBuilder.addParameter("filter", "[[\"title\",\"contains word\",\"" + query + "\"]]");
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

        // Create HTTP GET object
        HttpGet method = new HttpGet(uriBuilder.build());

        // Set connection parameters
        int timeout = 5000;
        method.setConfig(RequestConfig.custom()
            .setConnectionRequestTimeout(timeout)
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build());
        return new HttpGet(uriBuilder.build());
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    /**
     * Get number of results returned from a SHERPA v2 publication search
     * @param query The query to be search on and give the total amount of results
     * @return int representing number of journal results
     */
    @Override
    public int getNumberOfResults(String query) {
        HttpGet get = null;
        try {
            get = constructHttpGet(query);
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                // Read response and parse as SHERPAResponse object
                InputStream content = null;
                try {
                    content = response.getEntity().getContent();
                    SHERPAResponse sherpaResponse = new SHERPAResponse(content, SHERPAResponse.SHERPAFormat.JSON);
                    if (CollectionUtils.isNotEmpty(sherpaResponse.getJournals())) {
                        return sherpaResponse.getJournals().size();
                    }
                } catch (IOException e) {
                    log.error("Error reading input stream for SHERPAResponse: " + e.getMessage());
                } finally {
                    if (content != null) {
                        content.close();
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.error("SHERPA/RoMEO query failed: ", e);
            return 0;
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
        // If other checks have failed return 0
        return 0;
    }

    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this SHERPAv2JournalDataProvider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic getter for the url
     * @return the url value of this SherpaJournalDataProvider
     */
    public String getUrl() {
        return url;
    }

    /**
     * Generic setter for the url
     * @param url   The url to be set on this SHERPAv2JournalDataProvider
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Generic getter for the apiKey
     * @return the apiKey value of this SHERPAv2JournalDataProvider
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Generic setter for the apiKey
     * @param apiKey   The apiKey to be set on this SHERPAv2JournalDataProvider
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
