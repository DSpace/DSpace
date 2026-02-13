/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.curate.CurationQueueEntry;
import org.dspace.curate.dao.CurationQueueEntryDAO;

/**
 * Hibernate implementation of the Database Access Object for the CurationQueueEntry entity.
 * This class is responsible for all database calls for the CurationQueueEntry object and is
 * autowired by Spring. It should only be accessed from a single service.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class CurationQueueEntryDAOImpl extends AbstractHibernateDAO<CurationQueueEntry>
        implements CurationQueueEntryDAO {

    protected CurationQueueEntryDAOImpl() {
        super();
    }

    @Override
    public List<String> findAllQueueNames(Context context) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
        Root<CurationQueueEntry> root = criteriaQuery.from(CurationQueueEntry.class);

        // Select distinct queue_name values
        criteriaQuery.select(root.get("queueName")).distinct(true);

        TypedQuery<String> query = getHibernateSession(context).createQuery(criteriaQuery);
        return query.getResultList();
    }

    @Override
    public List<CurationQueueEntry> findByQueueName(Context context, String queueName) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<CurationQueueEntry> criteriaQuery = getCriteriaQuery(criteriaBuilder, CurationQueueEntry.class);
        Root<CurationQueueEntry> root = criteriaQuery.from(CurationQueueEntry.class);

        criteriaQuery.where(criteriaBuilder.equal(root.get("queueName"), queueName));
        TypedQuery<CurationQueueEntry> query = getHibernateSession(context).createQuery(criteriaQuery);
        return query.getResultList();
    }

    @Override
    public long countByQueueName(Context context, String queueName) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<CurationQueueEntry> root = criteriaQuery.from(CurationQueueEntry.class);

        criteriaQuery.select(criteriaBuilder.count(root));
        criteriaQuery.where(criteriaBuilder.equal(root.get("queueName"), queueName));

        return getHibernateSession(context).createQuery(criteriaQuery).getSingleResult();
    }

    @Override
    public int deleteByQueueName(Context context, String queueName) throws SQLException {
        // Use a direct query for bulk deletion
        Query query = createQuery(context,
                "DELETE FROM CurationQueueEntry e WHERE e.queueName = :queueName");
        query.setParameter("queueName", queueName);

        // Returns the number of rows deleted
        return query.executeUpdate();
    }

    @Override
    public int deleteByIds(Context context, Set<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        // Use a direct query for bulk deletion by IDs
        Query query = createQuery(context,
                "DELETE FROM CurationQueueEntry e WHERE e.id IN :ids");
        query.setParameter("ids", ids);

        // Returns the number of rows deleted
        return query.executeUpdate();
    }
}
