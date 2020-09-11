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

/**
 * This class exists in order to provide a default implementation for the equals and hashCode methods.
 * Since IndexableObjects can be made multiple times for the same underlying object, we needed a more finetuned
 * equals and hashcode methods. We're simply checking that the underlying objects are equal and generating the hashcode
 * for the underlying object. This way, we'll always get a proper result when calling equals or hashcode on an
 * IndexableObject because it'll depend on the underlying object
 * @param <T>   Refers to the underlying entity that is linked to this object
 * @param <PK>  The type of ID that this entity uses
 */
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
