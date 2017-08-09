/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Generic Database Access Object interface class that should be implemented by all DAOs.
 * It offers up a lot of general methods so these don't need to be declared again in each DAO.
 * The default Hibernate implementation offers up a class that implements all these methods.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> type which is accessed by this DAO, for example Item.
 */
public interface GenericDAO<T>
{
    /**
     * Create a new instance of this type in the database.
     * @param context current DSpace context.
     * @param t type to be created.
     * @return entity tracking the created instance.
     * @throws SQLException
     */
    public T create(Context context, T t) throws SQLException;

    /**
     * Persist this instance in the database.
     * @param context current DSpace context.
     * @param t type created here.
     * @throws SQLException passed through.
     */
    public void save(Context context, T t) throws SQLException;

    /**
     * Remove an instance from the database.
     * @param context current DSpace context.
     * @param t type of the instance to be removed.
     * @throws SQLException passed through.
     */
    public void delete(Context context, T t) throws SQLException;

    /**
     * Fetch all persisted instances of a given object type.
     *
     * @param context
     * @param clazz the desired type.
     * @return list of DAOs of the same type as clazz
     * @throws SQLException if database error
     */
    public List<T> findAll(Context context, Class<T> clazz) throws SQLException;

    /**
     * Execute a JPQL query returning a unique result.
     *
     * @param context
     * @param query JPQL query string
     * @return a DAO specified by the query string
     * @throws SQLException if database error
     */
    public T findUnique(Context context, String query) throws SQLException;

    /**
     * Fetch the entity identified by its legacy database identifier.
     * @param context current DSpace context.
     * @param clazz class of entity to be found.
     * @param id legacy database record ID.
     * @return the found entity.
     * @throws SQLException passed through.
     */
    public T findByID(Context context, Class clazz, int id) throws SQLException;

    /**
     * Fetch the entity identified by its UUID primary key.
     * @param context current DSpace context.
     * @param clazz class of entity to be found.
     * @param id primary key of the database record.
     * @return the found entity.
     * @throws SQLException
     */
    public T findByID(Context context, Class clazz, UUID id) throws SQLException;

    /**
     * Execute a JPQL query and return a collection of results.
     *
     * @param context
     * @param query JPQL query string
     * @return list of DAOs specified by the query string
     * @throws SQLException if database error
     */
    public List<T> findMany(Context context, String query) throws SQLException;
}
