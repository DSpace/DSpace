/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.metrics;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.Constants;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

public class TotalDownloadsAndVisitsGenerator {
    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

    /**
     * Create stat points of the items over views and downloads
     * specific community, collection
     *
     * @param uuid      Uuid of item for which to find views and downloads
     * @return  Map<String, Integer> with views and downloads
     */

    public  Map<String, Integer> createUsageReport(UUID uuid, int type)
            throws SolrServerException, IOException, SQLException {
        // query for item
        String query = "type:" + type + " AND id:" + uuid;
        // View and downloads point
        Map<String, Integer> views_downloads = new HashMap<>();
        // first check item visits
        StringBuilder filterQuery = new StringBuilder();
        filterQuery.append("(statistics_type:").append(SolrLoggerServiceImpl.StatisticsType.VIEW
                .text()).append(")").append(" AND -isBot:true");
        ObjectCount[] topCounts = solrLoggerService
                                      .queryFacetField(query,
                                                       filterQuery.toString(),
                                                       "id", 50,
                                                       false, null,
                                                       1);
        //use accumulator in case the query returns more than one data
        int downloads = 0;
        int views = 0;
        for (ObjectCount topCount : topCounts) {
            //add visits for item
            views += (int) topCount.getCount();
            // check bitstreams  statistics related with this item
            String bitStreamQuery = "owningItem" + ":" + topCount.getValue() + " AND type:" + Constants.BITSTREAM
                    + " AND -bundleName:LICENSE AND -bundleName:THUMBNAIL AND -bundleName:SWORD"
                    + " AND -bundleName:TEXT AND -isBot:true";
            ObjectCount[] topCounts1 = solrLoggerService
                                           .queryFacetField(bitStreamQuery,
                                                            filterQuery.toString(),
                                                            "id", 50,
                                                            false, null, 1);
            //it can have more than one
            for (ObjectCount objectCountBitStream : topCounts1) {
                //calculate downloads for item count bitstream values
                downloads += objectCountBitStream.getCount();
            }
        }
        views_downloads.put("views", views);
        views_downloads.put("downloads", downloads);
        return views_downloads;
    }

}
