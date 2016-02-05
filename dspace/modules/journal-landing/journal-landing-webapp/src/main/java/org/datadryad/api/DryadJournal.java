/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.datadryad.api;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Convenience class for querying Solr and Postgres for data related to a given 
 * journal, as used by the journal landing pages reporting.
 * 
 * @author Nathan Day <nday@datadryad.org>
 */
public class DryadJournal {

    private static Logger log = Logger.getLogger(DryadJournal.class);
    private static final String solrStatsUrl = ConfigurationManager.getProperty("solr.stats.server");

    private Context context;
    private String journalName;

    public DryadJournal(Context context, String journalName) throws IllegalArgumentException {
        if (context == null) {
            throw new IllegalArgumentException("Illegal null Context.");
        } else if (journalName == null || journalName.length() == 0) {
            throw new IllegalArgumentException("Illegal null or empty journal name.");
        }
        this.context = context;
        this.journalName = journalName;
    }

    /**
     * Query to request all item id values for the data files associated with
     * all archived data packages for a given journal.
     * ? #1: full journal name
     */
    private final static String ARCHIVED_DATAFILE_QUERY =
    // item.item_id for data file
    " SELECT DISTINCT mdv_df.item_id                                                                                    " +
    "   FROM metadatavalue mdv_df                                                                                       " +
    "   JOIN metadatafieldregistry mdfr_df ON mdv_df.metadata_field_id=mdfr_df.metadata_field_id                        " +
    "   JOIN metadataschemaregistry mdsr_df ON mdsr_df.metadata_schema_id=mdfr_df.metadata_schema_id                    " +
    "  WHERE mdsr_df.short_id='dc'                                                                                      " +
    "    AND mdfr_df.element='relation'                                                                                 " +
    "    AND mdfr_df.qualifier='ispartof'                                                                               " +
    "    AND mdv_df.text_value IN                                                                                       " +
    //  doi for data packages for provided journal
    "   (SELECT mdv_p_doi.text_value                                                                                    " +
    "      FROM  metadatavalue mdv_p_doi                                                                                " +
    "      JOIN  metadatafieldregistry mdfr_p_doi ON mdv_p_doi.metadata_field_id=mdfr_p_doi.metadata_field_id           " +
    "      JOIN  metadataschemaregistry mdsr_p_doi ON mdfr_p_doi.metadata_schema_id=mdsr_p_doi.metadata_schema_id       " +
    "     WHERE  mdsr_p_doi.short_id='dc'                                                                               " +
    "       AND  mdfr_p_doi.element='identifier'                                                                        " +
    "       AND  mdfr_p_doi.qualifier IS NULL                                                                           " +
    "       AND  mdv_p_doi.item_id IN                                                                                   " +
    //    item_id for data packages for provided journal
    "     (SELECT mdv_p_pub.item_id                                                                                     " +
    "          FROM  metadatavalue mdv_p_pub                                                                            " +
    "          JOIN  metadatafieldregistry mdfr_p_pub  ON mdv_p_pub.metadata_field_id=mdfr_p_pub.metadata_field_id      " +
    "          JOIN  metadataschemaregistry mdsr_p_pub ON mdfr_p_pub.metadata_schema_id=mdsr_p_pub.metadata_schema_id   " +
    "          JOIN  item item_p on mdv_p_pub.item_id=item_p.item_id                                                    " +
    "         WHERE  mdsr_p_pub.short_id='prism'                                                                        " +
    "           AND  mdfr_p_pub.element='publicationName'                                                               " +
    "           AND  mdv_p_pub.text_value = ?                                                                           " + // ? : journal name
    "           AND  item_p.in_archive = true                                                                           " +
    "    ));                                                                                                            ";

