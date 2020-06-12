/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;

import org.dspace.batch.ImpRecord;
import org.dspace.batch.dao.ImpRecordDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the ImpRecord object. The
 * implementation of this class is responsible for all database calls for the
 * ImpRecord object and is autowired by spring This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author fcadili (francesco.cadili at 4science.it)
 */
public class ImpRecordDAOImpl extends AbstractHibernateDAO<ImpRecord> implements ImpRecordDAO {

    public List<ImpRecord> searchNewRecords(Context context) throws SQLException {
        Query query = createQuery(context,
                "SELECT r FROM ImpRecord r WHERE r.lastModified IS NULL " + "ORDER BY r.impId");

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return list(query);
    }

    @Override
    public int countNewImpRecords(Context context, ImpRecord impRecord) throws SQLException {
        Query query = createQuery(context,
                "SELECT count(r) FROM ImpRecord r WHERE r.impRecordId = :impRecordId AND r.lastModified IS NULL "
                        + "GROUP BY r.impId " + "ORDER BY r.impId");
        query.setParameter("impRecordId", impRecord.getImpRecordId());

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return count(query);
    }

    @Override
    public void deleteAll(Context context) throws SQLException {
        getHibernateSession(context).createQuery("delete from ImpRecord").executeUpdate();
    }
}