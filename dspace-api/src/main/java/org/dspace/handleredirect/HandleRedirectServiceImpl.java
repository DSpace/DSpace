/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handleredirect;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.handleredirect.dao.HandleRedirectDAO;
import org.dspace.handleredirect.service.HandleRedirectService;
import org.springframework.beans.factory.annotation.Autowired;

public class HandleRedirectServiceImpl implements HandleRedirectService {

    @Autowired(required = true)
    protected HandleRedirectDAO handleRedirectDAO;

    /**
     * Public Constructor
     */
    protected HandleRedirectServiceImpl() {
    }

    @Override
    public String resolveToURL(Context context, String handle) throws SQLException {
        HandleRedirect dbhandle = findHandleInternal(context, handle);

        if (dbhandle == null) {
            return null;
        }
        return dbhandle.getUrl();
    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////
    /**
     * Find the database row corresponding to handleredirect.
     *
     * @param context DSpace context
     * @param handle  The handle to resolve
     * @return The database row corresponding to the handleredirect
     * @throws SQLException If a database error occurs
     */
    protected HandleRedirect findHandleInternal(Context context, String handle)
        throws SQLException {
        if (handle == null) {
            throw new IllegalArgumentException("Handle is null");
        }

        return handleRedirectDAO.findByHandleRedirect(context, handle);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return handleRedirectDAO.countRows(context);
    }

}