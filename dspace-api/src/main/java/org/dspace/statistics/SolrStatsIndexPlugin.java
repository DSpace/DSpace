package org.dspace.statistics;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;

public interface SolrStatsIndexPlugin
{
    public void additionalIndex(HttpServletRequest request, DSpaceObject dso, SolrInputDocument document);
}
