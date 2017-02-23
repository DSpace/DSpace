/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.dspace.content.DSpaceObject;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Generic Database Access Object interface class that should be implemented by all DAOs.
 * It offers up a lot of general methods so these don't need to be declared again in each DAO.
 * The default hibernate implementation offers up a class that implements all these methods.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> class type
 */
public interface GenericDAO<T>
{
    public T create(Context context, T t) throws SQLException;

    public void save(Context context, T t) throws SQLException;

    public void delete(Context context, T t) throws SQLException;

    /**
     * Fetch all persisted instances of a given object type.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param clazz the desired type.
     * @return list of DAOs of the same type as clazz
     * @throws SQLException if database error
     */
    public List<T> findAll(Context context, Class<T> clazz) throws SQLException;

    /**
     * Execute a JPQL query returning a unique result.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param query JPQL query string
     * @return a DAO specified by the query string
     * @throws SQLException if database error
     */
    public T findUnique(Context context, String query) throws SQLException;

    public T findByID(Context context, Class clazz, int id) throws SQLException;

    public T findByID(Context context, Class clazz, UUID id) throws SQLException;

    /**
     * Execute a JPQL query and return a collection of results.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param query JPQL query string
     * @return list of DAOs specified by the query string
     * @throws SQLException if database error
     */
    public List<T> findMany(Context context, String query) throws SQLException;
}
