/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BitstreamFormatBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.MetadataSchemaBuilder;
import org.dspace.builder.OrcidHistoryBuilder;
import org.dspace.builder.OrcidQueueBuilder;
import org.dspace.builder.OrcidTokenBuilder;
import org.dspace.builder.PoolTaskBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.builder.RequestItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.SiteBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;

/**
 * This class will ensure that all the builders that are registered will be cleaned up in the order as defined
 * in the constructor. This will ensure foreign-key constraint safe deletion of the objects made with these
 * builders.
 */
public class AbstractBuilderCleanupUtil {

    private final LinkedHashMap<String, List<AbstractBuilder>> map
            = new LinkedHashMap<>();

    /**
     * Constructor that will initialize the Map with a predefined order for deletion.
     * <P>
     * Objects are deleted top-to-bottom in this Map. Objects that need to be removed *first*
     * should appear at the top. Objects that may be deleted later, appear at the bottom.
     */
    public AbstractBuilderCleanupUtil() {
        initMap();
    }

    private void initMap() {
        map.put(OrcidQueueBuilder.class.getName(), new ArrayList<>());
        map.put(OrcidHistoryBuilder.class.getName(), new ArrayList<>());
        map.put(OrcidTokenBuilder.class.getName(), new ArrayList<>());
        map.put(ResourcePolicyBuilder.class.getName(), new ArrayList<>());
        map.put(RelationshipBuilder.class.getName(), new ArrayList<>());
        map.put(RequestItemBuilder.class.getName(), new ArrayList<>());
        map.put(PoolTaskBuilder.class.getName(), new ArrayList<>());
        map.put(WorkflowItemBuilder.class.getName(), new ArrayList<>());
        map.put(WorkspaceItemBuilder.class.getName(), new ArrayList<>());
        map.put(BitstreamBuilder.class.getName(), new ArrayList<>());
        map.put(BitstreamFormatBuilder.class.getName(), new ArrayList<>());
        map.put(ClaimedTaskBuilder.class.getName(), new ArrayList<>());
        map.put(BundleBuilder.class.getName(), new ArrayList<>());
        map.put(ItemBuilder.class.getName(), new ArrayList<>());
        map.put(CollectionBuilder.class.getName(), new ArrayList<>());
        map.put(CommunityBuilder.class.getName(), new ArrayList<>());
        map.put(GroupBuilder.class.getName(), new ArrayList<>());
        map.put(EPersonBuilder.class.getName(), new ArrayList<>());
        map.put(RelationshipTypeBuilder.class.getName(), new ArrayList<>());
        map.put(EntityTypeBuilder.class.getName(), new ArrayList<>());
        map.put(MetadataFieldBuilder.class.getName(), new ArrayList<>());
        map.put(MetadataSchemaBuilder.class.getName(), new ArrayList<>());
        map.put(SiteBuilder.class.getName(), new ArrayList<>());
        map.put(ProcessBuilder.class.getName(), new ArrayList<>());
    }

    /**
     * Adds a builder to the map.
     * This will make a new linkedList if the name doesn't exist yet as a key in the map with a list, if it already
     * exists it will simply add the AbstractBuilder to that list.
     * @param abstractBuilder   The AbstractBuilder to be added
     */
    public void addToMap(AbstractBuilder abstractBuilder) {
        map.computeIfAbsent(abstractBuilder.getClass().getName(), k -> new ArrayList<>()).add(abstractBuilder);
    }

    /**
     * This method takes care of iterating over all the AbstractBuilders in the predefined order and calls
     * the cleanup method to delete the objects from the database.
     * @throws Exception    If something goes wrong
     */
    public void cleanupBuilders() throws Exception {
        for (Map.Entry<String, List<AbstractBuilder>> entry : map.entrySet()) {
            List<AbstractBuilder> list = entry.getValue();
            for (AbstractBuilder abstractBuilder : list) {
                abstractBuilder.cleanup();
            }
        }
    }

    /**
     * Clears and re-initialises the map of builders
     */
    public void cleanupMap() {
        this.map.clear();
        initMap();
    }
}
