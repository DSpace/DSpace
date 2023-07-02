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

import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.content.Bitstream;
import org.dspace.core.GenericDAO;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the ChecksumHistory object.
 * The implementation of this class is responsible for all database calls for the ChecksumHistory object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ChecksumHistoryDAO extends GenericDAO<ChecksumHistory> {

    /**
     * Delete all ChecksumHistory rows with retention date before the given and
     * the specified result code.
     *
     * @param session            The current request's database context.
     * @param retentionDate      row must be older than this to be deleted.
     * @param checksumResultCode row must have this result to be deleted.
     * @return number of rows deleted.
     * @throws SQLException if database error
     */
    public int deleteByDateAndCode(Session session, Date retentionDate, ChecksumResultCode checksumResultCode)
        throws SQLException;

    /**
     * Delete all ChecksumHistory rows for the given Bitstream.
     *
     * @param session   The current request's database context.
     * @param bitstream which bitstream's checksums to delete
     * @throws SQLException if database error
     */
    public void deleteByBitstream(Session session, Bitstream bitstream) throws SQLException;
}
