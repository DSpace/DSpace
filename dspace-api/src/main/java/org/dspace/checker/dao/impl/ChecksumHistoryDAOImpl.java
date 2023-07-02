/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao.impl;

import java.sql.SQLException;
import java.util.Date;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.dao.ChecksumHistoryDAO;
import org.dspace.content.Bitstream;
import org.dspace.core.AbstractHibernateDAO;
import org.hibernate.Session;

/**
 * <p>
 * This is the data access for the checksum history information. All
 * update,insert and delete database operations should go through this class for
 * checksum history operations.
 * </p>
 *
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * @author kevinvandevelde at atmire.com
 *
 *
 */
public class ChecksumHistoryDAOImpl extends AbstractHibernateDAO<ChecksumHistory> implements ChecksumHistoryDAO {

    protected ChecksumHistoryDAOImpl() {
        super();
    }

    @Override
    public int deleteByDateAndCode(Session session, Date retentionDate, ChecksumResultCode resultCode)
        throws SQLException {
        String hql = "delete from ChecksumHistory where processEndDate < :processEndDate AND checksumResult" +
            ".resultCode=:resultCode";
        Query query = createQuery(session, hql);
        query.setParameter("processEndDate", retentionDate, TemporalType.TIMESTAMP);
        query.setParameter("resultCode", resultCode);
        return query.executeUpdate();
    }

    @Override
    public void deleteByBitstream(Session session, Bitstream bitstream) throws SQLException {
        String hql = "delete from ChecksumHistory where bitstream=:bitstream";
        Query query = createQuery(session, hql);
        query.setParameter("bitstream", bitstream);
        query.executeUpdate();
    }

}
