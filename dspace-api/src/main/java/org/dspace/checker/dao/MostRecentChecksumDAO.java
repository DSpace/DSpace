/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.MostRecentChecksum;
import org.dspace.content.Bitstream;
import org.dspace.core.GenericDAO;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the MostRecentChecksum object.
 * The implementation of this class is responsible for all database calls for
 * the MostRecentChecksum object and is autowired by Spring.
 * This class should only be accessed from a single service and should never be
 * exposed outside of the API.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MostRecentChecksumDAO extends GenericDAO<MostRecentChecksum> {

    public List<MostRecentChecksum> findByNotProcessedInDateRange(Session session, Date startDate, Date endDate)
        throws SQLException;

    public List<MostRecentChecksum> findByResultTypeInDateRange(Session session, Date startDate, Date endDate,
                                                                ChecksumResultCode resultCode) throws SQLException;

    public void deleteByBitstream(Session session, Bitstream bitstream) throws SQLException;

    public MostRecentChecksum getOldestRecord(Session session) throws SQLException;

    public MostRecentChecksum getOldestRecord(Session session, Date lessThanDate) throws SQLException;

    public List<MostRecentChecksum> findNotInHistory(Session session) throws SQLException;

    public MostRecentChecksum findByBitstream(Session session, Bitstream bitstream) throws SQLException;
}
