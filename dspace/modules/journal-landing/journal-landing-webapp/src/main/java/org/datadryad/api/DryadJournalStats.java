/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.datadryad.api;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.journal.landing.Const;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Convenience class for querying Solr and Postgres for data related to a given 
 * journal, as used by the journal landing pages reporting.
 * 
 * @author Nathan Day <nday@datadryad.org>
 */
public class DryadJournalStats {

    private static Logger log = Logger.getLogger(DryadJournalStats.class);

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
     * Return a sorted map of archived data packages (Item objects) for the journal
     * associated with this object. The data packages are sorted according to 
     * date-accessioned, with most recently accessioned package first.
     * @param max total number of items to return
     * @return List<org.dspace.content.Item> data packages
     * @throws SQLException 
     */
    public static LinkedHashMap<Item,String> getArchivedPackagesSortedRecent(Context context, String journalName, SimpleDateFormat fmt, int max)
        throws SQLException
    {
        LinkedHashMap<Item,String> dataPackages = new LinkedHashMap<Item,String>(max);
        try {
            TableRowIterator tri = DatabaseManager.query(context, Const.archivedDataPackageIds, journalName, max);
            while (tri.hasNext() && dataPackages.size() < max) {
                int itemId = tri.next().getIntColumn(Const.archivedDataPackageIdsCol);
                Item dso = Item.find(context, itemId);
                DCValue[] dateAccessioned = dso.getMetadata(Const.dcDateAccessioned);
                String dateStr = fmt.format(fmt.parse(dateAccessioned[0].value));
                dataPackages.put(dso, dateStr);
            }
        } catch (Exception e)  {
            throw new SQLException(e.getMessage());
        }
        return dataPackages;
    }

    /**
     * Make a facet-query substring, grouping ranges if possible
     * @param  list facet query values
     * @param facetQueryField
     * @return
     */
    public static String makeSolrDownloadFacetQuery(List<Integer> list, String facetQueryField) {
        StringBuilder q = new StringBuilder();
        Iterator<Integer> it = list.iterator();
        if (it.hasNext()) {
            Integer start = it.next();
            Integer end = start;
            if (it.hasNext()) {
                while (it.hasNext()) {
                    Integer next = it.next();
                    if ((end + 1) == next) {
                        end = next;
                    } else {
                        // single item
                        if (start == end) {
                            if (q.length() != 0) q.append(" OR ");
                            q.append(facetQueryField);
                            q.append(":");
                            q.append(start);
                        } else if (end == start + 1) {
                            if (q.length() != 0) q.append(" OR ");
                            q.append(facetQueryField);
                            q.append(":");
                            q.append(start);
                            q.append(" OR ");
                            q.append(facetQueryField);
                            q.append(":");
                            q.append(end);
                        } else {
                            if (q.length() != 0) q.append(" OR ");
                            q.append(facetQueryField);
                            q.append(String.format(":[%d TO %d]", start, end));
                        }
                        start = next;
                        end = next;
                        if (!it.hasNext()) {
                            if (q.length() != 0) q.append(" OR ");
                            q.append(facetQueryField);
                            q.append(":");
                            q.append(next);
                        }
                    }
                }
            // single item
            } else {
                q.append(facetQueryField);
                q.append(":");
                q.append(start);
            }
        }
        return q.toString();
    }
}

