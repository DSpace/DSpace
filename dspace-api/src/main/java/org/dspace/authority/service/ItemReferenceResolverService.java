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
 * Service to resolve all the configured will be referenced authorities that
 * refer the given item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemReferenceResolverService {

    /**
     * Resolve all the references to the given one via will be referenced authority.
     *
     * @param context the DSpace Context
     * @param item    the item to search for
     */
    void resolveReferences(Context context, Item item);

    /**
     * Clears the resolver cache if any is used
     */
    void clearResolversCache();
}
