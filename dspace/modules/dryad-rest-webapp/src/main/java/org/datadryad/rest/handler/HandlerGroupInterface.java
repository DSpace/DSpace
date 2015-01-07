/*
 */
package org.datadryad.rest.handler;

import org.datadryad.rest.storage.StoragePath;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public interface HandlerGroupInterface<T> {
    public void addHandler(HandlerInterface<T> handler);
    public void removeHandler(HandlerInterface<T> handler);

    public void handleObjectCreated(StoragePath path, T object);
    public void handleObjectUpdated(StoragePath path, T object);
    public void handleObjectDeleted(StoragePath path, T object);

}
