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
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderJournal;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderResponse;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with
 * Open Policy Finder Journal External data lookups.
 * This provider is a refactored version of OpenPolicyFinderJournalDataProvider, rewritten to work with
 * the Jisc Open Policy Finder API (formerly Open Policy Finder API)
 *
 * @author Kim Shepherd
 */
public class OpenPolicyFinderJournalDataProvider extends AbstractExternalDataProvider {

    // Source identifier (configured in spring configuration)
    private String sourceIdentifier;

    // Open Policy Finder API service (configured in spring configuration)
    OpenPolicyFinderService openPolicyFinderService;

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
     * Get a single journal based on a "title equals string" query
     * @param id    The journal title which will be used as query string
     * @return external data object representing journal
     */
    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        // Sanitise ID / title query string (strips some special characters)
        id = OpenPolicyFinderUtils.sanitiseQuery(id);

        // Perform request using the Open Policy Finder service (first row only)
        OpenPolicyFinderResponse opfResponse =
            openPolicyFinderService.performRequest("publication", "title", "equals", id,
                0, 1);

        // If a journal was returned, get it and convert it to an ExternalDataObject
        if (CollectionUtils.isNotEmpty(opfResponse.getJournals())) {
            OpenPolicyFinderJournal journal = opfResponse.getJournals().get(0);

            ExternalDataObject externalDataObject = constructExternalDataObjectFromJournal(journal);
            return Optional.of(externalDataObject);
        }

        // If no journal was returned, return an empty Optional object
        return Optional.empty();
    }

    /**
     * Construct ExternalDataObject populated with journal metadata from the Open Policy Finder API response
     * @param journal
     * @return external data object representing a journal
     */
    private ExternalDataObject constructExternalDataObjectFromJournal(OpenPolicyFinderJournal journal) {
        // Set up external object
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource(sourceIdentifier);
        // Set journal title in external object
        if (CollectionUtils.isNotEmpty(journal.getTitles())) {
            String journalTitle = journal.getTitles().get(0);
            externalDataObject.setId(journal.getTitles().get(0));
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "title", null, null, journalTitle));
            externalDataObject.setValue(journalTitle);
            externalDataObject.setDisplayValue(journalTitle);
        }
        // Set ISSNs in external object
        if (CollectionUtils.isNotEmpty(journal.getIssns())) {
            String issn = journal.getIssns().get(0);
            externalDataObject.addMetadata(new MetadataValueDTO(
                "creativeworkseries", "issn", null, null, issn));

        }

        return externalDataObject;
    }

    /**
     * Search Open Policy Finder API for journal results based on a 'contains word' query
     * @param query The query for the search
     * @param start The start of the search
     * @param limit The max amount of records to be returned by the search
     * @return a list of external data objects
     */
    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        // Search Open Policy Finder for journals with the query term in the title
        OpenPolicyFinderResponse opfResponse = openPolicyFinderService.performRequest("publication", "title",
            "contains word", query, start, limit);

        // Convert Open Policy Finder response to a Collection and return the list
        if (CollectionUtils.isNotEmpty(opfResponse.getJournals())) {
            List<ExternalDataObject> list = opfResponse.getJournals().stream().map(
                journal -> constructExternalDataObjectFromJournal(journal)).collect(
                Collectors.toList());

            // Unlike the previous API version we can request offset and limit, so no need to build a
            // sublist from this list, we can just return the list.
            return list;
        }

        // If nothing has been returned yet, return an empty list
        return Collections.emptyList();
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    /**
     * Get number of results returned from a Open Policy Finder publication search
     * @param query The query to be search on and give the total amount of results
     * @return int representing number of journal results
     */
    @Override
    public int getNumberOfResults(String query) {
        // Search Open Policy Finder for journals with the query term in the title
        // Limit = 0 means the limit parameter will not be added to the API query
        OpenPolicyFinderResponse opfResponse = openPolicyFinderService.performRequest("publication", "title",
            "contains word", query, 0, 0);

        // Get number of journals returned in response
        if (CollectionUtils.isNotEmpty(opfResponse.getJournals())) {
            return opfResponse.getJournals().size();
        }

        // If other checks have failed return 0
        return 0;
    }

    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this OpenPolicyFinderJournalDataProvider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic setter for the Open Policy Finder service
     * @param openPolicyFinderService   The openPolicyFinderService to be set on this
     *                                     OpenPolicyFinderJournalDataProvider
     */
    public void setOpenPolicyFinderService(OpenPolicyFinderService openPolicyFinderService) {
        this.openPolicyFinderService = openPolicyFinderService;
    }

}
