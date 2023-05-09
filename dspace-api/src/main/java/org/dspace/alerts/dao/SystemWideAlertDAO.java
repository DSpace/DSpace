/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.alerts.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.alerts.SystemWideAlert;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * This is the Data Access Object for the {@link SystemWideAlert} object
 */
public interface SystemWideAlertDAO extends GenericDAO<SystemWideAlert> {

    /**
     * Returns a list of all SystemWideAlert objects in the database
     *
     * @param context The relevant DSpace context
     * @param limit   The limit for the amount of SystemWideAlerts returned
     * @param offset  The offset for the Processes to be returned
     * @return The list of all SystemWideAlert objects in the Database
     * @throws SQLException If something goes wrong
     */
    List<SystemWideAlert> findAll(Context context, int limit, int offset) throws SQLException;

    /**
     * Returns a list of all active SystemWideAlert objects in the database
     *
     * @param context The relevant DSpace context
     * @param limit   The limit for the amount of SystemWideAlerts returned
     * @param offset  The offset for the Processes to be returned
     * @return The list of all SystemWideAlert objects in the Database
     * @throws SQLException If something goes wrong
     */
    List<SystemWideAlert> findAllActive(Context context, int limit, int offset) throws SQLException;


}
