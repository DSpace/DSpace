/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.submit;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.dspace.app.sherpa.SHERPAService;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;

/**
 * SHERPASubmitService is
 * @see
 * @author Kim Shepherd
 */
public class SHERPASubmitService {

    /**
     * Spring beans for configuration and API service
     */
    protected SHERPAService sherpaService;
    protected SHERPASubmitConfigurationService configuration;

    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(SHERPASubmitService.class);

    /**
     * Setter for configuration (from Spring)
     * @see "dspace-dspace-addon-sherpa-configuration-services.xml"
     * @param configuration
     */
    public void setConfiguration(SHERPASubmitConfigurationService configuration) {
        this.configuration = configuration;
    }

    /**
     * Setter for SHERPA service, reponsible for actual HTTP API calls
     * @see "dspace-dspace-addon-sherpa-configuration-services.xml"
     * @param sherpaService
     */
    public void setSherpaService(SHERPAService sherpaService) {
        this.sherpaService = sherpaService;
    }

    /**
     * Search SHERPA for journal policies matching the ISSNs in the item.
     * Rather than a 'search' query for any/all ISSNs, the v2 API requires a separate
     * query for each ISSN found in the item. The ISSNs are extracted using the configured
     * issnItemExtractor(s) in the SHERPA spring configuration.
     * The ISSNs are not validated with a regular expression or other rules - any values
     * extracted will be included in API queries.
     * Return the first not empty response from Sherpa
     * @see "dspace-dspace-addon-sherpa-configuration-services.xml"
     * @param context   DSpace context
     * @param item      DSpace item containing ISSNs to be checked
     * @return          SHERPA v2 API response (policy data)
     */
    public SHERPAResponse searchRelatedJournals(Context context, Item item) {
        Set<String> issns = getISSNs(context, item);
        if (issns == null || issns.size() == 0) {
            return null;
        } else {
            // SHERPA v2 API no longer supports "OR'd" ISSN search, perform individual searches instead
            Iterator<String> issnIterator = issns.iterator();
            while (issnIterator.hasNext()) {
                String issn = issnIterator.next();
                SHERPAResponse response = sherpaService.searchByJournalISSN(issn);
                if (response.isError()) {
                    // Continue with loop
                    log.warn("Failed to look up SHERPA ROMeO result for ISSN: " + issn
                        + ": " + response.getMessage());
                    return response;
                } else if (!response.getJournals().isEmpty()) {
                    // return this response, if it is not empty
                    return response;
                }
            }
            return new SHERPAResponse();
        }
    }

    /**
     * Search SHERPA for journal policies matching the passed ISSN.
     * The ISSN are not validated with a regular expression or other rules - any String
     * passed to this method will be considered an ISSN for the purposes of an API query
     * @param issn  ISSN string
     * @return      SHERPA v2 API response object (policy data)
     */
    public SHERPAResponse searchRelatedJournalsByISSN(String issn) {
        return sherpaService.searchByJournalISSN(issn);
    }

    /**
     * Using the configured itemIssnExtractors from SHERPA configuration, extract
     * ISSNs from item metadata or authority values
     * @param context   DSpace context
     * @param item      Item containing metadata / authority values
     * @return          Set of ISSN strings
     */
    public Set<String> getISSNs(Context context, Item item) {
        Set<String> issns = new LinkedHashSet<String>();
        if (configuration.getIssnItemExtractors() == null) {
            log.warn(LogHelper.getHeader(context, "searchRelatedJournals",
                                          "no issnItemExtractors defined"));
            return null;
        }
        for (ISSNItemExtractor extractor : configuration.getIssnItemExtractors()) {
            List<String> eIssns = extractor.getISSNs(context, item);
            if (eIssns != null) {
                for (String eIssn : eIssns) {
                    issns.add(eIssn.trim());
                }
            }
        }
        return issns;
    }

    /**
     * Simple boolean test that runs the getISSNs extraction method
     * to determine whether an item has any ISSNs at all
     * @param context   DSpace context
     * @param item      Item to test
     * @return          boolean indicating presence of >=1 ISSNs
     */
    public boolean hasISSNs(Context context, Item item) {
        Set<String> issns = getISSNs(context, item);
        if (issns == null || issns.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

}
