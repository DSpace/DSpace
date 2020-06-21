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
import java.util.Collections;
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
import org.dspace.app.sherpa.v2.SHERPAPublisher;
import org.dspace.app.sherpa.v2.SHERPAPublisherResponse;
import org.dspace.app.sherpa.v2.SHERPAUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with SHERPAPublisher External
 * data lookups.
 * This provider is a refactored version of SherpaPublisherDataPublisher, rewritten to work with SHERPA v2 API
 *
 * It uses a more simple response object than the normal publication / policy search
 *
 * @author Kim Shepherd
 */
public class SHERPAv2PublisherDataProvider implements ExternalDataProvider {

    private static final Logger log =
        org.apache.logging.log4j.LogManager.getLogger(SHERPAv2PublisherDataProvider.class);

    private String sourceIdentifier;
    private String url;
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

        // Initialise API key and base URL from configuration service
        apiKey = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("sherpa.romeo.apikey");
        url = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("sherpa.romeo.url",
            "https://v2.sherpa.ac.uk/cgi/retrieve");
    }

    /**
     * Get a single publisher based on a "id equals string" query
     * @param id    The publisher ID which will be used as query string
     * @return external data object representing publisher
     */
    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {

        HttpGet method = null;
        SHERPAPublisherResponse sherpaResponse = null;
        int timeout = 5000;
        URIBuilder uriBuilder = null;

        // Escape the given ID / title query
        id = SHERPAUtils.escapeQuery(id);

        try {
            // Construct URI for an exact match on journal title
            uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter("item-type", "publisher");
            uriBuilder.addParameter("filter", "[[\"id\",\"equals\",\"" + id + "\"]]");
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
                sherpaResponse = new SHERPAPublisherResponse("SHERPA/RoMEO return not OK status: "
                    + statusCode);
            }

            HttpEntity responseBody = response.getEntity();

            // Get InputStream from API response and parse JSON
            if (null != responseBody) {
                InputStream content = null;
                try {
                    content = responseBody.getContent();
                    sherpaResponse = new SHERPAPublisherResponse(content, SHERPAPublisherResponse.SHERPAFormat.JSON);
                } catch (IOException e) {
                    log.error("Encountered exception while contacting SHERPA/RoMEO: " + e.getMessage(), e);
                } finally {
                    if (content != null) {
                        content.close();
                    }
                }
            } else {
                sherpaResponse = new SHERPAPublisherResponse("SHERPA/RoMEO returned no response");
            }
        } catch (Exception e) {
            log.error("SHERPA/RoMEO query failed: ", e);
        }

        if (sherpaResponse == null) {
            sherpaResponse = new SHERPAPublisherResponse("Error processing the SHERPA/RoMEO answer");
        }
        if (CollectionUtils.isNotEmpty(sherpaResponse.getPublishers())) {
            SHERPAPublisher sherpaPublisher = sherpaResponse.getPublishers().get(0);
            // Construct external data object from returned publisher
            ExternalDataObject externalDataObject = constructExternalDataObjectFromSherpaPublisher(sherpaPublisher);
            return Optional.of(externalDataObject);
        }
        return Optional.empty();
    }

    /**
     * Search SHERPA v2 API for publisher results based on a 'contains word' query for publisher name
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
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // Get response input stream and parse JSON into new SHERPAResponse object
                InputStream content = null;
                try {
                    content = response.getEntity().getContent();
                    SHERPAPublisherResponse sherpaResponse =
                        new SHERPAPublisherResponse(content, SHERPAPublisherResponse.SHERPAFormat.JSON);
                    if (CollectionUtils.isNotEmpty(sherpaResponse.getPublishers())) {
                        List<ExternalDataObject> list = sherpaResponse.getPublishers().stream().map(
                            sherpaPublisher -> constructExternalDataObjectFromSherpaPublisher(sherpaPublisher)).collect(
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
        return Collections.emptyList();
    }

    /**
     * Construct HTTP GET object for a "publisher name contains word" query using default start/limit parameters
     * @param query a word or phrase to search for in publisher name
     * @return HttpGet method which can then be executed by the client
     * @throws URISyntaxException if the URL build fails
     */
    private HttpGet constructHttpGet(String query) throws URISyntaxException {
        return constructHttpGet(query, -1, -1);
    }

    /**
     * Construct HTTP GET object for a "publisher name contains word" query
     * @param query the search query
     * @param start row offset
     * @param limit number of results to return
     * @return HttpGet object to be executed by the client
     * @throws URISyntaxException
     */
    private HttpGet constructHttpGet(String query, int start, int limit) throws URISyntaxException {
        // Escape query string
        query = SHERPAUtils.escapeQuery(query);

        // Build URL based on search query
        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.addParameter("item-type", "publisher");
        uriBuilder.addParameter("filter", "[[\"name\",\"contains word\",\"" + query + "\"]]");
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

    private ExternalDataObject constructExternalDataObjectFromSherpaPublisher(SHERPAPublisher sherpaPublisher) {
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource(sourceIdentifier);

        // Set publisher name
        if (StringUtils.isNotBlank(sherpaPublisher.getName())) {
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "title", null, null, sherpaPublisher.getName()));
            externalDataObject.setDisplayValue(sherpaPublisher.getName());
            externalDataObject.setValue(sherpaPublisher.getName());
        }
        // Set publisher ID
        if (StringUtils.isNotBlank(sherpaPublisher.getIdentifier())) {
            externalDataObject.setId(sherpaPublisher.getIdentifier());
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "identifier", "sherpaPublisher", null,
                sherpaPublisher.getIdentifier()));
        }

        // Set home URL
        if (StringUtils.isNotBlank(sherpaPublisher.getUri())) {
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "identifier", "other", null, sherpaPublisher.getUri()));
        }

        return externalDataObject;
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    /**
     * Get number of results returned from a SHERPA v2 publication search
     * @param query The query to be search on and give the total amount of results
     * @return int representing number of publisher results
     */
    @Override
    public int getNumberOfResults(String query) {
        HttpGet get = null;
        try {
            get = constructHttpGet(query);
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // Read response and parse as SHERPAResponse object
                InputStream content = null;
                try {
                    content = response.getEntity().getContent();
                    SHERPAPublisherResponse sherpaResponse =
                        new SHERPAPublisherResponse(content, SHERPAPublisherResponse.SHERPAFormat.JSON);
                    if (CollectionUtils.isNotEmpty(sherpaResponse.getPublishers())) {
                        return sherpaResponse.getPublishers().size();
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
     * @param sourceIdentifier   The sourceIdentifier to be set on this SHERPAv2PublisherDataProvider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

}
