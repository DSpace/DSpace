/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handleredirect.dao;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.handleredirect.HandleRedirect;

/**
 * Database Access Object interface class for the RedirectHandle object.
 * The implementation of this class is responsible for all database calls for the
 * HandleRedirect object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author Ying Jin at rice.edu Updated from HandleDAO.java
 */
public interface HandleRedirectDAO extends GenericDAO<HandleRedirect> {

    public HandleRedirect findByHandleRedirect(Context context, String handleredirect) throws SQLException;

    public int countRows(Context context) throws SQLException;
}
