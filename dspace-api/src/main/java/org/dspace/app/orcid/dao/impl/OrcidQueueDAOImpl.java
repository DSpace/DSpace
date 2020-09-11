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
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Implementation of {@link OrcidQueueDAO}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueDAOImpl extends AbstractHibernateDAO<OrcidQueue> implements OrcidQueueDAO {

    @Override
    @SuppressWarnings("unchecked")
    public List<OrcidQueue> findByOwnerId(Context context, UUID ownerId, Integer limit, Integer offset)
        throws SQLException {
        Query query = createQuery(context, "FROM OrcidQueue WHERE owner.id= :ownerId");
        query.setParameter("ownerId", ownerId);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OrcidQueue> findByOwnerAndEntityId(Context context, UUID ownerId, UUID entityId) throws SQLException {
        Query query = createQuery(context, "FROM OrcidQueue WHERE owner.id= :ownerId and entity.id = :entityId");
        query.setParameter("ownerId", ownerId);
        query.setParameter("entityId", entityId);
        return query.getResultList();
    }

    @Override
    public long countByOwnerId(Context context, UUID ownerId) throws SQLException {
        Query query = createQuery(context, "SELECT COUNT(queue) FROM OrcidQueue queue WHERE owner.id= :ownerId");
        query.setParameter("ownerId", ownerId);
        return (long) query.getSingleResult();
    }

}
