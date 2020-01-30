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
    public List<Deduplication> findByFirstAndSecond(Context context, String firstId, String secondId)
            throws SQLException {
        Query query = queryByFirstAndSecond(context, firstId, secondId);
        return list(query);
    }

    @Override
    public Deduplication uniqueByFirstAndSecond(Context context, String firstId, String secondId) throws SQLException {
        Query query = queryByFirstAndSecond(context, firstId, secondId);
        return singleResult(query);
    }

    public List<Deduplication> findAll(Context context) throws SQLException {
        Query query = createQuery(context, "SELECT d FROM Deduplication d");
        return list(query);
    }

    /**
     * Return next available value of Deduplication id (based on DB sequence).
     * 
     * @param context Current DSpace Context
     * @return next available id (as a Long)
     * @throws SQLException if database error or sequence doesn't exist
     */
    @Override
    public Integer getNextDeduplicationId(Context context) throws SQLException {
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

    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Deduplication"));
    }

    private Query queryByFirstAndSecond(Context context, String firstId, String secondId) throws SQLException {
        Query query = createQuery(context, "SELECT d FROM Deduplication d"
                + " WHERE d.firstItemId = :firstItemId and d.secondItemId = :secondItemId");

        query.setParameter("firstItemId", firstId);
        query.setParameter("secondItemId", secondId);

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return query;
    }
}
