/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.datadryad.api;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.dspace.app.xmlui.aspect.journal.landing.Const;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.net.MalformedURLException;
import java.net.URI;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Convenience class for querying Solr and Postgres for data related to a given 
 * journal, as used by the journal landing pages reporting.
 * 
 * @author Nathan Day <nday@datadryad.org>
 */
public class DryadJournalStats {

    private static Logger log = Logger.getLogger(DryadJournalStats.class);

    private static final String  solrStatsUrl = ConfigurationManager.getProperty("landing-page.stats.base");
    private static final String  solrQueryFormat = ConfigurationManager.getProperty("landing-page.stats.query.format");
    private static final Integer solrBatchSize = ConfigurationManager.getIntProperty("landing-page.stats.query.downloads.batch-size", 1000);
    private static final Integer displayCount = ConfigurationManager.getIntProperty("landing-page.stats.item-count");
    private static final String  solrQueryFacetField = ConfigurationManager.getProperty("landing-page.stats.query.facet.field");
    private static final Integer threadPoolSize = ConfigurationManager.getIntProperty("landing-page.stats.query.pool-size", 1);
    private static final Long solrQueryTimeoutDuration = ConfigurationManager.getLongProperty("landing-page.stats.query.timeout", 2000);

    private static final String emptyStr = " ";
    private static final String urlSpace = "%20";
    private static final String encUTF8 = "UTF-8";

    /**
     * Executes query to Postgres to get archived data file item ids for a given
     * journal, returning the item ids.
     * @return a List of {@link Integer} values representing item.item_id values
     * @throws SQLException
     */
    public static List<Integer> getArchivedDataFiles(Context context, String journalName)
            throws SQLException
    {
        List<Integer> dataFiles = new ArrayList<Integer>();
        try {
            TableRowIterator tri = DatabaseManager.query(context, Const.archivedDataFilesQuery, journalName);
            while(tri.hasNext()) {
                TableRow row = tri.next();
                int itemId = row.getIntColumn(Const.archivedDataFilesQueryCol);
                dataFiles.add(itemId);
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
        return dataFiles;
    }

    /**
     * Return count of archived data packages for the journal associated with this object.
     * @return int count
     */
    public static long getArchivedPackagesCount(Context context, String journalName) throws SQLException
    {
        long count = 0;
        try {
            TableRowIterator tri = DatabaseManager.query(context, Const.archivedPackageCount, journalName);
            if (tri.hasNext()) {
                count = tri.next().getLongColumn(Const.archivedPackageCountCol);
            }
        } catch (Exception ex) {
            log.error(ex);
            throw new SQLException(ex.getMessage());
        }
        return count;
    }

    /**
     * Return ordered map of Item to download count.
     * @param context
     * @param items
     * @param downloadWindow
     * @return
     * @throws MalformedURLException
     */
    public static LinkedHashMap<Item, Long> getDownloadCounts(final Context context, List<Integer> items, String downloadWindow)
            throws MalformedURLException
    {
        int start = 0;
        int end = solrBatchSize;
        // query solr for a batch of data to prevent solr query length errors
        final List<String> queryUrls = new ArrayList<String>();
        do {
            if (end > items.size()) end = items.size();
            List<Integer> batch = items.subList(start, end);
            start += solrBatchSize;
            end += solrBatchSize;
            String facetQueryVals = makeSolrDownloadFacetQuery(batch, solrQueryFacetField);
            String queryString = String.format(solrQueryFormat, downloadWindow, facetQueryVals);
            queryString = queryString.replaceAll(emptyStr, urlSpace);
            queryUrls.add(solrStatsUrl + queryString);
        } while (end < items.size());
        ExecutorService exec = Executors.newFixedThreadPool(threadPoolSize);
        List<Callable<Map<Integer, Long>>> calls= new ArrayList<Callable<Map<Integer, Long>>>();
        for (final String queryString : queryUrls) {
            calls.add(makeSolrCallable(queryString));
        }
        final Map<Item, Long> results = new HashMap<Item, Long>();
        Comparator<Item> itemComp = new Comparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                // sort max-to-min
                return results.get(i2).compareTo(results.get(i1));
            }
        };
        try {
            List<Future<Map<Integer, Long>>> callResults = exec.invokeAll(calls, solrQueryTimeoutDuration, TimeUnit.MILLISECONDS);
            for (Future<Map<Integer, Long>> callResult : callResults) {
                if (callResult.isDone()) {
                    try {
                        for (Map.Entry<Integer, Long> e : callResult.get().entrySet()) {
                            try {
                                Item item = Item.find(context, e.getKey());
                                results.put(item, e.getValue());
                            } catch (SQLException e1) {
                                log.error(e1.getMessage());
                            }
                        }
                    } catch (ExecutionException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        exec.shutdownNow();
        // sort by count and return sublist
        List<Item> allItems = new ArrayList<Item>();
        allItems.addAll(results.keySet());
        Collections.sort(allItems, itemComp);
        start = 0;
        end = allItems.size() > displayCount ? displayCount : allItems.size();
        LinkedHashMap<Item, Long> refs = new LinkedHashMap<Item, Long>();
        for (Item item : allItems.subList(start, end)) {
            refs.put(item, results.get(item));
        }
        return refs;
    }

    // make a Callable to do a Solr request using a basic HTTP client, due to
    // performance issues with CommonsHttpServer
    private static Callable<Map<Integer, Long>> makeSolrCallable(final String url)
    {
        return new Callable<Map<Integer, Long>>() {
            @Override
            public Map<Integer, Long> call() throws Exception {
                CloseableHttpResponse response = null;
                Map<Integer, Long> result = new HashMap<Integer, Long>();
                try {
                    URI uri = new URI(url);
                    HttpPost httppost = new HttpPost(uri);
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    response = httpclient.execute(httppost);
                    HttpEntity entity = response.getEntity();
                    if (entity != null && entity.getContent() != null) {
                        XMLResponseParser p = new XMLResponseParser();
                        NamedList<Object> nl = p.processResponse(entity.getContent(), encUTF8);
                        QueryResponse r = new QueryResponse();
                        r.setResponse(nl);
                        List<FacetField.Count> counts = r.getFacetFields().get(0).getValues();
                        for (FacetField.Count count : counts) {
                            result.put(Integer.parseInt(count.getName()), count.getCount());
                        }
                    }
                } catch (SolrException e) {
                    log.debug(e.getMessage());
                } catch (Exception e) {
                    log.error(e.getMessage());
                } finally {
                    try {
                        if (response != null)
                            response.close();
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
                return result;
            }
        };
    }

    // make a facet query given facet query field and field values
    private static String makeSolrDownloadFacetQuery(List<Integer> list, String facetQueryField)
    {
        StringBuilder q = new StringBuilder();
        for (int i = 0; i < list.size(); ++i) {
            if (i != 0) q.append(" OR ");
            q.append(facetQueryField);
            q.append(":");
            q.append(list.get(i));
        }
        return q.toString();
    }
}

