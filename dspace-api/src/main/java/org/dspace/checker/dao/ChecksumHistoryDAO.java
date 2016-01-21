/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao;

import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;
import java.util.Date;

/**
 * Database Access Object interface class for the ChecksumHistory object.
 * The implementation of this class is responsible for all database calls for the ChecksumHistory object and is autowired by spring
 * This class should only be accessed from a single service & should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ChecksumHistoryDAO extends GenericDAO<ChecksumHistory> {
    public int deleteByDateAndCode(Context context, Date retentionDate, ChecksumResultCode checksumResultCode) throws SQLException;

    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException;
}
