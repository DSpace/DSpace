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
 * Interface for classes that search for items based on metadata values.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemSearcher {

    /**
     * Searches the entire repository to find a single Item that matches the provided
     * identifier.
     * * <p>Depending on the implementation, the search may be performed by:
     * <ul>
     * <li>Internal Database ID (UUID)</li>
     * <li>External Metadata Identifier (e.g., ORCID, DOI, Scopus ID) via Discovery</li>
     * </ul>
     * </p>
     *
     * @param context     the DSpace context
     * @param searchParam the unique value to search for (e.g., "0000-0002-1825-0097")
     * @param source      the item currently being processed (optional, used for
     * caching or to track resolution attempts)
     * @return the matched DSpace Item, or null if no item in the repository matches
     * the parameter.
     */
    Item searchBy(Context context, String searchParam, Item source);
}
