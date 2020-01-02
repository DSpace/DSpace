/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.util.ArrayList;
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
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.dspace.app.sherpa.SHERPAJournal;
import org.dspace.app.sherpa.SHERPAResponse;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with SherpaJournal External
 * data lookups
 */
public class SherpaJournalDataProvider implements ExternalDataProvider {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SherpaJournalDataProvider.class);

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

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {

        HttpGet method = null;
        SHERPAResponse sherpaResponse = null;
        int timeout = 5000;
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter("jtitle", id);
            if (StringUtils.isNotBlank(apiKey)) {
                uriBuilder.addParameter("ak", apiKey);
            }

            method = new HttpGet(uriBuilder.build());
            method.setConfig(RequestConfig.custom()
                                          .setConnectionRequestTimeout(timeout)
                                          .setConnectTimeout(timeout)
                                          .setSocketTimeout(timeout)
                                          .build());
            // Execute the method.

            HttpResponse response = client.execute(method);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                sherpaResponse = new SHERPAResponse("SHERPA/RoMEO return not OK status: "
                                                        + statusCode);
            }

            HttpEntity responseBody = response.getEntity();

            if (null != responseBody) {
                sherpaResponse = new SHERPAResponse(responseBody.getContent());
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

    private ExternalDataObject constructExternalDataObjectFromSherpaJournal(SHERPAJournal sherpaJournal) {
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource(sourceIdentifier);
        externalDataObject.setId(sherpaJournal.getTitle());
        externalDataObject
            .addMetadata(new MetadataValueDTO("dc", "title", null, null, sherpaJournal.getTitle()));
        externalDataObject
            .addMetadata(new MetadataValueDTO("dc", "identifier", "issn", null, sherpaJournal.getIssn()));
        externalDataObject.setValue(sherpaJournal.getTitle());
        externalDataObject.setDisplayValue(sherpaJournal.getTitle());
        return externalDataObject;
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        // query args to add to SHERPA/RoMEO request URL
        HttpGet get = constructHttpGet(query);
        try {
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {

                SHERPAResponse sherpaResponse = new SHERPAResponse(response.getEntity().getContent());
                List<ExternalDataObject> list = sherpaResponse.getJournals().stream().map(
                    sherpaJournal -> constructExternalDataObjectFromSherpaJournal(sherpaJournal)).collect(
                    Collectors.toList());
                // This is because Sherpa returns everything by default so we can't specifiy a start and limit
                // in the query itself
                return list.subList(start, Math.min(start + limit, list.size()));
            }
        } catch (IOException e) {
            log.error("SHERPA/RoMEO query failed: ", e);
            return null;
        } finally {
            get.releaseConnection();
        }
        return null;
    }

    private HttpGet constructHttpGet(String query) {
        List<BasicNameValuePair> args = new ArrayList<BasicNameValuePair>();
        args.add(new BasicNameValuePair("jtitle", query));
        args.add(new BasicNameValuePair("qtype", "contains"));
        args.add(new BasicNameValuePair("ak", apiKey));
        String srUrl = url + "?" + URLEncodedUtils.format(args, "UTF8");
        return new HttpGet(srUrl);
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        HttpGet get = constructHttpGet(query);
        try {
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {

                SHERPAResponse sherpaResponse = new SHERPAResponse(response.getEntity().getContent());
                return sherpaResponse.getNumHits();
            }
        } catch (IOException e) {
            log.error("SHERPA/RoMEO query failed: ", e);
            return 0;
        } finally {
            get.releaseConnection();
        }
        return 0;
    }

    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this SherpaJournalDataProvider
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
     * @param url   The url to be set on this SherpaJournalDataProvider
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Generic getter for the apiKey
     * @return the apiKey value of this SherpaJournalDataProvider
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Generic setter for the apiKey
     * @param apiKey   The apiKey to be set on this SherpaJournalDataProvider
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
