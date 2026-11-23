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
 * one using the "will be referenced" authority prefix.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemReferenceResolver {

    /**
     * Resolves all placeholder references pointing to the given item.
     * <p>
     * When items are created, they may refer to entities that do not yet exist in the
     * database using a placeholder authority format:
     * {@code will be referenced::PREFIX::IDENTIFIER} (e.g., {@code will be referenced::ORCID::0000-0002-1825-0097}).
     * </p>
     * <p>
     * This method searches for all items in the repository containing these placeholders
     * and "resolves" them by replacing the placeholder string with the actual
     * UUID of the newly created {@code item}.
     * </p>
     *
     * @param context the DSpace Context
     * @param item    the newly created or existing item that is the target of the references
     */
    void resolveReferences(Context context, Item item);

    /**
     * Clears the resolver cache if any is used
     */
    void clearCache();
}
