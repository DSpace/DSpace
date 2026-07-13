/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.openpolicyfinder.OpenPolicyFinderService;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderPublisher;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderPublisherResponse;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with
 * OpenPolicyFinderPublisher External
 * data lookups.
 * This provider is a refactored version of OpenPolicyFinderPublisherDataProvider, rewritten to work with
 * the Jisc Open Policy Finder API (formerly Open Policy Finder API)
 *
 * It uses a more simple response object than the normal publication / policy search
 *
 * @author Kim Shepherd
 */
public class OpenPolicyFinderPublisherDataProvider extends AbstractExternalDataProvider {

    // Source identifier (eg 'opfPublisher') configured in spring configuration
    private String sourceIdentifier;
    // Open Policy Finder service configured in spring configuration
    private OpenPolicyFinderService openPolicyFinderService;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * Initialise the client that we need to call the endpoint
     * @throws IOException  If something goes wrong
     */
    public void init() throws IOException {}

    /**
     * Get a single publisher based on a "id equals string" query
     * @param id    The publisher ID which will be used as query string
     * @return external data object representing publisher
     */
    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        // Sanitise the given ID / title query
        id = OpenPolicyFinderUtils.sanitiseQuery(id);

        // Search for publishers matching this ID in Open Policy Finder.
        // Limit to 1 result since this is for a single object
        OpenPolicyFinderPublisherResponse opfResponse =
            openPolicyFinderService.performPublisherRequest("publisher", "id", "equals", id, 0, 1);

        // If there is at least one publisher, retrieve it and transform it to an ExternalDataObject
        if (CollectionUtils.isNotEmpty(opfResponse.getPublishers())) {
            OpenPolicyFinderPublisher publisher = opfResponse.getPublishers().get(0);
            // Construct external data object from returned publisher
            ExternalDataObject externalDataObject = constructExternalDataObjectFromPublisher(publisher);
            return Optional.of(externalDataObject);
        }
        return Optional.empty();
    }

    /**
     * Search Open Policy Finder API for publisher results based on a 'contains word' query for publisher name
     * @param query The query for the search
     * @param start The start of the search
     * @param limit The max amount of records to be returned by the search
     * @return a list of external data objects
     */
    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        // Search Open Policy Finder for publishers with the query term in the title (name)
        OpenPolicyFinderPublisherResponse opfResponse = openPolicyFinderService.performPublisherRequest(
            "publisher", "name", "contains word", query, start, limit);

        // If at least one publisher was found, convert to a list of ExternalDataObjects and return
        if (CollectionUtils.isNotEmpty(opfResponse.getPublishers())) {
            List<ExternalDataObject> list = opfResponse.getPublishers().stream().map(
                publisher -> constructExternalDataObjectFromPublisher(publisher)).collect(
                Collectors.toList());

            // Unlike the previous API version we can request offset and limit, so no need to build a
            // sublist from this list, we can just return the list.
            return list;
        }

        // Return an empty list if nothing was found
        return Collections.emptyList();
    }

    private ExternalDataObject constructExternalDataObjectFromPublisher(
        OpenPolicyFinderPublisher publisher) {
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource(sourceIdentifier);

        // Set publisher name
        if (StringUtils.isNotBlank(publisher.getName())) {
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "title", null, null, publisher.getName()));
            externalDataObject.setDisplayValue(publisher.getName());
            externalDataObject.setValue(publisher.getName());
        }
        // Set publisher ID
        if (StringUtils.isNotBlank(publisher.getIdentifier())) {
            externalDataObject.setId(publisher.getIdentifier());
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "identifier", "opfPublisher", null,
                publisher.getIdentifier()));
        }

        // Set home URL
        if (StringUtils.isNotBlank(publisher.getUri())) {
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "identifier", "other", null, publisher.getUri()));
        }

        return externalDataObject;
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    /**
     * Get number of results returned from a Open Policy Finder publication search
     * @param query The query to be search on and give the total amount of results
     * @return int representing number of publisher results
     */
    @Override
    public int getNumberOfResults(String query) {
        // Search Open Policy Finder for publishers with the query term in the title (name)
        // a limit of 0 means the limit parameter won't be added to the API query
        OpenPolicyFinderPublisherResponse opfResponse = openPolicyFinderService.performPublisherRequest(
            "publication", "title", "contains word", query, 0, 0);

        // Return the number of publishers in the response object
        if (CollectionUtils.isNotEmpty(opfResponse.getPublishers())) {
            return opfResponse.getPublishers().size();
        }

        // If other checks have failed return 0
        return 0;
    }

    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this OpenPolicyFinderPublisherDataProvider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic setter for the Open Policy Finder Service
     * @param openPolicyFinderService     THe Open Policy Finder service to be set on this
     *                                       OpenPolicyFinderPublisherDataProvider
     */
    public void setOpenPolicyFinderService(OpenPolicyFinderService openPolicyFinderService) {
        this.openPolicyFinderService = openPolicyFinderService;
    }

}
