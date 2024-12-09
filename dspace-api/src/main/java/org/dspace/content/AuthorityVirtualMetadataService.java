/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;

/**
 * Simple service to handle authority virtual metadata operations.
 * Separated from item service for better separation of concerns and because the field lookups
 * required by this service require a context which is not provided in
 * {@link org.dspace.content.service.ItemService#getMetadata}
 *
 * @author Kim Shepherd
 */
public interface AuthorityVirtualMetadataService {

    /**
     * This method retrieves a list of authority virtual metadata values for a given item.
     *
     * @param item The item for which to retrieve the authority virtual metadata values.
     * @param dbMetadataValues The list of regular metadata values associated with the item.
     * @return The list of authority virtual metadata values for the item.
     */
    List<MetadataValue> getAuthorityVirtualMetadata(Item item, List<MetadataValue> dbMetadataValues);

}