    /**
     * Executes query to Postgres to get archived data file item ids for a given 
     * journal, returning the item ids.
     * @return a List of {@link Integer} values representing item.item_id values
     * @throws SQLException
     */
    public List<Integer> getArchivedDataFiles() throws SQLException {
        TableRowIterator tri = DatabaseManager.query(this.context, ARCHIVED_DATAFILE_QUERY, this.journalName);
        List<Integer> dataFiles = new ArrayList<Integer>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            int itemId = row.getIntColumn("item_id");
            dataFiles.add(itemId);
        }
        return dataFiles;
    }

    /**
     * Query to return a list of item ids for archived data packages for a given journal.
     */
    private final static String ARCHIVED_DATAPACKAGE_QUERY_IDS =
    " SELECT item_p.item_id                                                                                 " +
    "  FROM item item_p                                                                                     " +
    "  JOIN metadatavalue          mdv_pub   ON item_p.item_id               = mdv_pub.item_id              " +
    "  JOIN metadatafieldregistry  mdfr_pub  ON mdv_pub.metadata_field_id    = mdfr_pub.metadata_field_id   " +
    "  JOIN metadataschemaregistry mdsr_pub  ON mdfr_pub.metadata_schema_id  = mdsr_pub.metadata_schema_id  " +
    "  JOIN metadatavalue          mdv_date  ON item_p.item_id               = mdv_date.item_id             " +
    "  JOIN metadatafieldregistry  mdfr_date ON mdv_date.metadata_field_id   = mdfr_date.metadata_field_id  " +
    "  JOIN metadataschemaregistry mdsr_date ON mdfr_date.metadata_schema_id = mdsr_date.metadata_schema_id " +
    " WHERE item_p.in_archive   = true                                                                      " +
    "   AND mdsr_pub.short_id   = 'prism'                                                                   " +
    "   AND mdfr_pub.element    = 'publicationName'                                                         " +
    "   AND mdv_pub.text_value  = ?                                                                         " +     // ?: journal name
    "   AND mdsr_date.short_id  = 'dc'                                                                      " +
    "   AND mdfr_date.element   = 'date'                                                                    " +
    "   AND mdfr_date.qualifier = 'accessioned'                                                             " +
    " ORDER BY mdv_date.text_value DESC                                                                     " +
    " LIMIT ?                                                                                               ";      // ?: limit
    
    /**
     * Query to return a count of archived data packages for a given journal.
     */
    private final static String ARCHIVED_DATAPACKAGE_QUERY_COUNT =
    " SELECT COUNT(item_p) AS total                                                                         " +
    "  FROM item item_p                                                                                     " +
    "  JOIN metadatavalue          mdv_pub   ON item_p.item_id               = mdv_pub.item_id              " +
    "  JOIN metadatafieldregistry  mdfr_pub  ON mdv_pub.metadata_field_id    = mdfr_pub.metadata_field_id   " +
    "  JOIN metadataschemaregistry mdsr_pub  ON mdfr_pub.metadata_schema_id  = mdsr_pub.metadata_schema_id  " +
    " WHERE item_p.in_archive   = true                                                                      " +
    "   AND mdsr_pub.short_id   = 'prism'                                                                   " +
    "   AND mdfr_pub.element    = 'publicationName'                                                         " +
    "   AND mdv_pub.text_value  = ?                                                                         ";

    /**
     * Return count of archived data packages for the journal associated with this object.
     * @return int count
     */
    public int getArchivedPackagesCount() {
        int count = 0;
        try {
            PreparedStatement statement = context.getDBConnection().prepareStatement(ARCHIVED_DATAPACKAGE_QUERY_COUNT);
            statement.setString(1,journalName);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch (Exception ex) {
            log.error(ex);
        }
        return count;
    }

    /**
     * Return a sorted list of archived data packages (Item objects) for the journal 
     * associated with this object. The data packages are sorted according to 
     * date-accessioned, with most recently accessioned package first.
     * @param max total number of items to return
     * @return List<org.dspace.content.Item> data packages
     * @throws SQLException 
     */
    public List<Item> getArchivedPackagesSortedRecent(int max) throws SQLException {
        TableRowIterator tri = DatabaseManager.query(this.context, ARCHIVED_DATAPACKAGE_QUERY_IDS, this.journalName, max);
        List<Item> dataPackages = new ArrayList<Item>();
        while (tri.hasNext() && dataPackages.size() < max) {
            TableRow row = tri.next();
            int itemId = row.getIntColumn("item_id");
            try {
                Item dso = Item.find(context, itemId);
                dataPackages.add(dso);
            } catch (SQLException ex) {
                log.error("Error making DSO from " + itemId + ": " + ex.getMessage());
            }
        }
        return dataPackages;
    }

    /**
     * Return sorted listing of data file Items for the journal associated with
     * this object, faceted by a given field, for example, page views or data file 
     * downloads.
     * @param facetQueryField value for "&facet.query=..." parameter, e.g., "owningItem"
     * @param time query value to provide in Solr "q=time:[...]" field
     * @param max maximum number of items to return
     * @return LinkedHashMap<Item, String> of data package Items and counts. This 
     *      HashMap structure's iterator returns items in the order in which they were
     *      inserted, preserving the sort order performed in sortFilterQuery().
     */
    public LinkedHashMap<Item, String> getRequestsPerJournal(String facetQueryField, String time, int max) {
        // default solr query for site
        SolrQuery queryArgs = new SolrQuery();
        queryArgs.setQuery(time);
        queryArgs.setRows(0);
        queryArgs.set("omitHeader", "true");
        queryArgs.setFacet(true);
        Iterator<Integer> itDataFileIds = null;
        try {
            itDataFileIds = getArchivedDataFiles().iterator();
        } catch (SQLException ex) {
            log.error(ex);
            return new LinkedHashMap<Item, String>();
        }
        while(itDataFileIds.hasNext()) {
            queryArgs.addFacetQuery(facetQueryField + ":" + itDataFileIds.next());
        }
        try {
            return sortFilterQuery(doSolrPost(solrStatsUrl, queryArgs), facetQueryField, max);
        } catch (Exception ex) {
            log.error(ex);
            return new LinkedHashMap<Item, String>();
        }
    }

    /**
     * Given a solrResponse produced by a faceted query, return a list of items 
     * sorted by facet query value. Note that this method assumes
     * that the query producing the response was faceted (&facet=true) and that 
     * it had one or more query facets (&facet.query=...) that correspond to
     * Dryad Items, as is produced by getRequestsPerJournal above.
     * @param solrResponse
     * @param facetQueryField facet.query field to use as sort key, e.g., "owningItem"
     * @param max maximum number of items to return
     * @return LinkedHashMap<Item, String> of 
     * @throws SQLException 
     */
    private LinkedHashMap<Item, String> sortFilterQuery(QueryResponse solrResponse, String facetQueryField, int max) throws SQLException {
        final Map<String,Integer> facets = solrResponse.getFacetQuery();
        ArrayList<String> sortedKeys = new ArrayList<String>();
        sortedKeys.addAll(facets.keySet());
        Collections.sort(sortedKeys, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return facets.get(b).compareTo(facets.get(a));
            }
        });
        LinkedHashMap<Item, String> result = new LinkedHashMap<Item, String>();
        int pfxLen = (facetQueryField + ":").length();
        int keyMax = sortedKeys.size();
        for (int i = 0; i < keyMax && i < max; ++i) {
            String itemStr = sortedKeys.get(i);
            int itemId = Integer.valueOf(itemStr.substring(pfxLen));
            Item item = Item.find(this.context, itemId);
            result.put(item, Integer.toString(facets.get(itemStr)));
        }
        return result;
    }
    
    /**
     * Query Solr server using an HTTP POST request method to avoid the query
     * URL length limitations for a GET.
     * @param baseUrl url of solr server, e.g., http://datadryad.org/solr
     * @param solrQuery
     * @return QueryResponse
     * @throws MalformedURLException
     * @throws SolrServerException 
     */
    private QueryResponse doSolrPost(String baseUrl, SolrQuery solrQuery) throws MalformedURLException, SolrServerException {
        CommonsHttpSolrServer server = null;
        try {
            server = new CommonsHttpSolrServer(baseUrl);
        } catch (MalformedURLException ex) {
            log.error(ex);
            throw(ex);
        }
        QueryResponse response;
        try {
            response = server.query(solrQuery, SolrRequest.METHOD.POST);
        } catch (SolrServerException ex) {
            log.error(ex);
            throw(ex);
        }
        return response;
    }
}
