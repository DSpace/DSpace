/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao.impl;

import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.MostRecentChecksum;
import org.dspace.checker.dao.MostRecentChecksumDAO;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.*;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the MostRecentChecksum object.
 * This class is responsible for all database calls for the MostRecentChecksum object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MostRecentChecksumDAOImpl extends AbstractHibernateDAO<MostRecentChecksum> implements MostRecentChecksumDAO
{
    protected MostRecentChecksumDAOImpl()
    {
        super();
    }


    @Override
    public List<MostRecentChecksum> findByNotProcessedInDateRange(Context context, Date startDate, Date endDate) throws SQLException {
//                    + "most_recent_checksum.last_process_start_date, most_recent_checksum.last_process_end_date, "
//                    + "most_recent_checksum.expected_checksum, most_recent_checksum.current_checksum, "
//                    + "result_description "
//                    + "from checksum_results, most_recent_checksum "
//                    + "where most_recent_checksum.to_be_processed = false "
//                    + "and most_recent_checksum.result = checksum_results.result_code "
//                    + "and most_recent_checksum.last_process_start_date >= ? "
//                    + "and most_recent_checksum.last_process_start_date < ? "
//                    + "order by most_recent_checksum.bitstream_id

        Criteria criteria = createCriteria(context, MostRecentChecksum.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("toBeProcessed", false),
                        Restrictions.le("processStartDate", startDate),
                        Restrictions.gt("processStartDate", endDate)
                )
        );
        criteria.addOrder(Order.asc("bitstream.id"));
        return list(criteria);
    }


    @Override
    public MostRecentChecksum findByBitstream(Context context, Bitstream bitstream) throws SQLException {
        Criteria criteria = createCriteria(context, MostRecentChecksum.class);
        criteria.add(Restrictions.eq("bitstream", bitstream));
        return singleResult(criteria);
    }


    @Override
    public List<MostRecentChecksum> findByResultTypeInDateRange(Context context, Date startDate, Date endDate, ChecksumResultCode resultCode) throws SQLException {
//        "select bitstream_id, last_process_start_date, last_process_end_date, "
//                    + "expected_checksum, current_checksum, result_description "
//                    + "from most_recent_checksum, checksum_results "
//                    + "where most_recent_checksum.result = checksum_results.result_code "
//                    + "and most_recent_checksum.result= ? "
//                    + "and most_recent_checksum.last_process_start_date >= ? "
//                    + "and most_recent_checksum.last_process_start_date < ? "
//                    + "order by bitstream_id";
        Criteria criteria = createCriteria(context, MostRecentChecksum.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("checksumResult.resultCode", resultCode),
                        Restrictions.le("processStartDate", startDate),
                        Restrictions.gt("processStartDate", endDate)
                )
        );
        criteria.addOrder(Order.asc("bitstream.id"));
        return list(criteria);

    }

    @Override
    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException
    {
        String hql = "delete from MostRecentChecksum WHERE bitstream=:bitstream";
        Query query = createQuery(context, hql);
        query.setParameter("bitstream", bitstream);
        query.executeUpdate();
    }

    @Override
    public MostRecentChecksum getOldestRecord(Context context) throws SQLException {
        //        "select bitstream_id  "
        //        + "from most_recent_checksum " + "where to_be_processed = true "
        //        + "order by date_trunc('milliseconds', last_process_end_date), "
        //        + "bitstream_id " + "ASC LIMIT 1";
        Criteria criteria = createCriteria(context, MostRecentChecksum.class);
        criteria.add(Restrictions.eq("toBeProcessed", true));
        criteria.addOrder(Order.asc("processEndDate")).addOrder(Order.asc("bitstream.id"));
        criteria.setMaxResults(1);
        return singleResult(criteria);
    }

    @Override
    public MostRecentChecksum getOldestRecord(Context context, Date lessThanDate) throws SQLException {
//                "select bitstream_id  "
//                + "from most_recent_checksum "
//                + "where to_be_processed = true "
//                + "and last_process_start_date < ? "
//                + "order by date_trunc('milliseconds', last_process_end_date), "
//                + "bitstream_id " + "ASC LIMIT 1";
        Criteria criteria = createCriteria(context, MostRecentChecksum.class);
        criteria.add(
                Restrictions.and(
                        Restrictions.eq("toBeProcessed", true),
                        Restrictions.lt("processStartDate", lessThanDate)
                ));
        criteria.addOrder(Order.asc("processEndDate")).addOrder(Order.asc("bitstream.id"));
        criteria.setMaxResults(1);
        return singleResult(criteria);
    }

    @Override
    public List<MostRecentChecksum> findNotInHistory(Context context) throws SQLException {
        Criteria criteria = createCriteria(context, MostRecentChecksum.class);
        DetachedCriteria subCriteria = DetachedCriteria.forClass(ChecksumHistory.class);
        subCriteria.setProjection(Projections.property("bitstream.id"));
        criteria.add(Property.forName("bitstreamId").notIn(subCriteria));
        return list(criteria);
    }
}
