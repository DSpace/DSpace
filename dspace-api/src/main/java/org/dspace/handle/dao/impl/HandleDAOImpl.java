/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.dao.impl;

import org.dspace.content.DSpaceObject;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.handle.Handle;
import org.dspace.handle.dao.HandleDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.service.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.service.jdbc.dialect.spi.DialectResolver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the Handle object.
 * This class is responsible for all database calls for the Handle object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HandleDAOImpl extends AbstractHibernateDAO<Handle> implements HandleDAO
{
    // The name of the sequence used to determine next available handle
    private static final String HANDLE_SEQUENCE = "handle_seq";

    protected HandleDAOImpl()
    {
        super();
    }

    @Override
    public List<Handle> getHandlesByDSpaceObject(Context context, DSpaceObject dso) throws SQLException {
        if(dso == null) {
            return Collections.emptyList();
        } else {
            Query query = createQuery(context,
                    "SELECT h " +
                    "FROM Handle h " +
                    "LEFT JOIN FETCH h.dso " +
                    "WHERE h.dso.id = :id ");

            query.setParameter("id", dso.getID());

            query.setCacheable(true);
            return list(query);
        }
    }

    @Override
    public Handle findByHandle(Context context, String handle) throws SQLException {
        Query query = createQuery(context,
                "SELECT h " +
                "FROM Handle h " +
                "LEFT JOIN FETCH h.dso " +
                "WHERE h.handle = :handle ");

        query.setParameter("handle", handle);

        query.setCacheable(true);
        return uniqueResult(query);
    }

    @Override
    public List<Handle> findByPrefix(Context context, String prefix) throws SQLException {
        Criteria criteria = createCriteria(context, Handle.class);
        criteria.add(Restrictions.like("handle", prefix + "%"));
        return list(criteria);
    }

    @Override
    public long countHandlesByPrefix(Context context, String prefix) throws SQLException {
        Criteria criteria = createCriteria(context, Handle.class);
        criteria.add(Restrictions.like("handle", prefix + "%"));
        return countLong(criteria);
    }

    @Override
    public int updateHandlesWithNewPrefix(Context context, String newPrefix, String oldPrefix) throws SQLException
    {
        String hql = "UPDATE Handle set handle = concat(:newPrefix, '/', substring(handle, :oldPrefixLength + 2)) WHERE handle like concat(:oldPrefix,'%')";
        Query query = createQuery(context, hql);
        query.setString("newPrefix", newPrefix);
        query.setInteger("oldPrefixLength", oldPrefix.length());
        query.setString("oldPrefix", oldPrefix);
        return query.executeUpdate();
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Handle"));
    }

    /**
     * Return next available value of Handle suffix (based on DB sequence).
     * @param context Current DSpace Context
     * @return next available Handle suffix (as a Long)
     * @throws SQLException if database error or sequence doesn't exist
     */
    @Override
    public Long getNextHandleSuffix(Context context) throws SQLException
    {
        // Create a new Hibernate ReturningWork, which will return the
        // result of the next value in the Handle Sequence.
        ReturningWork<Long> nextValReturningWork = new ReturningWork<Long>() {
            @Override
            public Long execute(Connection connection) throws SQLException {
                Long nextVal = 0L;

                // Determine what dialect we are using for this DB
                DialectResolver dialectResolver = new StandardDialectResolver();
                Dialect dialect = dialectResolver.resolveDialect(connection.getMetaData());

                // Find the next value in our sequence (based on DB dialect)
                try (PreparedStatement preparedStatement = connection.prepareStatement(dialect.getSequenceNextValString(HANDLE_SEQUENCE)))
                {
                    // Execute query and return results
                    try(ResultSet resultSet = preparedStatement.executeQuery())
                    {
                        if(resultSet.next())
                        {
                            // Return result of query (from first column)
                            nextVal = resultSet.getLong(1);
                        }
                    }
                }
                return nextVal;
            }
        };

        // Run our work, returning the next value in the sequence (see 'nextValReturningWork' above)
        return getHibernateSession(context).doReturningWork(nextValReturningWork);
    }
}
