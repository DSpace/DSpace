/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deduplication.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.persistence.Query;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.deduplication.Deduplication;
import org.dspace.deduplication.dao.DeduplicationDAO;

public class DeduplicationDAOImpl extends AbstractHibernateDAO<Deduplication> implements DeduplicationDAO {
    private static final String DEDUPLICATION_SEQUENCE = "deduplication_id_seq";

    @Override
    public Deduplication create(Context context, Deduplication d) throws SQLException {
        Deduplication dedup = super.create(context, d);

        return dedup;
    }

    @Override
    public List<Deduplication> findByFirstAndSecond(Context context, UUID firstId, UUID secondId) throws SQLException {
        Query query = queryByFirstAndSecond(context, firstId, secondId);
        return list(query);
    }

    @Override
    public Deduplication uniqueByFirstAndSecond(Context context, UUID firstId, UUID secondId) throws SQLException {
        Query query = queryByFirstAndSecond(context, firstId, secondId);
        return singleResult(query);
    }

    public List<Deduplication> findAll(Context context, int pageSize, int offset) throws SQLException {
        Query query = createQuery(context, "SELECT d FROM Deduplication d");
        if (pageSize > 0) {
            query.setMaxResults(pageSize);
        }
        if (offset > 0) {
            query.setFirstResult(offset);
        }
        return list(query);
    }

    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Deduplication"));
    }

    private Query queryByFirstAndSecond(Context context, UUID firstId, UUID secondId) throws SQLException {
        Query query = createQuery(context, "SELECT d FROM Deduplication d"
                + " WHERE d.firstItemId = :firstItemId and d.secondItemId = :secondItemId");

        query.setParameter("firstItemId", firstId);
        query.setParameter("secondItemId", secondId);

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return query;
    }
}
