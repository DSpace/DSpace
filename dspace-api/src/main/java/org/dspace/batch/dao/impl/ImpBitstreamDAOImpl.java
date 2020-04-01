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

import org.dspace.batch.ImpBitstream;
import org.dspace.batch.ImpRecord;
import org.dspace.batch.dao.ImpBitstreamDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the ImpBitstream object. The
 * implementation of this class is responsible for all database calls for the
 * ImpBitstream object and is autowired by spring This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author fcadili (francesco.cadili at 4science.it)
 */
public class ImpBitstreamDAOImpl extends AbstractHibernateDAO<ImpBitstream> implements ImpBitstreamDAO {

    @Override
    public List<ImpBitstream> searchByImpRecord(Context context, ImpRecord impRecord) throws SQLException {
        Query query = createQuery(context, "SELECT b FROM ImpBitstream b LEFT JOIN FETCH b.impRecord "
                + "WHERE b.impRecord.impId = :impId ORDER BY b.bitstreamOrder");
        query.setParameter("impId", impRecord.getImpId());

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return list(query);
    }

    @Override
    public void deleteAll(Context context) throws SQLException {
        getHibernateSession(context).createQuery("delete from ImpBitstream").executeUpdate();
    }
}
