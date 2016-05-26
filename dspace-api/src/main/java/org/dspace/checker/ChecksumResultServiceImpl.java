/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.dspace.checker.dao.ChecksumResultDAO;
import org.dspace.checker.service.ChecksumResultService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

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

    protected ChecksumResultServiceImpl()
    {

    }

    /**
     * Get the result description for the given result code
     *
     * @param context Context
     * @param code
     *            to get the description for.
     * @return the found description.
     * @throws SQLException if database error
     */
    @Override
    public ChecksumResult findByCode(Context context, ChecksumResultCode code) throws SQLException
    {
        return checksumResultDAO.findByCode(context, code);
    }

    /**
     * Get a list of all the possible result codes.
     *
     * @param context Context
     * @return a list of all the result codes
     * @throws SQLException if database error
     */
    @Override
    public List<ChecksumResult> findAll(Context context) throws SQLException {
        return checksumResultDAO.findAll(context, ChecksumResult.class);
    }
}
