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
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Session;

//import org.hibernate.query.Query;

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

    protected Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }

    @Override
    public void delete(Context context, T t) throws SQLException {
        getHibernateSession(context).delete(t);
    }

    @Override
    public List<T> findAll(Context context, Class<T> clazz) throws SQLException {
        CriteriaQuery criteriaQuery = getCriteriaQuery(getCriteriaBuilder(context), clazz);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.select(root);
        return executeCriteriaQuery(context, criteriaQuery, false, clazz, -1, -1);
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

//    public Criteria createCriteria(Context context, Class<T> persistentClass) throws SQLException {
//        return getHibernateSession(context).createCriteria(persistentClass);
//    }
//
//    public Criteria createCriteria(Context context, Class<T> persistentClass, String alias) throws SQLException {
//        return getHibernateSession(context).createCriteria(persistentClass, alias);
//    }

    public Query createQuery(Context context, String query) throws SQLException {
        return getHibernateSession(context).createQuery(query);
    }

    public List<T> list(Context context, CriteriaQuery criteriaQuery, boolean cacheable, Class<T> clazz, int maxResults, int offset) throws SQLException
    {
        criteriaQuery.distinct(true);
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) executeCriteriaQuery(context, criteriaQuery, cacheable, clazz, maxResults, offset);
        return result;
    }
    public List<T> list(Context context, CriteriaQuery criteriaQuery, boolean cacheable, Class<T> clazz, int maxResults, int offset, boolean distinct) throws SQLException
    {
        criteriaQuery.distinct(distinct);
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) executeCriteriaQuery(context,criteriaQuery,cacheable, clazz, maxResults, offset);
        return result;
    }
    public List<T> list(Query query)
    {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) query.getResultList();
        return result;
    }

    /**
     * Retrieve a unique result from the query.  If multiple results CAN be
     * retrieved an exception will be thrown,
     * so only use when the criteria state uniqueness in the database.
     * @param criteriaQuery JPA criteria
     * @return a DAO specified by the criteria
     */
    public T uniqueResult(Context context, CriteriaQuery criteriaQuery, boolean cacheable, Class<T> clazz,
                          int maxResults, int offset) throws SQLException {
        List<T> list = list(context, criteriaQuery, cacheable, clazz, maxResults, offset);
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
     * @param criteriaQuery JPA criteria
     * @return a DAO specified by the criteria
     */
    public T singleResult(Context context, CriteriaQuery criteriaQuery) throws SQLException {
//        List<T> list = list(context, criteriaQuery, cacheable, clazz, maxResults, offset);
//        if(CollectionUtils.isNotEmpty(list))
//        {
//            return list.get(0);
//        }else{
//            return null;
//        }
//
        Query query = this.getHibernateSession(context).createQuery(criteriaQuery);
        return singleResult(query);

    }

    public T singleResult(final Query query) {
        query.setMaxResults(1);
        List<T> list = list(query);
        if (CollectionUtils.isNotEmpty(list)) {
            return list.get(0);
        } else {
            return null;
        }

    }

    public T uniqueResult(Query query) {
        @SuppressWarnings("unchecked")
        T result = (T) query.getSingleResult();
        return result;
    }

    public Iterator<T> iterate(Query query) {
        @SuppressWarnings("unchecked")
        Iterator<T> result = (Iterator<T>) query.getResultList().iterator();
        return result;
    }

    public int count(Context context, CriteriaQuery criteriaQuery, CriteriaBuilder criteriaBuilder, Root<T> root)
        throws SQLException {
        return Math.toIntExact(countLong(context, criteriaQuery, criteriaBuilder, root));
    }

    public int count(Query query) {
        return ((Long) query.getSingleResult()).intValue();
    }

    public long countLong(Context context, CriteriaQuery criteriaQuery, CriteriaBuilder criteriaBuilder, Root<T> root)
        throws SQLException {
        Expression<Long> countExpression = criteriaBuilder.countDistinct(root);
        criteriaQuery.select(countExpression);
        return (Long) this.getHibernateSession(context).createQuery(criteriaQuery).getSingleResult();
    }

    public CriteriaQuery<T> getCriteriaQuery(CriteriaBuilder criteriaBuilder, Class<T> c) {
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(c);
        return criteriaQuery;
    }

    public CriteriaBuilder getCriteriaBuilder(Context context) throws SQLException {
        return this.getHibernateSession(context).getCriteriaBuilder();
    }

    public List<T> executeCriteriaQuery(Context context, CriteriaQuery<T> criteriaQuery, boolean cacheable,
                                        Class<T> clazz, int maxResults, int offset) throws SQLException {
        //This has to be here, otherwise a 500 gets thrown
        Root<T> root = criteriaQuery.from(clazz);
        Query query = this.getHibernateSession(context).createQuery(criteriaQuery);

        //TODO Check if this works and is desireable
        query.setHint("org.hibernate.cacheable", cacheable);
//        query.setCacheable(cacheable);
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        if (maxResults != -1) {
            query.setFirstResult(offset);
        }
        return query.getResultList();

    }

    public List<T> findByX(Context context, Class clazz, Map<String, Object> equals, boolean cacheable, int maxResults,
                           int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<T> criteria = getCriteriaQuery(criteriaBuilder, clazz);
        Root root = criteria.from(clazz);
        criteria.select(root);

        //TODO Maybe one big where, test this;;;; seems to not be necessary
        for (Map.Entry<String, Object> entry : equals.entrySet()) {
            criteria.where(criteriaBuilder.equal(root.get(entry.getKey()), entry.getValue()));
        }
        return executeCriteriaQuery(context, criteria, cacheable, clazz, maxResults, offset);
    }

    //TODO find alternative for uniqueResult
}
