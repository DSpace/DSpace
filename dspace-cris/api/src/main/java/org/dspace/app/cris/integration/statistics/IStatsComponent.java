/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.statistics;

import java.util.Date;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.dspace.app.cris.statistics.bean.TreeKeyMap;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;


public interface IStatsComponent<T extends DSpaceObject> extends IStatsGenericComponent<T>
{

    TwoKeyMap getLabels(Context context, String type) throws Exception;
    
    TreeKeyMap query(String id, HttpSolrServer solrServer,Date startDate, Date endDate)  throws Exception;
       
    Map<String, ObjectCount[]> queryFacetDate(SolrLogger statsLogger,
            T object, String dateType, String dateStart,
            String dateEnd, int gap, Context context) throws SolrServerException;
}
