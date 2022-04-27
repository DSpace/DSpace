/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.dao.OrcidQueueDAO;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Implementation of {@link OrcidQueueDAO}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings("unchecked")
public class OrcidQueueDAOImpl extends AbstractHibernateDAO<OrcidQueue> implements OrcidQueueDAO {

    @Override
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId, Integer limit, Integer offset)
        throws SQLException {
        Query query = createQuery(context, "FROM OrcidQueue WHERE owner.id= :ownerId");
        query.setParameter("ownerId", ownerId);
        if (limit != null && limit.intValue() > 0) {
            query.setMaxResults(limit);
        }
        query.setFirstResult(offset);
        return query.getResultList();
    }

    @Override
    public List<OrcidQueue> findByOwnerAndEntity(Context context, Item owner, Item entity) throws SQLException {
        Query query = createQuery(context, "FROM OrcidQueue WHERE owner = :owner AND entity = :entity");
        query.setParameter("owner", owner);
        query.setParameter("entity", entity);
        return query.getResultList();
    }

    @Override
    public long countByOwnerId(Context context, UUID ownerId) throws SQLException {
        Query query = createQuery(context, "SELECT COUNT(queue) FROM OrcidQueue queue WHERE owner.id= :ownerId");
        query.setParameter("ownerId", ownerId);
        return (long) query.getSingleResult();
    }

    @Override
    public List<OrcidQueue> findByOwnerOrEntity(Context context, Item item) throws SQLException {
        Query query = createQuery(context, "FROM OrcidQueue WHERE owner.id= :itemId OR entity.id = :itemId");
        query.setParameter("itemId", item.getID());
        return query.getResultList();
    }

    @Override
    public List<OrcidQueue> findByEntityAndRecordType(Context context, Item entity, String type) throws SQLException {
        Query query = createQuery(context, "FROM OrcidQueue WHERE entity = :entity AND recordType = :type");
        query.setParameter("entity", entity);
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Override
    public List<OrcidQueue> findByOwnerAndRecordType(Context context, Item owner, String type) throws SQLException {
        Query query = createQuery(context, "FROM OrcidQueue WHERE owner = :owner AND recordType = :type");
        query.setParameter("owner", owner);
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Override
    public List<OrcidQueue> findByAttemptsLessThan(Context context, int attempts) throws SQLException {
        Query query = createQuery(context, "FROM OrcidQueue WHERE attempts IS NULL OR attempts < :attempts");
        query.setParameter("attempts", attempts);
        return query.getResultList();
    }

}
