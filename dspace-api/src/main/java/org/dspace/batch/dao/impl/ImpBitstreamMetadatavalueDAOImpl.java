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
import org.dspace.batch.ImpBitstreamMetadatavalue;
import org.dspace.batch.dao.ImpBitstreamMetadatavalueDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the ImpBitstreamMetadatavalue
 * object. The implementation of this class is responsible for all database
 * calls for the ImpBitstreamMetadatavalue object and is autowired by spring
 * This class should only be accessed from a single service and should never be
 * exposed outside of the API
 *
 * @author fcadili (francesco.cadili at 4science.it)
 */
public class ImpBitstreamMetadatavalueDAOImpl extends AbstractHibernateDAO<ImpBitstreamMetadatavalue>
        implements ImpBitstreamMetadatavalueDAO {

    @Override
    public List<ImpBitstreamMetadatavalue> searchByImpBitstream(Context context, ImpBitstream impBitstream)
            throws SQLException {
        Query query = createQuery(context, "SELECT m FROM ImpBitstreamMetadatavalue m LEFT JOIN FETCH m.impBitstream "
                + "WHERE m.impBitstream.impBitstreamId = :impBitstreamId "
                + "ORDER BY m.impBitstreamMetadatavalueId, m.impSchema, m.impElement, m.impQualifier, m.metadataOrder");
        query.setParameter("impBitstreamId", impBitstream.getImpBitstreamId());

        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
        return list(query);
    }

    @Override
    public void deleteAll(Context context) throws SQLException {
        getHibernateSession(context).createQuery("delete from ImpBitstreamMetadatavalue").executeUpdate();
    }
}
