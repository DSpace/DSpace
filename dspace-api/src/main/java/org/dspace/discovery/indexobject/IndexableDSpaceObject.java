/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.util.UUID;
import javax.validation.constraints.NotNull;

import org.dspace.content.DSpaceObject;
import org.dspace.discovery.IndexableObject;

/**
 * DSpaceObject implementation for the {@link IndexableObject}.
 * Contains methods used by all {@link DSpaceObject} implementations.
 * All {@code DSpaceObject} types that will be indexed in Discovery should
 * inherit from this class & have their own implementations.
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @param <T> type of this {@link DSpaceObject}.
 */
public abstract class IndexableDSpaceObject<T extends DSpaceObject>
        extends AbstractIndexableObject<T, UUID> {

    private T dso;

    public IndexableDSpaceObject(@NotNull T dso) {
        if (null == dso) {
            throw new NullPointerException("Null DSO constructing "
                    + this.getClass().getSimpleName());
        }
        this.dso = dso;
    }

    @Override
    public T getIndexedObject() {
        return dso;
    }

    @Override
    public void setIndexedObject(@NotNull T dso) {
        if (null == dso) {
            throw new NullPointerException("Null DSO set in "
                    + this.getClass().getSimpleName());
        }
        this.dso = dso;
    }

    @Override
    public UUID getID() {
        return dso.getID();
    }
}
