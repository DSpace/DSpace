/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deduplication.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.persistence.Query;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.deduplication.Deduplication;
import org.dspace.deduplication.dao.DeduplicationDAO;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.jdbc.ReturningWork;

public class DeduplicationDAOImpl extends AbstractHibernateDAO<Deduplication> implements DeduplicationDAO {
    private static final String DEDUPLICATION_SEQUENCE = "deduplication_id_seq";

    @Override
    public Deduplication create(Context context, Deduplication d) throws SQLException {
        Deduplication dedup = super.create(context, d);
        dedup.setDeduplicationId(getNextDeduplicationId(context));

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

    /**
     * Return next available value of Deduplication id (based on DB sequence).
     * 
     * @param context Current DSpace Context
     * @return next available id (as a Long)
     * @throws SQLException if database error or sequence doesn't exist
     */
    private Integer getNextDeduplicationId(Context context) throws SQLException {
        // Create a new Hibernate ReturningWork, which will return the
        // result of the next value in the Handle Sequence.
        ReturningWork<Integer> nextValReturningWork = new ReturningWork<Integer>() {
            @Override
            public Integer execute(Connection connection) throws SQLException {
                Integer nextVal = 0;

                // Determine what dialect we are using for this DB
                DialectResolver dialectResolver = new StandardDialectResolver();
                Dialect dialect = dialectResolver
                        .resolveDialect(new DatabaseMetaDataDialectResolutionInfoAdapter(connection.getMetaData()));

                // Find the next value in our sequence (based on DB dialect)
                try (PreparedStatement preparedStatement = connection
                        .prepareStatement(dialect.getSequenceNextValString(DEDUPLICATION_SEQUENCE))) {
                    // Execute query and return results
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            // Return result of query (from first column)
                            nextVal = resultSet.getInt(1);
                        }
                    }
                }
                return nextVal;
            }
        };

        // Run our work, returning the next value in the sequence (see
        // 'nextValReturningWork' above)
        return getHibernateSession(context).doReturningWork(nextValReturningWork);
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
