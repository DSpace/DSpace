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

/**
 * DAO implementation for Duplicate Detection
 *
 * @author 4Science
 */
public class DeduplicationDAOImpl extends AbstractHibernateDAO<Deduplication> implements DeduplicationDAO {

    /**
     * Create a new deduplication row in the database
     * @param context current DSpace context.
     * @param d Deduplication row
     * @return  new row
     * @throws SQLException
     */
    @Override
    public Deduplication create(Context context, Deduplication d) throws SQLException {
        return super.create(context, d);
    }

    /**
     * Find rows in the database where the item IDs match the given first and second item IDs
     * @param context   DSpace context
     * @param firstId   first item ID
     * @param secondId  second item ID
     * @return          List of deduplication rows
     * @throws SQLException
     */
    @Override
    public List<Deduplication> findByFirstAndSecond(Context context, UUID firstId, UUID secondId) throws SQLException {
        Query query = queryByFirstAndSecond(context, firstId, secondId);
        return list(query);
    }

    /**
     * Find a single row in the database where the item IDs match the given first and second item IDs
     * @param context   DSpace context
     * @param firstId   first item ID
     * @param secondId  second item ID
     * @return          List of deduplication rows
     */
    @Override
    public Deduplication uniqueByFirstAndSecond(Context context, UUID firstId, UUID secondId) throws SQLException {
        Query query = queryByFirstAndSecond(context, firstId, secondId);
        return singleResult(query);
    }

    /**
     * Find all deduplication rows
     * @param context       DSpace context
     * @param pageSize      page size for pagination
     * @param offset        offset for pagination
     * @return              List of deduplication rows
     * @throws SQLException
     */
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

    /**
     * Get number of deduplication rows from database
     * @param context       DSpace context
     * @return              number of rows
     * @throws SQLException
     */
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Deduplication"));
    }

    /**
     * Get all rows in teh database where the first item ID matches the first UUID argument
     * and the second item ID matches the second UUID argument.
     * Deduplication services always sort item UUIDs before creating or querying the DAO layer to
     * ensure reliable matching
     *
     * @param context   DSpace context
     * @param firstId   First item ID
     * @param secondId  Second item ID
     * @return          DAO query
     * @throws SQLException
     */
    private Query queryByFirstAndSecond(Context context, UUID firstId, UUID secondId) throws SQLException {
        Query query = createQuery(context, "SELECT d FROM Deduplication d"
                + " WHERE d.firstItemId = :firstItemId and d.secondItemId = :secondItemId");

        query.setParameter("firstItemId", firstId);
        query.setParameter("secondItemId", secondId);

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return query;
    }
}
