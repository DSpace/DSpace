/*
 */
package org.datadryad.rest.storage;

import org.datadryad.rest.models.ResultSet;
import org.dspace.core.Context;

import java.lang.Integer;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

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
        ResultSet resultSet = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        if (dateTo == null) {
            dateTo = new Date();
        }
        if (dateFrom == null) {
            dateFrom = new Date(0);
        }

        if (dateFrom.after(dateTo)) {
            throw new StorageException("date_from " + sdf.format(dateFrom) + " is later than date_to " + sdf.format(dateTo));
        }
        return resultSet;
    }

    @Override
    public void deleteByPath(StoragePath path) throws StorageException {
        this.checkObjectPath(path);
        this.deleteObject(path);
    }

    protected static Context getContext() {
        Context context = null;
        try {
            context = new Context();
        } catch (SQLException ex) {
        }
        return context;
    }

    protected static void completeContext(Context context) throws SQLException {
        try {
            context.complete();
        } catch (SQLException ex) {
            // Abort the context to force a new connection
            abortContext(context);
            throw ex;
        }
    }

    protected static void abortContext(Context context) {
        if (context != null) {
            context.abort();
        }
    }
}
