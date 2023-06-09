/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.content.ProcessStatus;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessQueryParameterContainer;

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

    /**
     * Returns a list of all Processes in the database which match the given field requirements. If the
     * requirements are not null, they will be combined with an AND operation.
     * @param context          The relevant DSpace context
     * @param processQueryParameterContainer       The {@link ProcessQueryParameterContainer} containing all the values
     *                                             that the returned {@link Process} objects must adhere to
     * @param limit            The limit for the amount of Processes returned
     * @param offset           The offset for the Processes to be returned
     * @return The list of all Processes which match the metadata requirements
     * @throws SQLException If something goes wrong
     */
    List<Process> search(Context context, ProcessQueryParameterContainer processQueryParameterContainer, int limit,
                         int offset) throws SQLException;

    /**
     * Count all the processes which match the requirements. The requirements are evaluated like the search
     * method.
     * @param context       The relevant DSpace context
     * @param processQueryParameterContainer       The {@link ProcessQueryParameterContainer} containing all the values
     *                                             that the returned {@link Process} objects must adhere to
     * @return The number of results matching the query
     * @throws SQLException If something goes wrong
     */

    int countTotalWithParameters(Context context, ProcessQueryParameterContainer processQueryParameterContainer)
        throws SQLException;

    /**
     * Find all the processes with one of the given status and with a creation time
     * older than the specified date.
     *
     * @param  context      The relevant DSpace context
     * @param  statuses     the statuses of the processes to search for
     * @param  date         the creation date to search for
     * @return              The list of all Processes which match requirements
     * @throws SQLException If something goes wrong
     */
    List<Process> findByStatusAndCreationTimeOlderThan(Context context, List<ProcessStatus> statuses, Date date)
        throws SQLException;

    /**
     * Returns a list of all Process objects in the database by the given user.
     *
     * @param context The relevant DSpace context
     * @param user    The user to search for
     * @param limit   The limit for the amount of Processes returned
     * @param offset  The offset for the Processes to be returned
     * @return The list of all Process objects in the Database
     * @throws SQLException If something goes wrong
     */
    List<Process> findByUser(Context context, EPerson user, int limit, int offset) throws SQLException;

    /**
     * Count all the processes which is related to the given user.
     *
     * @param context The relevant DSpace context
     * @param user    The user to search for
     * @return The number of results matching the query
     * @throws SQLException If something goes wrong
     */
    int countByUser(Context context, EPerson user) throws SQLException;

}
