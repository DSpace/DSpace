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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.dspace.app.sherpa.SHERPAPublisher;
import org.dspace.app.sherpa.SHERPAResponse;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.ExternalDataProvider;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with SherpaPublisher External
 * data lookups
 */
public class SherpaPublisherDataProvider implements ExternalDataProvider {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SherpaPublisherDataProvider.class);

    private String sourceIdentifier;
    private String url;
    private String apiKey;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        List<BasicNameValuePair> args = new ArrayList<BasicNameValuePair>();
        args.add(new BasicNameValuePair("id", id));
        args.add(new BasicNameValuePair("ak", apiKey));
        HttpClient hc = new DefaultHttpClient();
        String srUrl = url + "?" + URLEncodedUtils.format(args, "UTF8");
        HttpGet get = new HttpGet(srUrl);
        try {
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                SHERPAResponse sherpaResponse = new SHERPAResponse(response.getEntity().getContent());
                List<SHERPAPublisher> list = sherpaResponse.getPublishers();
                if (CollectionUtils.isNotEmpty(list)) {
                    return Optional.of(constructExternalDataObjectFromSherpaPublisher(list.get(0)));
                }
            }
        } catch (IOException e) {
            log.error("SHERPA/RoMEO query failed: ", e);
            return null;
        } finally {
            get.releaseConnection();
        }
        return null;
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        HttpGet get = constructHttpGet(query);
        try {
            HttpClient hc = new DefaultHttpClient();
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                SHERPAResponse sherpaResponse = new SHERPAResponse(response.getEntity().getContent());
                List<ExternalDataObject> list = sherpaResponse.getPublishers().stream().map(
                    sherpaPublisher -> constructExternalDataObjectFromSherpaPublisher(sherpaPublisher)).collect(
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
        args.add(new BasicNameValuePair("pub", query));
        args.add(new BasicNameValuePair("qtype", "all"));
        args.add(new BasicNameValuePair("ak", apiKey));
        String srUrl = url + "?" + URLEncodedUtils.format(args, "UTF8");
        return new HttpGet(srUrl);
    }

    private ExternalDataObject constructExternalDataObjectFromSherpaPublisher(SHERPAPublisher sherpaPublisher) {
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource(sourceIdentifier);

        //Text value == name
        externalDataObject.addMetadata(new MetadataValueDTO("dc", "title", null, null, sherpaPublisher.getName()));
        externalDataObject.setDisplayValue(sherpaPublisher.getName());
        externalDataObject.setValue(sherpaPublisher.getName());
        if (StringUtils.isNotBlank(sherpaPublisher.getId())) {
            externalDataObject.setId(sherpaPublisher.getId());
            externalDataObject
                .addMetadata(
                    new MetadataValueDTO("dc", "identifier", "sherpaPublisher", null, sherpaPublisher.getId()));
        }

        //Text value == homeurl
        externalDataObject
            .addMetadata(new MetadataValueDTO("dc", "identifier", "other", null, sherpaPublisher.getHomeurl()));

        return externalDataObject;
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
     * @param sourceIdentifier   The sourceIdentifier to be set on this SherpaPublisherDataProvider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic getter for the url
     * @return the url value of this SherpaPublisherDataProvider
     */
    public String getUrl() {
        return url;
    }

    /**
     * Generic setter for the url
     * @param url   The url to be set on this SherpaPublisherDataProvider
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Generic getter for the apiKey
     * @return the apiKey value of this SherpaPublisherDataProvider
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Generic setter for the apiKey
     * @param apiKey   The apiKey to be set on this SherpaPublisherDataProvider
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
