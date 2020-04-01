/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.dao.impl;

import java.sql.SQLException;
import javax.persistence.Query;

import org.dspace.batch.ImpRecordToItem;
import org.dspace.batch.dao.ImpRecordToItemDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the ImpRecordToItem object. The
 * implementation of this class is responsible for all database calls for the
 * ImpRecordToItem object and is autowired by spring This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author fcadili (francesco.cadili at 4science.it)
 */
public class ImpRecordToItemDAOImpl extends AbstractHibernateDAO<ImpRecordToItem> implements ImpRecordToItemDAO {

    @Override
    public ImpRecordToItem findByPK(Context context, String impRecordId) throws SQLException {
        Query query = createQuery(context, "SELECT r FROM ImpRecordToItem r WHERE r.impRecordId = :impRecordId");
        query.setParameter("impRecordId", impRecordId);

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return singleResult(query);
    }

    @Override
    public void deleteAll(Context context) throws SQLException {
        getHibernateSession(context).createQuery("delete from ImpRecordToItem").executeUpdate();
    }
}