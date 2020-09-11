/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.io.Serializable;

import org.dspace.core.ReloadableEntity;
import org.dspace.discovery.IndexableObject;

public abstract class AbstractIndexableObject<T extends ReloadableEntity<PK>, PK extends Serializable>
    implements IndexableObject<T,PK> {

    @Override
    public boolean equals(Object obj) {
        //Two IndexableObjects of the same DSpaceObject are considered equal
        if (!(obj instanceof AbstractIndexableObject)) {
            return false;
        }
        IndexableDSpaceObject other = (IndexableDSpaceObject) obj;
        return other.getIndexedObject().equals(getIndexedObject());
    }

    @Override
    public int hashCode() {
        //Two IndexableObjects of the same DSpaceObject are considered equal
        return getIndexedObject().hashCode();
    }

}
