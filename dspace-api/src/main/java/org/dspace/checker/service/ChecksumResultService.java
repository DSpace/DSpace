/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.checker.ChecksumResult;
import org.dspace.checker.ChecksumResultCode;
import org.hibernate.Session;

/**
 * Service interface class for the ChecksumResult object.
 * The implementation of this class is responsible for all business logic calls for the ChecksumResult object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ChecksumResultService {

    public ChecksumResult findByCode(Session session, ChecksumResultCode code) throws SQLException;

    public List<ChecksumResult> findAll(Session session) throws SQLException;
}
