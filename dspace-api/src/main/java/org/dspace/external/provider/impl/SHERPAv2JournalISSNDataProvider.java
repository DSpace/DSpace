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
import org.apache.logging.log4j.Logger;
import org.dspace.app.sherpa.SHERPAService;
import org.dspace.app.sherpa.v2.SHERPAJournal;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.app.sherpa.v2.SHERPAUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with SherpaJournal External
 * data lookups based on ISSN (to match functinoality offered by legacy SHERPASubmitService for policy lookups
 * at the time of submission).
 * This provider is a refactored version of SherpaJournalDataPublisher, rewritten to work with SHERPA v2 API
 *
 * @author Kim Shepherd
 */
public class SHERPAv2JournalISSNDataProvider extends AbstractExternalDataProvider {

    private static final Logger log =
        org.apache.logging.log4j.LogManager.getLogger(
            org.dspace.external.provider.impl.SHERPAv2JournalISSNDataProvider.class);

    // Source identifier (configured in spring configuration)
    private String sourceIdentifier;

    // SHERPA v2 API service (configured in spring configuration)
    SHERPAService sherpaService;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * Initialise the provider - this no longer starts client since that is handled by SHERPAService
     * @throws IOException  If something goes wrong
     */
    public void init() throws IOException {
    }

    /**
     * Get a single journal based on a "issn equals string" query
     * @param issn    The ISSN which will be used as query string
     * @return external data object representing journal
     */
    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String issn) {

        // Sanitise ID / title query string (strips some special characters)
        issn = SHERPAUtils.sanitiseQuery(issn);

        log.debug("Searching SHERPA for ISSN: " + issn);

        // Get SHERPA response from base service
        // Get SHERPA response from the API for all objects matching this ISSN
        SHERPAResponse sherpaResponse = sherpaService.performRequest(
            "publication", "issn", "equals", issn, 0, 1);

        // Construct external data objects
        if (CollectionUtils.isNotEmpty(sherpaResponse.getJournals())) {
            SHERPAJournal sherpaJournal = sherpaResponse.getJournals().get(0);
            ExternalDataObject externalDataObject = constructExternalDataObjectFromSherpaJournal(sherpaJournal);
            return Optional.of(externalDataObject);
        }
        return Optional.empty();
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
            externalDataObject.setId(issn);
            externalDataObject.addMetadata(new MetadataValueDTO(
                "dc", "identifier", "issn", null, issn));

        }

        log.debug("New external data object. Title=" + externalDataObject.getValue() + ". ID="
            + externalDataObject.getId());

        return externalDataObject;
    }

    /**
     * Search SHERPA v2 API for journal results based on a 'contains word' query
     * @param query The term to query for the search
     * @param start The start of the search
     * @param limit The max amount of records to be returned by the search
     * @return a list of external data objects
     */
    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {

        // Get SHERPA response from the API for all objects matching this ISSN
        SHERPAResponse sherpaResponse = sherpaService.performRequest(
            "publication", "issn", "equals", query, start, limit);

        // Construct a list of external data objects and return it
        if (CollectionUtils.isNotEmpty(sherpaResponse.getJournals())) {
            log.debug("Found " + sherpaResponse.getJournals().size() + " matching journals for ISSN " + query);
            List<ExternalDataObject> list = sherpaResponse.getJournals().stream().map(
                sherpaJournal -> constructExternalDataObjectFromSherpaJournal(sherpaJournal)).collect(
                Collectors.toList());

            // Unlike the previous API version we can request offset and limit, so no need to build aF
            // sublist from this list, we can just return the list.
            return list;
        } else {
            log.debug("Empty response from SHERPA v2 API for ISSN " + query);
        }

        // If nothing was returned from the response, return an empty list
        return Collections.emptyList();
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    /**
     * Get number of results returned from a SHERPA v2 publication search
     * @param issn The query to be search on and give the total amount of results
     * @return int representing number of journal results
     */
    @Override
    public int getNumberOfResults(String issn) {

        // Get SHERPA response from the API for all objects matching this ISSN.
        // The limit of 0 means a limit parameter will not be added to the API query
        SHERPAResponse sherpaResponse = sherpaService.performRequest(
            "publication", "issn", "equals", issn, 0, 0);

        // Return the size of the journal collection
        if (CollectionUtils.isNotEmpty(sherpaResponse.getJournals())) {
            return sherpaResponse.getJournals().size();
        }

        // If other checks have failed return 0
        return 0;
    }

    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this SHERPAv2JournalISSNDataProvider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic setter for the SHERPA Service
     * @param sherpaService     THe SHERPA service to be set on this SHERPAv2JournalISSNDataProvider
     */
    public void setSherpaService(SHERPAService sherpaService) {
        this.sherpaService = sherpaService;
    }
}
