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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumHistory_;
import org.dspace.checker.ChecksumResult;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.ChecksumResult_;
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
        // For Hibernate 7 compatibility: Use entity-based deletion instead of bulk delete.
        // Bulk HQL deletes don't update the persistence context, causing TransientPropertyValueException.
        List<ChecksumHistory> histories = findByDateAndCode(context, retentionDate, resultCode);
        for (ChecksumHistory history : histories) {
            delete(context, history);
        }
        return histories.size();
    }

    @Override
    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException {
        // For Hibernate 7 compatibility: Use entity-based deletion instead of bulk delete.
        // Bulk HQL deletes don't update the persistence context, causing TransientPropertyValueException.
        List<ChecksumHistory> histories = findByBitstream(context, bitstream);
        for (ChecksumHistory history : histories) {
            delete(context, history);
        }
    }

    /**
     * Find ChecksumHistory records by date and result code.
     * Used for Hibernate 7 compatible entity-based deletion.
     *
     * @param context the DSpace context
     * @param retentionDate the cutoff date (find records before this date)
     * @param resultCode the checksum result code
     * @return list of matching ChecksumHistory records
     * @throws SQLException if database error occurs
     */
    public List<ChecksumHistory> findByDateAndCode(Context context, Instant retentionDate,
                                                    ChecksumResultCode resultCode) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<ChecksumHistory> criteriaQuery = getCriteriaQuery(criteriaBuilder, ChecksumHistory.class);
        Root<ChecksumHistory> checksumHistoryRoot = criteriaQuery.from(ChecksumHistory.class);
        Join<ChecksumHistory, ChecksumResult> checksumResult =
            checksumHistoryRoot.join(ChecksumHistory_.checksumResult);
        criteriaQuery.select(checksumHistoryRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.lessThan(checksumHistoryRoot.get(ChecksumHistory_.processEndDate), retentionDate),
            criteriaBuilder.equal(checksumResult.get(ChecksumResult_.resultCode), resultCode)
        ));
        return list(context, criteriaQuery, false, ChecksumHistory.class, -1, -1);
    }

    /**
     * Find ChecksumHistory records by bitstream.
     * Used for Hibernate 7 compatible entity-based deletion.
     *
     * @param context the DSpace context
     * @param bitstream the bitstream to find history for
     * @return list of matching ChecksumHistory records
     * @throws SQLException if database error occurs
     */
    public List<ChecksumHistory> findByBitstream(Context context, Bitstream bitstream) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<ChecksumHistory> criteriaQuery = getCriteriaQuery(criteriaBuilder, ChecksumHistory.class);
        Root<ChecksumHistory> checksumHistoryRoot = criteriaQuery.from(ChecksumHistory.class);
        criteriaQuery.select(checksumHistoryRoot);
        criteriaQuery.where(criteriaBuilder.equal(checksumHistoryRoot.get(ChecksumHistory_.bitstream), bitstream));
        return list(context, criteriaQuery, false, ChecksumHistory.class, -1, -1);
    }

}
