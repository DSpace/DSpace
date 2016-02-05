/*
 */
package org.datadryad.rest.handler;

import org.datadryad.rest.storage.StoragePath;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public interface HandlerInterface<T> {

    public void handleCreate(StoragePath path, T object) throws HandlerException;
    public void handleUpdate(StoragePath path, T object) throws HandlerException;
    public void handleDelete(StoragePath path, T object) throws HandlerException;

}
