/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Discovery (Solr) driver implementing ItemCountDAO interface to look up item
 * count information in communities and collections.
 * <p>
 * Counts are computed by querying Solr for archived, non-withdrawn, discoverable
 * items using {@code location.comm} / {@code location.coll} filters.
 * The query returns only {@code numFound} (rows=0), making it very fast.
 */
public class ItemCountDAOSolr implements ItemCountDAO {

    private static final Logger log = LogManager.getLogger(ItemCountDAOSolr.class);

    @Autowired
    private SolrSearchCore solrSearchCore;

    @Override
    public int getCount(Context context, DSpaceObject dso) {
        String locationFilter;
        if (dso instanceof Collection) {
            locationFilter = "location.coll:" + dso.getID().toString();
        } else if (dso instanceof Community) {
            locationFilter = "location.comm:" + dso.getID().toString();
        } else {
            return 0;
        }

        try {
            SolrClient solr = solrSearchCore.getSolr();
            if (solr == null) {
                return 0;
            }
            SolrQuery query = new SolrQuery("*:*");
            query.addFilterQuery(locationFilter);
            query.addFilterQuery("search.resourcetype:" + IndexableItem.TYPE);
            query.addFilterQuery("NOT(discoverable:false)");
            query.addFilterQuery("withdrawn:false");
            query.addFilterQuery("archived:true");
            query.setRows(0);
            QueryResponse response = solr.query(query, solrSearchCore.REQUEST_METHOD);
            return (int) response.getResults().getNumFound();
        } catch (Exception e) {
            log.error("Error counting items in Solr for {}: ", dso.getID(), e);
        }
        return 0;
    }
}
