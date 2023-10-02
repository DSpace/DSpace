/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.dao.OrcidQueueDAO;
import org.hibernate.Session;

/**
 * Implementation of {@link OrcidQueueDAO}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings("unchecked")
public class OrcidQueueDAOImpl extends AbstractHibernateDAO<OrcidQueue> implements OrcidQueueDAO {

    @Override
    public List<OrcidQueue> findByProfileItemId(Session session, UUID profileItemId, Integer limit, Integer offset)
        throws SQLException {
        Query query = createQuery(session, "FROM OrcidQueue WHERE profileItem.id= :profileItemId");
        query.setParameter("profileItemId", profileItemId);
        if (limit != null && limit > 0) {
            query.setMaxResults(limit);
        }
        query.setFirstResult(offset);
        return query.getResultList();
    }

    @Override
    public List<OrcidQueue> findByProfileItemAndEntity(Session session, Item profileItem, Item entity)
        throws SQLException {
        Query query = createQuery(session, "FROM OrcidQueue WHERE profileItem = :profileItem AND entity = :entity");
        query.setParameter("profileItem", profileItem);
        query.setParameter("entity", entity);
        return query.getResultList();
    }

    @Override
    public long countByProfileItemId(Session session, UUID profileItemId) throws SQLException {
        Query query = createQuery(session,
            "SELECT COUNT(queue) FROM OrcidQueue queue WHERE profileItem.id= :profileItemId");
        query.setParameter("profileItemId", profileItemId);
        return (long) query.getSingleResult();
    }

    @Override
    public List<OrcidQueue> findByProfileItemOrEntity(Session session, Item item) throws SQLException {
        Query query = createQuery(session, "FROM OrcidQueue WHERE profileItem.id= :itemId OR entity.id = :itemId");
        query.setParameter("itemId", item.getID());
        return query.getResultList();
    }

    @Override
    public List<OrcidQueue> findByEntityAndRecordType(Session session, Item entity, String type) throws SQLException {
        Query query = createQuery(session, "FROM OrcidQueue WHERE entity = :entity AND recordType = :type");
        query.setParameter("entity", entity);
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Override
    public List<OrcidQueue> findByProfileItemAndRecordType(Session session, Item profileItem, String type)
        throws SQLException {
        Query query = createQuery(session, "FROM OrcidQueue WHERE profileItem = :profileItem AND recordType = :type");
        query.setParameter("profileItem", profileItem);
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Override
    public List<OrcidQueue> findByAttemptsLessThan(Session session, int attempts) throws SQLException {
        Query query = createQuery(session, "FROM OrcidQueue WHERE attempts IS NULL OR attempts < :attempts");
        query.setParameter("attempts", attempts);
        return query.getResultList();
    }

}
