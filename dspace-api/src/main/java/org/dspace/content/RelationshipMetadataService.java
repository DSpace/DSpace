/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;

import org.dspace.content.virtual.VirtualMetadataPopulator;

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
}
