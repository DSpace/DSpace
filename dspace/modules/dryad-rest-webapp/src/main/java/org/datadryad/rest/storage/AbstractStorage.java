/*
 */
package org.datadryad.rest.storage;

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
    protected abstract void addAll(StoragePath path, List<T> objects) throws StorageException;

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
        if(objectExists(path, object)) {
            throw new StorageException("Unable to create, object already exists");
        } else {
            createObject(path, object);
        }
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
        // find the existing organization
        if(!objectExists(path, object)){
            throw new StorageException("Unable to update, object does not exist");
        } else {
            updateObject(path, object);
        }
    }

    @Override
    public List<T> getAll(StoragePath path) throws StorageException {
        List<T> objects = new ArrayList<T>();
        addAll(path, objects);
        return objects;
    }

    @Override
    public void deleteByPath(StoragePath path) throws StorageException {
        this.checkObjectPath(path);
        this.deleteObject(path);
    }
}
