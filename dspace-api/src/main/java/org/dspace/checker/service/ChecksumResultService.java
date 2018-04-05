/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.service;

import org.dspace.checker.ChecksumResult;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the ChecksumResult object.
 * The implementation of this class is responsible for all business logic calls for the ChecksumResult object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ChecksumResultService {

    public ChecksumResult findByCode(Context context, ChecksumResultCode code) throws SQLException;

    public List<ChecksumResult> findAll(Context context) throws SQLException;
}
