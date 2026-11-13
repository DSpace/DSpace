/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao.impl;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.Query;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.dao.ChecksumHistoryDAO;
import org.dspace.content.Bitstream;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

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
    public int deleteByDateAndCode(Context context, Instant retentionDate, ChecksumResultCode resultCode)
        throws SQLException {
        String hql = "delete from ChecksumHistory where processEndDate < :processEndDate AND checksumResult" +
            ".resultCode=:resultCode";
        Query query = createQuery(context, hql);
        query.setParameter("processEndDate", retentionDate);
        query.setParameter("resultCode", resultCode);
        return query.executeUpdate();
    }

    @Override
    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException {
        String hql = "delete from ChecksumHistory where bitstream=:bitstream";
        Query query = createQuery(context, hql);
        query.setParameter("bitstream", bitstream);
        query.executeUpdate();
    }

    @Override
    public List<ChecksumHistory> findByBitstream(Context context, Bitstream bitstream, int pageSize, int offset)
            throws SQLException {
        var hql = "SELECT c FROM ChecksumHistory c WHERE c.bitstream = :bitstream ORDER BY c.processStartDate DESC";
        Query query = createQuery(context, hql);
        query.setParameter("bitstream", bitstream);
        query.setMaxResults(pageSize);
        query.setFirstResult(offset);
        return list(query);
    }

    @Override
    public int countByBitstream(Context context, Bitstream bitstream) throws SQLException {
        var hql = "SELECT count(c) FROM ChecksumHistory c WHERE c.bitstream = :bitstream";
        Query query = createQuery(context, hql);
        query.setParameter("bitstream", bitstream);
        return count(query);
    }

    @Override
    public List<ChecksumHistory> findByResultCode(Context context, ChecksumResultCode checksumResultCode,
                                                  int pageSize, int offset) throws SQLException {
        var hql = """
            SELECT c FROM ChecksumHistory c
            WHERE c.checksumResult.resultCode = :resultCode
            ORDER BY c.processStartDate DESC
            """;
        Query query = createQuery(context, hql);
        query.setParameter("resultCode", checksumResultCode);
        query.setMaxResults(pageSize);
        query.setFirstResult(offset);
        return list(query);
    }

    @Override
    public int countByResultCode(Context context, ChecksumResultCode checksumResultCode) throws SQLException {
        var hql = "SELECT count(c) FROM ChecksumHistory c WHERE c.checksumResult.resultCode = :resultCode";
        Query query = createQuery(context, hql);
        query.setParameter("resultCode", checksumResultCode);
        return count(query);
    }

    @Override
    public ChecksumHistory findByID(Context context, Long id) throws SQLException {
        var hql = "SELECT c FROM ChecksumHistory c WHERE c.id = :id";
        Query query = createQuery(context, hql);
        query.setParameter("id", id);
        return singleResult(query);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        var hql = "SELECT count(c) FROM ChecksumHistory c";
        Query query = createQuery(context, hql);
        return count(query);
    }
}
