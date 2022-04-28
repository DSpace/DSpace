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
 * Since IndexableObjects can be made multiple times for the same underlying
 * object, we needed more finely-tuned {@link equals} and {@link hashCode} methods.
 * We're simply checking that the underlying objects are equal and returning the
 * hash-code for the underlying object. This way, we'll always get a proper
 * result when calling {@link equals} or {@link hashCode} on an IndexableObject
 * because it'll depend on the underlying object.
 *
 * @param <T>   Refers to the underlying entity that is linked to this object
 * @param <PK>  The type of ID that this entity uses
 */
public abstract class AbstractIndexableObject<T extends ReloadableEntity<PK>, PK extends Serializable>
    implements IndexableObject<T,PK> {

    @Override
    public boolean equals(Object obj) {
        //Two IndexableObjects of the same DSpaceObject are considered equal
        if (!(obj instanceof IndexableObject)) {
            return false;
        }
        IndexableObject other = (IndexableObject) obj;
        return other.getIndexedObject().equals(getIndexedObject());
    }

    @Override
    public int hashCode() {
        //Two IndexableObjects of the same DSpaceObject are considered equal
        return getIndexedObject().hashCode();
    }

}
