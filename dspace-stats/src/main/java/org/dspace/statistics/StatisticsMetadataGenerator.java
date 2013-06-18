package org.dspace.statistics;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;

public interface StatisticsMetadataGenerator
{

    public void addMetadata(SolrInputDocument doc1, HttpServletRequest request,
            DSpaceObject dspaceObject);
}
