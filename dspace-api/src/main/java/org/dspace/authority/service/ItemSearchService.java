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
     * Search an item with the given searchParam.
     *
     * @param context     the DSpace context
     * @param searchParam the searchParam
     * @return the found item
     */
    default Item search(Context context, String searchParam) {
        return search(context, searchParam, null);
    }

    /**
     * Search an item with the given searchParam.
     *
     * @param context     the DSpace context
     * @param searchParam the searchParam
     * @param source      the source item
     * @return the found item
     */
    Item search(Context context, String searchParam, Item source);

    /**
     * Search an item with the given searchParam and relationship type.
     *
     * @param context     the DSpace context
     * @param searchParam the searchParam
     * @param entityType  the item entityType
     * @param source      the source item
     * @return the found item
     */
    Item search(Context context, String searchParam, String entityType, Item source);

}
