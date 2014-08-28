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
    protected abstract void saveObject(T object) throws StorageException;
    protected abstract T readObject(String objectPath[]) throws StorageException;
    protected abstract void deleteObject(String objectPath[]) throws StorageException;
    protected abstract void addAll(List<T> objects) throws StorageException;

    @Override
    public void create(T object) throws StorageException {
        if(objectExists(object)) {
            throw new StorageException("Unable to create, object already exists");
        } else {
            saveObject(object);
        }
    }

    // If this returns null, not found
    @Override
    public T findByValue(String fields[], String values[]) throws StorageException {
        // can search by organization code id
        checkFindParameters(fields, values);
        // find parameters are valid, must be organization code
        return readObject(values);
    }

    /*
    @Override
    public T findById(Integer id) throws StorageException {
        String value = String.valueOf(id);
        String fields[] = {"id"};
        String values[] = {value};
        checkFindParameters(fields, values);
        return findByValue(fields, values);
    }
    */

    @Override
    public void update(T object) throws StorageException {
        // find the existing organization
        if(!objectExists(object)){
            throw new StorageException("Unable to update, object does not exist");
        } else {
            saveObject(object);
        }
    }

    @Override
    public List<T> getAll() throws StorageException {
        List<T> objects = new ArrayList<T>();
        addAll(objects);
        return objects;
    }

    @Override
    public void deleteByValue(String fields[], String values[]) throws StorageException {
        this.checkFindParameters(fields, values);
        this.deleteObject(values);
    }

    /*
    @Override
    public void deleteById(Integer id) throws StorageException {
        String value = String.valueOf(id);
        String fields[] = {"id"};
        String values[] = {value};
        checkFindParameters(fields, values);
        deleteByValue(fields, values);
    }
    */
}
