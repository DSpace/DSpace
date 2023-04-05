/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.virtual.VirtualMetadataPopulator;
import org.dspace.core.Context;

/**
 * Interface used for the {@link RelationshipMetadataServiceImpl}
 * This will define methods regarding the RelationshipMetadata
 */
public interface RelationshipMetadataService {

    /**
     * This method retrieves a list of MetadataValue objects that get constructed from processing
     * the given Item's Relationships through the config given to the {@link VirtualMetadataPopulator}
     * @param item  The Item that will be processed through it's Relationships
     * @param enableVirtualMetadata This parameter will determine whether the list of Relationship metadata
     *                              should be populated with metadata that is being generated through the
     *                              VirtualMetadataPopulator functionality or not
     * @return      The list of MetadataValue objects constructed through the Relationships
     */
    public List<RelationshipMetadataValue> getRelationshipMetadata(Item item, boolean enableVirtualMetadata);

    /**
     * Retrieves the list of RelationshipMetadataValue objects specific to only one Relationship of the item.
     *
     * This method processes one Relationship of an Item and will return a list of RelationshipMetadataValue objects
     * that are generated for this specific relationship for the item through the config in VirtualMetadataPopulator
     *
     * It returns a combination of the output of the findVirtualMetadataFromConfiguration method and
     * the getRelationMetadataFromOtherItem method.
     *
     * @param context               The context
     * @param item                  The item whose virtual metadata is requested
     * @param entityType            The entity type of the given item
     * @param relationship          The relationship whose virtual metadata is requested
     * @param enableVirtualMetadata Determines whether the VirtualMetadataPopulator should be used.
     *                              If false, only the relation."relationname" metadata is populated
     *                              If true, fields from the spring config virtual metadata is included as well
     * @return                      The list of virtual metadata values
     */
    public List<RelationshipMetadataValue> findRelationshipMetadataValueForItemRelationship(
        Context context, Item item, String entityType, Relationship relationship, boolean enableVirtualMetadata)
        throws SQLException;

    /**
     * This method will retrieve the EntityType String from an item
     * @param item  The Item for which the entityType String will be returned
     * @return      A String value indicating the entityType
     * @deprecated use {@link org.dspace.content.service.ItemService#getEntityTypeLabel(Item)} instead.
     */
    @Deprecated
    public String getEntityTypeStringFromMetadata(Item item);

}
