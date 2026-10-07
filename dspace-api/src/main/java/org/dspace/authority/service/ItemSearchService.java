/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service to search an item with a specific strategy.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemSearchService {

    /**
     * Performs a global search for an item using a unique identifier.
     * This is a convenience method for {@link #search(Context, String, Item)} with no source context.
     *
     * @param context     the DSpace context
     * @param searchParam the identifier (UUID, CRIS sourceId, or prefixed metadata value)
     * @return the found item, or null if no match is found
     */
    default Item search(Context context, String searchParam) {
        return search(context, searchParam, null);
    }

    /**
     * Performs a contextual search for an item. The search attempts to resolve the
     * identifier through multiple strategies (UUID, local CRIS ID, or external
     * authority mappers).
     *
     * @param context     the DSpace context
     * @param searchParam the identifier to search for
     * @param source      the item that triggered the search, used for tracking
     * resolution attempts or providing context to specialized searchers
     * @return the found item, or null if no match is found
     */
    Item search(Context context, String searchParam, Item source);

    /**
     * Performs a type-strict search for an item. Matches are only returned if the
     * resulting item has a {@code dspace.entity.type} that matches the provided
     * entityType.
     *
     * @param context     the DSpace context
     * @param searchParam the identifier to search for
     * @param entityType  the required dspace.entity.type (e.g., "Person", "OrgUnit")
     * @param source      the item that triggered the search
     * @return the found item that matches both the identifier and the entity type,
     * or null if no match is found or if the type does not align
     */
    Item search(Context context, String searchParam, String entityType, Item source);

}
