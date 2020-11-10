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
     * @param context the DSpace context
     * @param searchParam the searchParam
     * @return the found item
     */
    public Item search(Context context, String searchParam);

    /**
     * Search an item with the given searchParam and relationship type.
     *
     * @param context the DSpace context
     * @param searchParam the searchParam
     * @param relationshipType the item relationshipType
     * @return the found item
     */
    public Item search(Context context, String searchParam, String relationshipType);

}
