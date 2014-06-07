/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.content.DSpaceObject;
import org.dspace.statistics.SolrStatsIndexPlugin;

public class SectionStatsIndexPlugin implements SolrStatsIndexPlugin
{

    @Override
    public void additionalIndex(HttpServletRequest request, DSpaceObject dso,
            SolrInputDocument document)
    {
        Integer sectionID = null;

        if (dso != null && dso.getType() >= CrisConstants.CRIS_TYPE_ID_START)
        {
            if (request != null)
            {
                sectionID = (Integer) request.getAttribute("sectionid");
                if (sectionID > 0)
                {
                    document.addField("sectionid", sectionID);
                }
            }
        }

    }
}
