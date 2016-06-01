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
 * Interface that has to be implemented by all entities that can be reloaded by the Context
 * (see {@link org.dspace.core.Context#reloadEntity(ReloadableEntity)} ])}
 */
public interface ReloadableEntity<T extends Serializable> {

    T getID();

}
