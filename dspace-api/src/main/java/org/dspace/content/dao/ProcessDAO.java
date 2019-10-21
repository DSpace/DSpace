/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.scripts.Process;

/**
 * This is the Data Access Object for the {@link Process} object
 */
public interface ProcessDAO extends GenericDAO<Process> {

    /**
     * This method will return all the Process objects in the database in a list and it'll be sorted by script name
     * @param context   The relevant DSpace context
     * @return          The list of all Process objects in the database sorted on scriptname
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAllSortByScript(Context context) throws SQLException;

    /**
     * This method will return all the Process objects in the database in a list and it'll be sorted by start time.
     * The most recent one will be shown first
     * @param context   The relevant DSpace context
     * @return          The list of all Process objects in the database sorted by starttime
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAllSortByStartTime(Context context) throws SQLException;

    /**
     * Returns a list of all Process objects in the database
     * @param context   The relevant DSpace context
     * @param limit     The limit for the amount of Processes returned
     * @param offset    The offset for the Processes to be returned
     * @return The list of all Process objects in the Database
     * @throws SQLException If something goes wrong
     */
    List<Process> findAll(Context context, int limit, int offset) throws SQLException;

    /**
     * Returns the total amount of Process objects in the dataase
     * @param context   The relevant DSpace context
     * @return          An integer that describes the amount of Process objects in the database
     * @throws SQLException If something goes wrong
     */
    int countRows(Context context) throws SQLException;

}
