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
 * Interface for classes that allow to search all the items that refer the given
 * one using the will be refereced authority.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemReferenceResolver {

    /**
     * Resolve all the references to the given one via will be referenced authority.
     *
     * @param  context the DSpace Context
     * @param  item    the item to search for
     * @return         the found items
     */
    void resolveReferences(Context context, Item item);
}
