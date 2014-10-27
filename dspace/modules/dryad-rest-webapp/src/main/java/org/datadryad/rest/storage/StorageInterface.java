/*
 */
package org.datadryad.rest.storage;

import java.util.List;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public interface StorageInterface<T> {
    public void create(StoragePath path, T t) throws StorageException;
    public List<T> getAll(StoragePath path) throws StorageException;
    public T findByPath(StoragePath path) throws StorageException;
    public void update(StoragePath path, T object) throws StorageException;
    public void deleteByPath(StoragePath path) throws StorageException;
    public Boolean objectExists(StoragePath path, T object) throws StorageException;
    public void checkCollectionPath(StoragePath path) throws StorageException;
    public void checkObjectPath(StoragePath path) throws StorageException;
    
}
