/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * by lindat-dev team
 */
package org.dspace.health;

import org.apache.commons.io.FileUtils;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Core {

    // get info
    //
    public static String getCollectionSizesInfo() throws SQLException {
        String ret = "";
        List<TableRow> rows = sql(
            "SELECT "
                + "(SELECT text_value FROM metadatavalue "
                + "WHERE metadata_field_id=64 AND resource_type_id=3 AND resource_id=col.collection_id) AS name, "
                + "SUM(bit.size_bytes) AS sum "
                + "FROM collection2item col, item2bundle item, bundle2bitstream bun, bitstream bit "
                + "WHERE col.item_id=item.item_id AND item.bundle_id=bun.bundle_id AND bun.bitstream_id=bit.bitstream_id "
                + "GROUP BY col.collection_id;");
        long total_size = 0;
        for (TableRow row : rows) {
            double size = row.getLongColumn("sum") / (1024. * 1024.);
            total_size += size;
            ret += String.format(
                "\t%s:  %s\n", row.getStringColumn("name"), FileUtils.byteCountToDisplaySize((long)size));
        }
        ret += String.format(
            "Total size:              %s\n", FileUtils.byteCountToDisplaySize(total_size));

        ret += String.format(
            "Resource without policy: %d\n", getBitstreamsWithoutPolicyCount());

        ret += String.format(
            "Deleted bitstreams:      %d\n", getBitstreamsDeletedCount());

        rows = getBitstreamOrphansRows();
        String list_str = "";
        for (TableRow row : rows) {
            list_str += String.format("%d, ", row.getIntColumn("bitstream_id"));
        }
        ret += String.format(
            "Orphan bitstreams:       %d [%s]\n", rows.size(), list_str);

        return ret;
    }

    public static String getObjectSizesInfo() throws SQLException {
        String ret = "";
        Context c = new Context();

        for (String tb : new String[] { "bitstream", "bundle", "collection",
            "community", "dcvalue", "eperson", "item", "handle",
            "epersongroup", "workflowitem", "workspaceitem", }) {
            TableRowIterator irows = DatabaseManager.query(c,
                "SELECT COUNT(*) from " + tb);
            List<TableRow> rows = irows.toList();
            ret += String.format("Count %s: %s\n", tb,
                String.valueOf(rows.get(0).getLongColumn("count")));
        }

        c.complete();
        return ret;
    }


    // get objects
    //

    public static List<TableRow> getWorkspaceItemsRows() throws SQLException {
        return sql("SELECT stage_reached, count(1) AS cnt FROM workspaceitem GROUP BY stage_reached ORDER BY stage_reached;");
    }

    public static List<TableRow> getBitstreamOrphansRows() throws SQLException {
        return sql("SELECT bitstream_id FROM bitstream WHERE deleted<>true AND bitstream_id "
            + "NOT IN ("
            + "SELECT bitstream_id FROM bundle2bitstream "
            + "UNION SELECT logo_bitstream_id FROM community WHERE logo_bitstream_id IS NOT NULL "
            + "UNION SELECT primary_bitstream_id FROM bundle WHERE primary_bitstream_id IS NOT NULL ORDER BY bitstream_id "
            + ")");

    }

    public static List<TableRow> getSubscribersRows() throws SQLException {
        return sql("SELECT DISTINCT ON (eperson_id) eperson_id FROM subscription");
    }

    public static List<TableRow> getSubscribedCollectionsRows() throws SQLException {
        return sql("SELECT DISTINCT ON (collection_id) collection_id FROM subscription");
    }

    public static List<TableRow> getHandlesInvalidRows() throws SQLException {
        List<TableRow> rows = sql("SELECT * FROM handle "
            + " WHERE NOT ("
            + "     (handle IS NOT NULL AND resource_type_id IS NOT NULL AND resource_id IS NOT NULL)"
            + " OR " + "     (handle IS NOT NULL AND url IS NOT NULL)"
            + " ) ");
        return rows;
    }

    // get sizes
    //

    public static int getItemsTotalCount() throws SQLException {
        int total = 0;
        for (java.util.Map.Entry<String, Integer> name_count : getCommunities()) {
            total += name_count.getValue();
        }
        return total;
    }

    public static int getWorkflowItemsCount() throws SQLException {
        return sql("SELECT * FROM workflowitem;").size();
    }

    public static int getNotArchivedItemsCount() throws SQLException {
        return sql(
            "SELECT * FROM item WHERE in_archive=false AND withdrawn=false").size();
    }

    public static int getWithdrawnItemsCount() throws SQLException {
        return sql("SELECT * FROM item WHERE withdrawn=true").size();
    }

    public static int getBitstreamsWithoutPolicyCount() throws SQLException {
        return sql(
            "SELECT bitstream_id FROM bitstream WHERE deleted<>true AND bitstream_id NOT IN "
                + "(SELECT resource_id FROM resourcepolicy WHERE resource_type_id=0)")
            .size();
    }

    public static int getBitstreamsDeletedCount() throws SQLException {
        return sql("SELECT * FROM bitstream WHERE deleted=true").size();
    }

    public static long getHandlesTotalCount() throws SQLException {
        List<TableRow> rows = sql("SELECT count(1) AS cnt FROM handle");
        return rows.get(0).getLongColumn("cnt");
    }


    // get more complex information
    //

    public static List<Map.Entry<String, Integer>> getCommunities()
        throws SQLException {

        List<Map.Entry<String, Integer>> cl = new java.util.ArrayList<>();
        Context context = new Context();
        Community[] top_communities = Community.findAllTop(context);
        for (Community c : top_communities) {
            cl.add(
                new java.util.AbstractMap.SimpleEntry<>(c.getName(), c.countItems())
            );
        }
        context.complete();
        return cl;
    }

    public static List<String> getEmptyGroups() throws SQLException {
        List<String> ret = new ArrayList<>();
        Context c = new Context();
        TableRowIterator irows = DatabaseManager
            .query(c,
                "SELECT eperson_group_id, "
                    + "(SELECT text_value FROM metadatavalue "
                    + "WHERE metadata_field_id=64 AND resource_type_id=6 AND resource_id=eperson_group_id) AS name "
                    + "FROM epersongroup "
                    + "WHERE eperson_group_id NOT IN (SELECT eperson_group_id FROM epersongroup2eperson)");
        for (TableRow row : irows.toList()) {
            ret.add( row.getStringColumn("name") );
        }
        c.complete();
        return ret;
    }

    public static List<Integer> getSubscribers() throws SQLException {
        List<Integer> ret = new ArrayList<>();
        for (TableRow row : getSubscribersRows()) {
            ret.add(row.getIntColumn("eperson_id"));
        }
        return ret;
    }

    public static List<Integer> getSubscribedCollections() throws SQLException {
        List<Integer> ret = new ArrayList<>();
        for (TableRow row : getSubscribedCollectionsRows()) {
            ret.add(row.getIntColumn("collection_id"));
        }
        return ret;
    }

    @SuppressWarnings("deprecation")
    public static Map<String, String> getItemRightsInfo() {
        Map<String, String> ret = new HashMap<>();
        Map<String, Integer> info = new HashMap<>();
        try {
            Context context = new Context();
            ItemIterator it = Item.findAll(context);
            while (it.hasNext()) {
                Item i = it.next();
                Metadatum[] labels = i.getMetadata("dc", "rights", "label",
                        Item.ANY);
                String pub_dc_value = "";

                if (labels.length > 0) {
                    for (Metadatum dc : labels) {
                        if (pub_dc_value.length() == 0) {
                            pub_dc_value = dc.value;
                        } else {
                            pub_dc_value = pub_dc_value + " " + dc.value;
                        }
                    }
                } else {
                    pub_dc_value = "no licence";
                }

                if (!info.containsKey(pub_dc_value)) {
                    info.put(pub_dc_value, 0);
                }
                info.put(pub_dc_value, info.get(pub_dc_value) + 1);
            }
            context.complete();

            for (Map.Entry<String, Integer> e : info.entrySet()) {
                ret.put(e.getKey(), String.valueOf(e.getValue()));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    //
    //

    static List<TableRow> sql(String sql) throws SQLException {
        Context c = new Context();
        List<TableRow> ret = DatabaseManager.query(c, sql).toList();
        c.complete();
        return ret;
    }

}

