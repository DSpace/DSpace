/*
 */
package org.datadryad.rest.storage;

import java.util.List;
import org.datadryad.rest.models.Manuscript;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public interface StorageInterface<T> {
    public void create(T t) throws StorageException;
    public List<T> getAll() throws StorageException;
//    public T findById(Integer id) throws StorageException;
    public T findByValue(String fields[], String values[]) throws StorageException;
    public void update(T object) throws StorageException;
//    public void deleteById(Integer key) throws StorageException;
    public void deleteByValue(String fields[], String values[]) throws StorageException;

    public Boolean objectExists(T object) throws StorageException;
    public void checkFindParameters(String fields[], String values[]) throws StorageException;

}
