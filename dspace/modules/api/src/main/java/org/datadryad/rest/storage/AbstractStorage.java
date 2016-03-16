/*
 */
package org.datadryad.rest.storage;

import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AbstractStorage<T> implements StorageInterface<T> {
    protected abstract void createObject(StoragePath path, T object) throws StorageException;
    protected abstract void updateObject(StoragePath path, T object) throws StorageException;
    protected abstract T readObject(StoragePath path) throws StorageException;
    protected abstract void deleteObject(StoragePath path) throws StorageException;
    protected abstract void addResults(StoragePath path, List<T> objects, String searchParam, Integer limit) throws StorageException;

    final void checkPath(StoragePath path, List<String> expectedKeyPath) throws StorageException {
        if(path == null) {
            throw new StorageException("Null path");
        } else if(path.size() != expectedKeyPath.size()) {
            throw new StorageException("Path length should be " + expectedKeyPath.size());
        } else if(!path.getKeyPath().equals(expectedKeyPath)) {
            throw new StorageException("Invalid path " + path.toString());
        } else if(!path.validElements()) {
            throw new StorageException("Invalid path elements" + path.toString());
        }
    }

    @Override
    public void create(StoragePath path, T object) throws StorageException {
        createObject(path, object);
    }

    // If this returns null, not found
    @Override
    public T findByPath(StoragePath path) throws StorageException {
        // can search by organization code id
        checkObjectPath(path);
        // find parameters are valid, must be organization code
        return readObject(path);
    }

    @Override
    public void update(StoragePath path, T object) throws StorageException {
        updateObject(path, object);
    }

    @Override
    public List<T> getAll(StoragePath path) throws StorageException {
        List<T> objects = new ArrayList<T>();
        addResults(path, objects, null, null);
        return objects;
    }

    @Override
    public List<T> getResults(StoragePath path, String searchParam, Integer limit) throws StorageException {
        List<T> objects = new ArrayList<T>();
        addResults(path, objects, searchParam, limit);
        return objects;
    }

    @Override
    public void deleteByPath(StoragePath path) throws StorageException {
        this.checkObjectPath(path);
        this.deleteObject(path);
    }
}
