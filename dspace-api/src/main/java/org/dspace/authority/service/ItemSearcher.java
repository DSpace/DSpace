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
 * Interface for classes that allow to search an item by the given search param string.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemSearcher {

    /**
     * Search a single item by the given search param.
     *
     * @param context     the DSpace context
     * @param searchParam the search param
     * @param source      the source item
     * @return the found item
     */
    Item searchBy(Context context, String searchParam, Item source);
}
