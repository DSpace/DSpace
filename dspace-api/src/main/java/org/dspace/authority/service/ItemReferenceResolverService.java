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
 * Service to resolve all the configured "will be referenced" authority references.
 * 
 * This service orchestrates the resolution of authority references across multiple
 * metadata fields. When an entity (e.g., a Person) is created or updated, this service
 * searches for all items that reference it using placeholder authority values
 * (e.g., "will be referenced::ORCID::...") and updates them to use the actual
 * item UUID.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface ItemReferenceResolverService {

    /**
     * Resolve all the references to the given item by updating placeholder authority
     * values to use the actual item UUID. This allows linking related items
     * (e.g., publications referencing a person) after the target entity has been created.
     *
     * @param context the DSpace Context
     * @param item    the item to resolve references for
     */
    void resolveReferences(Context context, Item item);

    /**
     * Clears the resolver cache if any is used
     */
    void clearResolversCache();
}
