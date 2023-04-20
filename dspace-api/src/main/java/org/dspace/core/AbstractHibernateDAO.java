/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import com.google.common.collect.AbstractIterator;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Session;

/**
 * Hibernate implementation for generic DAO interface.  Also includes additional
 * Hibernate calls that are commonly used.
 * Each DAO should extend this class to prevent code duplication.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> class type
 */
public abstract class AbstractHibernateDAO<T> implements GenericDAO<T> {

    protected AbstractHibernateDAO() {

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
    *
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

        return findAll(context, clazz, -1, -1);
    }

    @Override
    public List<T> findAll(Context context, Class<T> clazz, Integer limit, Integer offset) throws SQLException {
        CriteriaQuery criteriaQuery = getCriteriaQuery(getCriteriaBuilder(context), clazz);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.select(root);
        return executeCriteriaQuery(context, criteriaQuery, false, limit, offset);
    }

    @Override
    public T findUnique(Context context, String query) throws SQLException {
        @SuppressWarnings("unchecked")
        T result = (T) createQuery(context, query).getSingleResult();
        return result;
    }

    @Override
    public T findByID(Context context, Class clazz, UUID id) throws SQLException {
        if (id == null) {
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
        List<T> result = (List<T>) createQuery(context, query).getResultList();
        return result;
    }

    /**
     * Execute a JPA Criteria query and return a collection of results.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param query
     *     JPQL query string
     * @return list of DAOs specified by the query string
     * @throws SQLException if database error
     */
    public List<T> findMany(Context context, Query query) throws SQLException {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) query.getResultList();
        return result;
    }

    /**
     * Create a parsed query from a query expression.
     *
     * @param context current DSpace context.
     * @param query   textual form of the query.
     * @return parsed form of the query.
     * @throws SQLException
     */
    public Query createQuery(Context context, String query) throws SQLException {
        return getHibernateSession(context).createQuery(query);
    }

    /**
     * This method will return a list with unique results, no duplicates, made by the given CriteriaQuery and parameters
     *
     * @param context
     *         The standard DSpace context object
     * @param criteriaQuery
     *         The CriteriaQuery for which this list will be retrieved
     * @param cacheable
     *         Whether or not this query should be cacheable
     * @param clazz
     *         The class for which this CriteriaQuery will be executed on
     * @param maxResults
     *         The maximum amount of results that will be returned for this CriteriaQuery
     * @param offset
     *         The offset to be used for the CriteriaQuery
     * @return A list of distinct results as depicted by the CriteriaQuery and parameters
     * @throws SQLException
     */
    public List<T> list(
        Context context, CriteriaQuery<T> criteriaQuery, boolean cacheable, Class<T> clazz, int maxResults, int offset
    ) throws SQLException {
        criteriaQuery.distinct(true);
        return executeCriteriaQuery(context, criteriaQuery, cacheable, maxResults, offset);
    }

    /**
     * This method will return a list of results for the given CriteriaQuery and parameters
     *
     * @param context
     *         The standard DSpace context object
     * @param criteriaQuery
     *         The CriteriaQuery to be used to find the list of results
     * @param cacheable
     *         A boolean value indicating whether this query should be cached or not
     * @param clazz
     *         The class on which the CriteriaQuery will search
     * @param maxResults
     *         The maximum amount of results to be returned
     * @param offset
     *         The offset to be used for the CriteriaQuery
     * @param distinct
     *         A boolean value indicating whether this list should be distinct or not
     * @return A list of results determined by the CriteriaQuery and parameters
     * @throws SQLException
     */
    public List<T> list(
        Context context, CriteriaQuery<T> criteriaQuery, boolean cacheable, Class<T> clazz, int maxResults, int offset,
        boolean distinct
    ) throws SQLException {
        criteriaQuery.distinct(distinct);
        return executeCriteriaQuery(context, criteriaQuery, cacheable, maxResults, offset);
    }

