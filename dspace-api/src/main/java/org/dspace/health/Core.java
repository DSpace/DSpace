/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import org.apache.commons.io.FileUtils;
import org.dspace.app.util.CollectionDropDown;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.*;

/**
 * @author LINDAT/CLARIN dev team
 */
public class Core {

    private static AbstractHibernateDAO dao = new AbstractHibernateDAO<Object>(){};
    private static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    private final Context context;

    public Core(Context context){
        this.context = context;
    }

    // get info
    //
    public  String getCollectionSizesInfo() throws SQLException {
        final StringBuffer ret = new StringBuffer();
        List<Object[]> col_bitSizes = query("select col, sum(bit.sizeBytes) as sum from Item i join i.collections col join i.bundles bun join bun.bitstreams bit group by col");
        long total_size = 0;

        Collections.sort(col_bitSizes, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] o1, Object[] o2) {
                try {
                    return CollectionDropDown.collectionPath((Collection) o1[0]).compareTo(
                        CollectionDropDown.collectionPath((Collection) o2[0])
                    );
                } catch (Exception e) {
                    ret.append(e.getMessage());
                }
                return 0;
            }
        });
        for (Object[] row : col_bitSizes) {
            Long size = (Long) row[1];
            total_size += size;
            Collection col = (Collection) row[0];
            ret.append(String.format(
                    "\t%s:  %s\n", CollectionDropDown.collectionPath(col), FileUtils.byteCountToDisplaySize((long) size)));
        }
        ret.append(String.format(
                "Total size:              %s\n", FileUtils.byteCountToDisplaySize(total_size)));

        ret.append(String.format(
                "Resource without policy: %d\n", getBitstreamsWithoutPolicyCount()));

        ret.append(String.format(
                "Deleted bitstreams:      %d\n", getBitstreamsDeletedCount()));

        String list_str = "";
        List<UUID> bitstreamOrphans = getBitstreamOrphansRows();
        for (UUID id : bitstreamOrphans) {
            list_str += String.format("%d, ", id);
        }
        ret.append(String.format(
                "Orphan bitstreams:       %d [%s]\n", bitstreamOrphans.size(), list_str));

        return ret.toString();
    }

    public  String getObjectSizesInfo() throws SQLException {
        String ret = "";
        for (String tb : new String[] { "Bitstream", "Bundle", "Collection",
            "Community", "MetadataValue", "EPerson", "Item", "Handle",
            "Group", "BasicWorkflowItem", "WorkspaceItem", }) {
            int count = count("SELECT COUNT(*) from " + tb);
            ret += String.format("Count %-14s: %s\n", tb,
                String.valueOf(count));
        }
        return ret;
    }


    // get objects
    //

    public  List<Object[]> getWorkspaceItemsRows() throws SQLException {
        return query("SELECT wi.stageReached, count(*) as cnt from WorkspaceItem wi group by wi.stageReached order by wi.stageReached");
    }

    public  List<UUID> getBitstreamOrphansRows() throws SQLException {
        return query("select bit.id from Bitstream bit where bit.deleted != true" +
                " and bit.id not in (select bit2.id from Bundle bun join bun.bitstreams bit2)" +
                " and bit.id not in (select com.logo.id from Community com)" +
                " and bit.id not in (select col.logo.id from Collection col)" +
                " and bit.id not in (select bun.primaryBitstream.id from Bundle bun)");
    }

    // get sizes
    //

    public  int getWorkflowItemsCount() throws SQLException {
        return count("SELECT count(*) FROM BasicWorkflowItem");
    }

    public  int getNotArchivedItemsCount() throws SQLException {
        return count(
            "SELECT count(*) FROM Item i WHERE i.inArchive=false AND i.withdrawn=false");
    }

    public  int getWithdrawnItemsCount() throws SQLException {
        return count("SELECT count(*) FROM Item i WHERE i.withdrawn=true");
    }

    public  int getBitstreamsWithoutPolicyCount() throws SQLException {
        return count("SELECT count(bit.id) from Bitstream bit where bit.deleted<>true and bit.id not in" +
                " (select res.dSpaceObject from ResourcePolicy res where res.resourceTypeId=" + Constants.BITSTREAM +")");
    }

    public  int getBitstreamsDeletedCount() throws SQLException {
        return count("SELECT count(*) FROM Bitstream b WHERE b.deleted=true");
    }

    // get more complex information
    //

    public  List<Map.Entry<String, Integer>> getCommunities()
        throws SQLException {

        List<Map.Entry<String, Integer>> cl = new java.util.ArrayList<>();
        List<Community> top_communities = communityService.findAllTop(context);
        for (Community c : top_communities) {
            cl.add(
                new java.util.AbstractMap.SimpleEntry<>(c.getName(), itemService.countItems(context, c))
            );
        }
        return cl;
    }

    public  List<String> getEmptyGroups() throws SQLException {
        List<String> ret = new ArrayList<>();
        List<Group> emptyGroups = query("SELECT g from Group g where g.epeople is EMPTY");
        for (Group group : emptyGroups) {
            ret.add(String.format("id=%s;name=%s", group.getID(), group.getName() ));
        }
        return ret;
    }

    public  List<UUID> getSubscribers() throws SQLException {
        return query("SELECT DISTINCT e.id from Subscription s join s.ePerson e");
    }

    public  List<UUID> getSubscribedCollections() throws SQLException {
        return query("SELECT DISTINCT col.id FROM Subscription s join  s.collection col");
    }

    //
    //

     int count(String query) throws SQLException {
        return dao.count(dao.createQuery(context, query));
    }

     List query(String query) throws SQLException {
        return dao.createQuery(context, query).list();
    }
}



