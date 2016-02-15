/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.submit;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.sherpa.SHERPAResponse;
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

    public SHERPAResponse searchRelatedJournals(Context context, Item item)
    {
        Set<String> issns = getISSNs(context, item);
        if (issns == null || issns.size() == 0)
        {
            return null;
        }
        else
        {
            return sherpaService.searchByJournalISSN(StringUtils.join(issns, ","));
        }
    }

    public SHERPAResponse searchRelatedJournalsByISSN(String issn)
    {
        return sherpaService.searchByJournalISSN(issn);
    }

    public Set<String> getISSNs(Context context, Item item)
    {
        Set<String> issns = new LinkedHashSet<String>();
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
