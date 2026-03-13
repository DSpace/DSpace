/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.dspace.app.util.CollectionDropDown;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

/**
 * @author LINDAT/CLARIN dev team
 */
public class ItemCheck extends Check {

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private XmlWorkflowItemService workflowItemService =
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
    private HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();


    @Override
    public String run(ReportInfo ri) {
        String ret = "";
        int tot_cnt = 0;
        Context context = new Context();
        try {
            for (Map.Entry<String, Integer> name_count : getCommunities(context)) {
                ret += "Community [%s]: %d\n".formatted(
                    name_count.getKey(), name_count.getValue());
                tot_cnt += name_count.getValue();
            }
        } catch (SQLException e) {
            error(e);
        }

        try {
            ret += "\nCollection sizes:\n";
            ret += getCollectionSizesInfo(context);
        } catch (SQLException e) {
            error(e);
        }

        ret += "\nPublished items (archived, not withdrawn): %d\n".formatted(tot_cnt);
        try {
            ret += "Withdrawn items: %d\n".formatted(itemService.countWithdrawnItems(context));
            ret += "Not published items (in workspace or workflow mode): %d\n".formatted(
                itemService.countNotArchivedItems(context));

            for (Map.Entry<Integer, Long> row : workspaceItemService.getStageReachedCounts(context)) {
                ret += "\tIn Stage %s: %s\n".formatted(
                    row.getKey(), //"stage_reached"
                    row.getValue() //"cnt"
                );
            }

            ret += "\tWaiting for approval (workflow items): %d\n".formatted(
                workflowItemService.countAll(context));

        } catch (SQLException e) {
            error(e);
        }

        try {
            ret += getObjectSizesInfo(context);
            context.complete();
        } catch (SQLException e) {
            error(e);
        }
        return ret;
    }


    public String getObjectSizesInfo(Context context) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("Count %-14s: %s\n".formatted("Bitstream",
            String.valueOf(bitstreamService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("Bundle",
            String.valueOf(bundleService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("Collection",
            String.valueOf(collectionService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("Community",
            String.valueOf(communityService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("MetadataValue",
            String.valueOf(metadataValueService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("EPerson",
            String.valueOf(ePersonService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("Item",
            String.valueOf(itemService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("Handle",
            String.valueOf(handleService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("Group",
            String.valueOf(groupService.countTotal(context))));
        sb.append("Count %-14s: %s\n".formatted("BasicWorkflowItem",
            String.valueOf(workflowItemService.countAll(context))));
        sb.append("Count %-14s: %s\n".formatted("WorkspaceItem",
            String.valueOf(workspaceItemService.countTotal(context))));
        return sb.toString();
    }

    public String getCollectionSizesInfo(final Context context) throws SQLException {
        final StringBuffer ret = new StringBuffer();
        List<Map.Entry<Collection, Long>> colBitSizes = collectionService
            .getCollectionsWithBitstreamSizesTotal(context);
        long total_size = 0;

        Collections.sort(colBitSizes, new Comparator<Map.Entry<Collection, Long>>() {
            @Override
            public int compare(Map.Entry<Collection, Long> o1, Map.Entry<Collection, Long> o2) {
                try {
                    return CollectionDropDown.collectionPath(context, o1.getKey()).compareTo(
                        CollectionDropDown.collectionPath(context, o2.getKey())
                    );
                } catch (Exception e) {
                    ret.append(e.getMessage());
                }
                return 0;
            }
        });
        for (Map.Entry<Collection, Long> row : colBitSizes) {
            Long size = row.getValue();
            total_size += size;
            Collection col = row.getKey();
            ret.append("\t%s:  %s\n".formatted(CollectionDropDown.collectionPath(context, col),
                FileUtils.byteCountToDisplaySize((long) size)));
        }
        ret.append("Total size:              %s\n".formatted(FileUtils.byteCountToDisplaySize(total_size)));

        ret.append("Resource without policy: %d\n".formatted(bitstreamService.countBitstreamsWithoutPolicy(context)));

        ret.append("Deleted bitstreams:      %d\n".formatted(bitstreamService.countDeletedBitstreams(context)));

        String list_str = "";
        List<Bitstream> bitstreamOrphans = bitstreamService.getNotReferencedBitstreams(context);
        for (Bitstream orphan : bitstreamOrphans) {
            UUID id = orphan.getID();
            list_str += "%s, ".formatted(id);
        }
        ret.append("Orphan bitstreams:       %d [%s]\n".formatted(bitstreamOrphans.size(), list_str));

        return ret.toString();
    }

    public List<Map.Entry<String, Integer>> getCommunities(Context context)
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
}
