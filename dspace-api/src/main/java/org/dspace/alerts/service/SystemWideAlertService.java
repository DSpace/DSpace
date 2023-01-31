/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.alerts.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.alerts.AllowSessionsEnum;
import org.dspace.alerts.SystemWideAlert;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * An interface for the SystemWideAlertService with methods regarding the SystemWideAlert workload
 */
public interface SystemWideAlertService {

    /**
     * This method will create a SystemWideAlert object in the database
     *
     * @param context           The relevant DSpace context
     * @param message           The message of the system-wide alert
     * @param allowSessionsType Which sessions need to be allowed for the system-wide alert
     * @param countdownTo       The date to which to count down to when the system-wide alert is active
     * @param active            Whether the system-wide alert os active
     * @return The created SystemWideAlert object
     * @throws SQLException If something goes wrong
     */
    SystemWideAlert create(Context context, String message, AllowSessionsEnum allowSessionsType,
                           Date countdownTo, boolean active
    ) throws SQLException, AuthorizeException;

    /**
     * This method will retrieve a SystemWideAlert object from the Database with the given ID
     *
     * @param context The relevant DSpace context
     * @param alertId The alert id on which we'll search for in the database
     * @return The system-wide alert that holds the given alert id
     * @throws SQLException If something goes wrong
     */
    SystemWideAlert find(Context context, int alertId) throws SQLException;

    /**
     * Returns a list of all SystemWideAlert objects in the database
     *
     * @param context The relevant DSpace context
     * @return The list of all SystemWideAlert objects in the Database
     * @throws SQLException If something goes wrong
     */
    List<SystemWideAlert> findAll(Context context) throws SQLException;

    /**
     * Returns a list of all SystemWideAlert objects in the database
     *
     * @param context The relevant DSpace context
     * @param limit   The limit for the amount of system-wide alerts returned
     * @param offset  The offset for the system-wide alerts to be returned
     * @return The list of all SystemWideAlert objects in the Database
     * @throws SQLException If something goes wrong
     */
    List<SystemWideAlert> findAll(Context context, int limit, int offset) throws SQLException;


    /**
     * Returns a list of all active SystemWideAlert objects in the database
     *
     * @param context The relevant DSpace context
     * @return The list of all active SystemWideAlert objects in the database
     * @throws SQLException If something goes wrong
     */
    List<SystemWideAlert> findAllActive(Context context, int limit, int offset) throws SQLException;

    /**
     * This method will delete the given SystemWideAlert object from the database
     *
     * @param context         The relevant DSpace context
     * @param systemWideAlert The SystemWideAlert object to be deleted
     * @throws SQLException If something goes wrong
     */
    void delete(Context context, SystemWideAlert systemWideAlert)
            throws SQLException, IOException, AuthorizeException;


    /**
     * This method will be used to update the given SystemWideAlert object in the database
     *
     * @param context         The relevant DSpace context
     * @param systemWideAlert The SystemWideAlert object to be updated
     * @throws SQLException If something goes wrong
     */
    void update(Context context, SystemWideAlert systemWideAlert) throws SQLException, AuthorizeException;


    /**
     * Verifies if the user connected to the current context can retain its session
     *
     * @param context The relevant DSpace context
     * @return if the user connected to the current context can retain its session
     */
    boolean canUserMaintainSession(Context context, EPerson ePerson) throws SQLException;


    /**
     * Verifies if a non admin user can log in
     *
     * @param context The relevant DSpace context
     * @return if a non admin user can log in
     */
    boolean canNonAdminUserLogin(Context context) throws SQLException;
}
