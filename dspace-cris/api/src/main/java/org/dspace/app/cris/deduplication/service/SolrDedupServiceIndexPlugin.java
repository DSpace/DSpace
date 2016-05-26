package org.dspace.app.cris.deduplication.service;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public interface SolrDedupServiceIndexPlugin
{
    public void additionalIndex(Context context, Integer dsoFirst, Integer dsoSecond, Integer type, SolrInputDocument document);
}
