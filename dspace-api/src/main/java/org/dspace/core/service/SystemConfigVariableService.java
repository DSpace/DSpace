/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.SystemConfigVariable;

/**
 * Service interface for retrieving system configuration variables allowed in templates.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public interface SystemConfigVariableService {

    /**
     * Gets all allowed system configuration variables with resolved values and placeholders.
     *
     * @param context the DSpace context
     * @return list of allowed system configuration variables sorted by key
     * @throws SQLException       if a database error occurs during authorization check
     * @throws AuthorizeException if the current user is not authorized
     */
    List<SystemConfigVariable> getConfigVariables(Context context) throws SQLException, AuthorizeException;

    /**
     * Gets a single system configuration variable by key, if allowed and configured.
     *
     * @param context the DSpace context
     * @param key the configuration property key
     * @return the configuration variable or null if not found
     * @throws SQLException       if a database error occurs during authorization check
     * @throws AuthorizeException if the current user is not authorized
     */
    SystemConfigVariable getConfigVariable(Context context, String key) throws SQLException, AuthorizeException;
}
