/*
 */
package org.datadryad.rest.storage;

import org.datadryad.rest.models.ResultSet;

import java.lang.Integer;
import java.util.ArrayList;
import java.util.Date;
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
    protected abstract ResultSet addResults(StoragePath path, List<T> objects, String searchParam, Integer limit, Integer cursor) throws StorageException;

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
        // can search by journal code
        checkObjectPath(path);
        // find parameters are valid, must be journal code
        return readObject(path);
    }

    @Override
    public void update(StoragePath path, T object) throws StorageException {
        updateObject(path, object);
    }

    @Override
    public List<T> getAll(StoragePath path) throws StorageException {
        List<T> objects = new ArrayList<T>();
        addResults(path, objects, null, null, 0);
        return objects;
    }

    @Override
    public ResultSet getResults(StoragePath path, List<T> objects, String searchParam, Integer limit, Integer cursor) throws StorageException {
        if (objects == null) {
            objects = new ArrayList<T>();
        }
        return addResults(path, objects, searchParam, limit, cursor);
    }

    public ResultSet addResultsInDateRange(StoragePath path, List<T> objects, Date dateFrom, Date dateTo, Integer limit, Integer cursor) throws StorageException {
        return getResults(path, objects, null, limit, cursor);
    }

    @Override
    public void deleteByPath(StoragePath path) throws StorageException {
        this.checkObjectPath(path);
        this.deleteObject(path);
    }
}
