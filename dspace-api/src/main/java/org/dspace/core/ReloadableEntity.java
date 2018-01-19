/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.Serializable;

/**
 * Implemented by all entities that can be reloaded by the {@link Context}.
 * @param <T> type of this entity's primary key.
 * @see org.dspace.core.Context#reloadEntity(ReloadableEntity)
 */
public interface ReloadableEntity<T extends Serializable> {
    /**
     * The unique identifier of this entity instance.
     * @return the value of the primary key for this instance.
     */
    T getID();
}
