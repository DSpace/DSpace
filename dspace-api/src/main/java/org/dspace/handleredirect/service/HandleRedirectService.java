/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handleredirect.service;

import java.sql.SQLException;

import org.dspace.core.Context;

/**
 * Interface to the <a href="https://www.handle.net" target=_new>CNRI Handle
 * System</a>.
 *
 * <p>
 * The class is going to solve handles that are not hosted in DSpace anymore
 * </p>
 *
 * @author Ying Jin @ rice.edu
 */
public interface HandleRedirectService {

    /**
     * Return the local URL for handle, or null if handle cannot be found.
     *
     * The returned URL is a (non-handle-based) location where a dissemination
     * of the object referred to by handle can be obtained.
     *
     * @param context DSpace context
     * @param redirecthandle  The handle
     * @return The local URL
     * @throws SQLException If a database error occurs
     */
    public String resolveToURL(Context context, String redirecthandle)
        throws SQLException;

    int countTotal(Context context) throws SQLException;

}
