/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao;

import org.dspace.checker.ChecksumResult;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;

/**
 * Database Access Object interface class for the ChecksumResult object.
 * The implementation of this class is responsible for all database calls for the ChecksumResult object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ChecksumResultDAO extends GenericDAO<ChecksumResult> {

    public ChecksumResult findByCode(Context context, ChecksumResultCode code) throws SQLException;

}
