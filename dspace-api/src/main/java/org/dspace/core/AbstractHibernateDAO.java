/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Hibernate implementation for generic DAO interface.  Also includes additional
 * Hibernate calls that are commonly used.
 * Each DAO should extend this class to prevent code duplication.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> class type
 */
public abstract class AbstractHibernateDAO<T> implements GenericDAO<T> {

    protected AbstractHibernateDAO()
    {

    }

    @Override
    public T create(Context context, T t) throws SQLException {
        getHibernateSession(context).persist(t);
        return t;
    }

    @Override
    public void save(Context context, T t) throws SQLException {
        //Isn't required, is just here for other DB implementation. Hibernate auto keeps track of changes.
    }

    /**
     * The Session used to manipulate entities of this type.
     * @param context current DSpace context.
     * @return the current Session.
     * @throws SQLException
     */
    protected Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }

    @Override
    public void delete(Context context, T t) throws SQLException {
        getHibernateSession(context).delete(t);
    }

    @Override
    public List<T> findAll(Context context, Class<T> clazz) throws SQLException {
        return list(createCriteria(context, clazz));
    }

    @Override
    public T findUnique(Context context, String query) throws SQLException {
        @SuppressWarnings("unchecked")
        T result = (T) createQuery(context, query).uniqueResult();
        return result;
    }

    @Override
    public T findByID(Context context, Class clazz, UUID id) throws SQLException {
        if(id == null)
        {
            return null;
        }
        @SuppressWarnings("unchecked")
        T result = (T) getHibernateSession(context).get(clazz, id);
        return result;
    }

    @Override
    public T findByID(Context context, Class clazz, int id) throws SQLException {
        @SuppressWarnings("unchecked")
        T result = (T) getHibernateSession(context).get(clazz, id);
        return result;
    }

    @Override
    public List<T> findMany(Context context, String query) throws SQLException {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) createQuery(context, query).list();
        return result;
    }

    /**
     * Execute a JPA Criteria query and return a collection of results.
     *
     * @param context
     * @param query JPQL query string
     * @return list of DAOs specified by the query string
     * @throws SQLException if database error
     */
    public List<T> findMany(Context context, Query query) throws SQLException {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) query.list();
        return result;
    }

    /**
     * Create criteria matching an entity type or a supertype thereof.
     * Use when building a criteria query.
     * @param context current DSpace context.
     * @param persistentClass specifies the type to be matched by the criteria.
     * @return criteria concerning the type to be found.
     * @throws SQLException passed through.
     */
    public Criteria createCriteria(Context context, Class<T> persistentClass) throws SQLException {
        return getHibernateSession(context).createCriteria(persistentClass);
    }

    /**
     * Create criteria matching an entity type or a supertype thereof.
     * Use when building a criteria query.
     * @param context current DSpace context.
     * @param persistentClass specifies the type to be matched by the criteria.
     * @param alias alias for the type.
     * @return criteria concerning the type to be found.
     * @throws SQLException passed through.
     */
    public Criteria createCriteria(Context context, Class<T> persistentClass, String alias) throws SQLException {
        return getHibernateSession(context).createCriteria(persistentClass, alias);
    }

    /**
     * Create a parsed query from a query expression.
     * @param context current DSpace context.
     * @param query textual form of the query.
     * @return parsed form of the query.
     * @throws SQLException
     */
    public Query createQuery(Context context, String query) throws SQLException {
        return getHibernateSession(context).createQuery(query);
    }

    /**
     * Get the entities matched by the given Criteria.
     * Use this if you need all results together.
     * @param criteria description of desired entities.
     * @return the entities matched.
     */
    public List<T> list(Criteria criteria)
    {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) criteria.list();
        return result;
    }

    /**
     * Get the entities matching a given parsed query.
     * Use this if you need all results together.
     * @param query the query to be executed.
     * @return entities matching the query.
     */
    public List<T> list(Query query)
    {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) query.list();
        return result;
    }

    /**
     * Retrieve a unique result selected by criteria.  If multiple results CAN be
     * retrieved an exception will be thrown,
     * so only use when the criteria state uniqueness in the database.
     * @param criteria description of the desired entity.
     * @return a DAO specified by the criteria
     */
    public T uniqueResult(Criteria criteria)
    {
        @SuppressWarnings("unchecked")
        T result = (T) criteria.uniqueResult();
        return result;
    }

    /**
     * Retrieve a unique result selected by a query.  If multiple results CAN be
     * retrieved then an exception will be thrown, so only use when the query
     * states uniqueness in the database.
     * @param query description of the desired entity.
     * @return the found entity.
     */
    public T uniqueResult(Query query)
    {
        @SuppressWarnings("unchecked")
        T result = (T) query.uniqueResult();
        return result;
    }

    /**
     * Retrieve a single result selected by criteria.  Best used if you expect a
     * single result, but this isn't enforced on the database.
     * @param criteria description of the desired entities.
     * @return a DAO specified by the criteria
     */
    public T singleResult(Criteria criteria)
    {
        criteria.setMaxResults(1);
        List<T> list = list(criteria);
        if(CollectionUtils.isNotEmpty(list))
        {
            return list.get(0);
        }else{
            return null;
        }
    }

    /**
     * Retrieve a single result matching a query.  Best used if you expect a
     * single result, but this isn't enforced on the database.
     * @param query description of desired entities.
     * @return matched entities.
     */
    public T singleResult(final Query query) {
        query.setMaxResults(1);
        List<T> list = list(query);
        if(CollectionUtils.isNotEmpty(list))
        {
            return list.get(0);
        }else{
            return null;
        }
    }

    /**
     * Get an iterator over a stream of query results.
     * Use this when consuming results one at a time.
     * @param query description of desired entities.
     * @return iterator over the results of the query.
     */
    public Iterator<T> iterate(Query query)
    {
        @SuppressWarnings("unchecked")
        Iterator<T> result = (Iterator<T>) query.iterate();
        return result;
    }

    /**
     * How many rows match these criteria?
     * The same value as {@link countLong(Criteria)}, coerced to {@code int}.
     * @param criteria description of the rows.
     * @return count of matching rows.
     */
    public int count(Criteria criteria)
    {
        return ((Long) criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();
    }

    /**
     * How many rows match this query?
     * @param query description of the rows.
     * @return count of matching rows.
     */
    public int count(Query query)
    {
        return ((Long) query.uniqueResult()).intValue();
    }

    /**
     * How many rows match these criteria?
     * @param criteria description of the rows.
     * @return count of matching rows.
     */
    public long countLong(Criteria criteria)
    {
        return (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
    }
}
