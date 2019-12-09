/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder.util;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.builder.AbstractBuilder;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.BitstreamFormatBuilder;
import org.dspace.app.rest.builder.ClaimedTaskBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.EntityTypeBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.MetadataFieldBuilder;
import org.dspace.app.rest.builder.MetadataSchemaBuilder;
import org.dspace.app.rest.builder.PoolTaskBuilder;
import org.dspace.app.rest.builder.ProcessBuilder;
import org.dspace.app.rest.builder.RelationshipBuilder;
import org.dspace.app.rest.builder.RelationshipTypeBuilder;
import org.dspace.app.rest.builder.SiteBuilder;
import org.dspace.app.rest.builder.WorkflowItemBuilder;
import org.dspace.app.rest.builder.WorkspaceItemBuilder;

/**
 * This class will ensure that all the builders that are registered will be cleaned up in the order as defined
 * in the constructor. This will ensure foreign-key constraint safe deletion of the objects made with these
 * builders
 */
public class AbstractBuilderCleanupUtil {

    private LinkedHashMap<String, List<AbstractBuilder>> map = new LinkedHashMap<>();

    /**
     * Constructor that will initialize the Map with a predefined order for deletion
     */
    public AbstractBuilderCleanupUtil() {
        map.put(RelationshipBuilder.class.getName(), new LinkedList<>());
        map.put(RelationshipTypeBuilder.class.getName(), new LinkedList<>());
        map.put(EntityTypeBuilder.class.getName(), new LinkedList<>());
        map.put(PoolTaskBuilder.class.getName(), new LinkedList<>());
        map.put(WorkflowItemBuilder.class.getName(), new LinkedList<>());
        map.put(WorkspaceItemBuilder.class.getName(), new LinkedList<>());
        map.put(BitstreamBuilder.class.getName(), new LinkedList<>());
        map.put(BitstreamFormatBuilder.class.getName(), new LinkedList<>());
        map.put(ClaimedTaskBuilder.class.getName(), new LinkedList<>());
        map.put(CollectionBuilder.class.getName(), new LinkedList<>());
        map.put(CommunityBuilder.class.getName(), new LinkedList<>());
        map.put(EPersonBuilder.class.getName(), new LinkedList<>());
        map.put(GroupBuilder.class.getName(), new LinkedList<>());
        map.put(ItemBuilder.class.getName(), new LinkedList<>());
        map.put(MetadataFieldBuilder.class.getName(), new LinkedList<>());
        map.put(MetadataSchemaBuilder.class.getName(), new LinkedList<>());
        map.put(SiteBuilder.class.getName(), new LinkedList<>());
        map.put(ProcessBuilder.class.getName(), new LinkedList<>());

    }

    /**
     * Adds a builder to the map.
     * This will make a new linkedList if the name doesn't exist yet as a key in the map with a list, if it already
     * exists it will simply add the AbstractBuilder to that list.
     * @param abstractBuilder   The AbstractBuilder to be added
     */
    public void addToMap(AbstractBuilder abstractBuilder) {
        map.computeIfAbsent(abstractBuilder.getClass().getName(), k -> new LinkedList<>()).add(abstractBuilder);
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
}
