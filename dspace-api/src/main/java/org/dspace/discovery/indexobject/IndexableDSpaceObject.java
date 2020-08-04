/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.discovery.IndexableObject;

/**
 * DSpaceObject implementation for the IndexableObject, contains methods used by all DSpaceObject methods
 * All DSpaceObjects that will be indexed in discovery should inherit from this class & have their own implementation
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public abstract class IndexableDSpaceObject<T extends DSpaceObject> implements IndexableObject<T, UUID> {

    private T dso;

    public IndexableDSpaceObject(T dso) {
        this.dso = dso;
    }

    @Override
    public T getIndexedObject() {
        return dso;
    }

    @Override
    public void setIndexedObject(T dso) {
        this.dso = dso;
    }

    @Override
    public UUID getID() {
        return dso.getID();
    }
}