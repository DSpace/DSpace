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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.curate.CurationQueueLock;
import org.dspace.curate.dao.CurationQueueLockDAO;

/**
 * Hibernate implementation of the Database Access Object for the CurationQueueLock entity.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class CurationQueueLockDAOImpl extends AbstractHibernateDAO<CurationQueueLock> implements CurationQueueLockDAO {

    protected CurationQueueLockDAOImpl() {
        super();
    }

    @Override
    public CurationQueueLock findByQueueName(Context context, String queueName) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<CurationQueueLock> criteriaQuery = getCriteriaQuery(criteriaBuilder, CurationQueueLock.class);
        Root<CurationQueueLock> root = criteriaQuery.from(CurationQueueLock.class);

        criteriaQuery.where(criteriaBuilder.equal(root.get("queueName"), queueName));

        List<CurationQueueLock> result = list(context, criteriaQuery, false, CurationQueueLock.class, -1, -1);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public boolean isQueueLocked(Context context, String queueName) throws SQLException {
        return findByQueueName(context, queueName) != null;
    }

    @Override
    public boolean validateLock(Context context, String queueName, long ticket) throws SQLException {
        CurationQueueLock lock = findByQueueName(context, queueName);
        return lock != null && lock.getTicket() == ticket;
    }

    @Override
    public boolean releaseLock(Context context, String queueName, long ticket) throws SQLException {
        CurationQueueLock lock = findByQueueName(context, queueName);
        if (lock != null && lock.getTicket() == ticket) {
            delete(context, lock);
            return true;
        }
        return false;
    }
}
