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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.app.sherpa.SHERPAService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

public class SHERPASubmitService
{
    private SHERPAService sherpaService;

    private SHERPASubmitConfigurationService configuration;

    /** log4j logger */
    private static Logger log = Logger.getLogger(SHERPASubmitService.class);

    public void setConfiguration(SHERPASubmitConfigurationService configuration)
    {
        this.configuration = configuration;
    }

    public void setSherpaService(SHERPAService sherpaService)
    {
        this.sherpaService = sherpaService;
    }

    public List<SHERPAResponse> searchRelatedJournals(Context context, Item item) {
        Set<String> issns = getISSNs(context, item);
        if (issns == null || issns.size() == 0) {
            return null;
        } else {
            // SHERPA v2 API no longer supports "OR'd" ISSN search, perform individual searches instead
            Iterator<String> issnIterator = issns.iterator();
            List<SHERPAResponse> responses = new LinkedList<>();
            while (issnIterator.hasNext()) {
                String issn = issnIterator.next();
                SHERPAResponse response = sherpaService.searchByJournalISSN(issn);
                if (response.isError()) {
                    // Continue with loop
                    log.warn("Failed to look up SHERPA ROMeO result for ISSN: " + issn);
                }
                // Store this response, even if it has an error (useful for UI reporting)
                responses.add(response);
            }
            if (responses.isEmpty()) {
                responses.add(new SHERPAResponse("SHERPA ROMeO lookup failed"));
            }
            return responses;
        }
    }

    public SHERPAResponse searchRelatedJournalsByISSN(String issn)
    {
        return sherpaService.searchByJournalISSN(issn);
    }

    public Set<String> getISSNs(Context context, Item item)
    {
        Set<String> issns = new LinkedHashSet<>();
        if (configuration.getIssnItemExtractors() == null)
        {
            log.warn(LogManager.getHeader(context, "searchRelatedJournals",
                    "no issnItemExtractors defined"));
            return null;
        }
        for (ISSNItemExtractor extractor : configuration.getIssnItemExtractors())
        {
            List<String> eIssns = extractor.getISSNs(context, item);
            if (eIssns != null)
            {
                for (String eIssn : eIssns)
                {
                    issns.add(eIssn.trim());
                }
            }
        }
        return issns;
    }

    public boolean hasISSNs(Context context, Item item)
    {
        Set<String> issns = getISSNs(context, item);
        if (issns == null || issns.size() == 0)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

}
