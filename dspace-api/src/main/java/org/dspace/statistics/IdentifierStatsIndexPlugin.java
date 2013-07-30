package org.dspace.statistics;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;

public class IdentifierStatsIndexPlugin implements SolrStatsIndexPlugin
{

    @Override
    public void additionalIndex(HttpServletRequest request, DSpaceObject dso,
            SolrInputDocument document)
    {
        document.addField("search.uniqueid", dso.getType() + "-"
                + dso.getID());   

    }
    
}
