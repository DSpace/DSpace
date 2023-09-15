/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.sql.SQLException;
import java.util.List;

import org.dspace.checker.dao.ChecksumResultDAO;
import org.dspace.checker.service.ChecksumResultService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the ChecksumResult object.
 * This class is responsible for all business logic calls for the ChecksumResult object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ChecksumResultServiceImpl implements ChecksumResultService {

    @Autowired(required = true)
    private ChecksumResultDAO checksumResultDAO;

    protected ChecksumResultServiceImpl() {

    }

    /**
     * Get the result description for the given result code
     *
     * @param session Context
     * @param code    to get the description for.
     * @return the found description.
     * @throws SQLException if database error
     */
    @Override
    public ChecksumResult findByCode(Session session, ChecksumResultCode code) throws SQLException {
        return checksumResultDAO.findByCode(session, code);
    }

    /**
     * Get a list of all the possible result codes.
     *
     * @param session Context
     * @return a list of all the result codes
     * @throws SQLException if database error
     */
    @Override
    public List<ChecksumResult> findAll(Session session) throws SQLException {
        return checksumResultDAO.findAll(session, ChecksumResult.class);
    }
}