    /**
     * This method will be used to return a list of results for the given query
     *
     * @param query
     *         The query for which the resulting list will be returned
     * @return The list of results for the given query
     */
    public List<T> list(Query query) {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) query.getResultList();
        return result;
    }

    /**
     * This method will return a list of results for the given Query and parameters
     * 
     * @param query     The query for which the resulting list will be returned
     * @param limit     The maximum amount of results to be returned
     * @param offset    The offset to be used for the Query
     * @return          A list of results determined by the Query and parameters
     */
    public List<T> list(Query query, int limit, int offset) {
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) query.getResultList();
        return result;
    }

    /**
     * Retrieve a unique result from the query.  If multiple results CAN be
     * retrieved an exception will be thrown, so only use when the criteria
     * state uniqueness in the database.
     * @param context current DSpace session.
     * @param criteriaQuery JPA criteria
     * @param cacheable whether or not this query should be cacheable.
     * @param clazz type of object that should match the query.
     * @return the single model object specified by the criteria,
     *          or {@code null} if none match.
     * @throws java.sql.SQLException passed through.
     * @throws IllegalArgumentException if multiple objects match.
     */
    public T uniqueResult(Context context, CriteriaQuery criteriaQuery,
            boolean cacheable, Class<T> clazz) throws SQLException {
        List<T> list = list(context, criteriaQuery, cacheable, clazz, -1, -1);
        if (CollectionUtils.isNotEmpty(list)) {
            if (list.size() == 1) {
                return list.get(0);
            } else {
                throw new IllegalArgumentException("More than one result found");
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieve a single result from the query.  Best used if you expect a
     * single result, but this isn't enforced on the database.
     * @param context current DSpace session
     * @param criteriaQuery JPA criteria
     * @return a DAO specified by the criteria
     * @throws java.sql.SQLException passed through.
     */
    public T singleResult(Context context, CriteriaQuery criteriaQuery) throws SQLException {
        Query query = this.getHibernateSession(context).createQuery(criteriaQuery);
        return singleResult(query);

    }

    /**
     * This method will return the first result from the given query or null if no results were found
     *
     * @param query
     *         The query that is to be executed
     * @return One result from the given query or null if none was found
     */
    public T singleResult(final Query query) {
        query.setMaxResults(1);
        List<T> list = list(query);
        if (CollectionUtils.isNotEmpty(list)) {
            return list.get(0);
        } else {
            return null;
        }

    }

    /**
     * This method will return a singular result for the given query
     *
     * @param query
     *         The query for which a single result will be given
     * @return The single result for this query
     */
    public T uniqueResult(Query query) {
        @SuppressWarnings("unchecked")
        T result = (T) query.getSingleResult();
        return result;
    }

    /**
     * This method will return an Iterator for the given Query
     *
     * @param query
     *         The query for which an Iterator will be made
     * @return The Iterator for the results of this query
     */
    public Iterator<T> iterate(Query query) {
        @SuppressWarnings("unchecked")
        org.hibernate.query.Query hquery = query.unwrap(org.hibernate.query.Query.class);
        Stream<T> stream = hquery.stream();
        Iterator<T> iter = stream.iterator();
        return new AbstractIterator<T> () {
            @Override
            protected T computeNext() {
                return iter.hasNext() ? iter.next() : endOfData();
            }
            @Override
            public void finalize() {
                stream.close();
            }
        };
    }

    /**
     * This method will return the amount of results that would be generated for this CriteriaQuery as an integer
     *
     * @param context
     *         The standard DSpace Context object
     * @param criteriaQuery
     *         The CriteriaQuery for which this result will be retrieved
     * @param criteriaBuilder
     *         The CriteriaBuilder that accompanies the CriteriaQuery
     * @param root
     *         The root that'll determine on which class object we need to calculate the result
     * @return The amount of results that would be found by this CriteriaQuery as an integer value
     * @throws SQLException
     */
    public int count(Context context, CriteriaQuery criteriaQuery, CriteriaBuilder criteriaBuilder, Root<T> root)
        throws SQLException {
        return Math.toIntExact(countLong(context, criteriaQuery, criteriaBuilder, root));
    }

    /**
     * This method will return the count of items for this query as an integer
     * This query needs to already be in a formate that'll return one record that contains the amount
     *
     * @param query
     *         The query for which the amount of results will be returned.
     * @return The amount of results
     */
    public int count(Query query) {
        return ((Long) query.getSingleResult()).intValue();
    }

    /**
     * This method will return the count of items for this query as a long
     *
     * @param context
     *         The standard DSpace Context object
     * @param criteriaQuery
     *         The CriteriaQuery for which the amount of results will be retrieved
     * @param criteriaBuilder
     *         The CriteriaBuilder that goes along with this CriteriaQuery
     * @param root
     *         The root created for a DSpace class on which this query will search
     * @return A long value that depicts the amount of results this query has found
     * @throws SQLException
     */
    public long countLong(Context context, CriteriaQuery criteriaQuery, CriteriaBuilder criteriaBuilder, Root<T> root)
        throws SQLException {
        Expression<Long> countExpression = criteriaBuilder.countDistinct(root);
        criteriaQuery.select(countExpression);
        return (Long) this.getHibernateSession(context).createQuery(criteriaQuery).getSingleResult();
    }

    /**
     * This method should always be used in order to retrieve the CriteriaQuery in order to
     * start creating a query that has to be executed
     *
     * @param criteriaBuilder
     *         The CriteriaBuilder for which this CriteriaQuery will be constructed
     * @param clazz
     *         The class that this CriteriaQuery will be constructed for
     * @return A CriteriaQuery on which a query can be built
     */
    public CriteriaQuery<T> getCriteriaQuery(CriteriaBuilder criteriaBuilder, Class<T> clazz) {
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
        return criteriaQuery;
    }

    /**
     * This method should always be used in order to retrieve a CriteriaBuilder for the given context
     *
     * @param context
     *         The standard DSpace Context class for which a CriteriaBuilder will be made
     * @return A CriteriaBuilder that can be used to create the query
     * @throws SQLException
     */
    public CriteriaBuilder getCriteriaBuilder(Context context) throws SQLException {
        return this.getHibernateSession(context).getCriteriaBuilder();
    }

    /**
     * This method will return a list of objects to be returned that match the given criteriaQuery and parameters.
     * The maxResults and offSet can be circumvented by entering the value -1 for them.
     *
     * @param context
     *         The standard context DSpace object
     * @param criteriaQuery
     *         The CriteriaQuery that will be used for executing the query
     * @param cacheable
     *         Whether or not this query is able to be cached
     * @param maxResults
     *         The maximum amount of results that this query will return
     *         This can be circumvented by passing along -1 as the value
     * @param offset
     *         The offset to be used in this query
     *         This can be circumvented by passing along -1 as the value
     * @return This will return a list of objects that conform to the made query
     * @throws SQLException
     */
    public List<T> executeCriteriaQuery(Context context, CriteriaQuery<T> criteriaQuery, boolean cacheable,
                                        int maxResults, int offset) throws SQLException {
        Query query = this.getHibernateSession(context).createQuery(criteriaQuery);

        query.setHint("org.hibernate.cacheable", cacheable);
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        if (offset != -1) {
            query.setFirstResult(offset);
        }
        return query.getResultList();

    }

    /**
     * This method can be used to construct a query for which there needs to be a bunch of equal properties
     * These properties can be passed along in the equals hashmap
     *
     * @param context
     *         The standard DSpace context object
     * @param clazz
     *         The class on which the criteriaQuery will be built
     * @param equals
     *         A hashmap that can be used to store the String representation of the column
     *         and the value that should match that in the DB
     * @param cacheable
     *         A boolean indicating whether this query should be cacheable or not
     * @param maxResults
     *         The max amount of results to be returned by this query
     * @param offset
     *         The offset to be used in this query
     * @return Will return a list of objects that correspond with the constructed query and parameters
     * @throws SQLException
     */
    public List<T> findByX(Context context, Class clazz, Map<String, Object> equals, boolean cacheable, int maxResults,
                           int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<T> criteria = getCriteriaQuery(criteriaBuilder, clazz);
        Root root = criteria.from(clazz);
        criteria.select(root);

        for (Map.Entry<String, Object> entry : equals.entrySet()) {
            criteria.where(criteriaBuilder.equal(root.get(entry.getKey()), entry.getValue()));
        }
        return executeCriteriaQuery(context, criteria, cacheable, maxResults, offset);
    }

}
