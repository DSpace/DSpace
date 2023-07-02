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
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessQueryParameterContainer;
import org.hibernate.Session;

/**
 * This is the Data Access Object for the {@link Process} object
 */
public interface ProcessDAO extends GenericDAO<Process> {

    /**
     * This method will return all the Process objects in the database in a list
     * sorted by script name.
     * @param session   The current request's database context.
     * @return          The list of all Process objects in the database sorted on script name.
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAllSortByScript(Session session) throws SQLException;

    /**
     * This method will return all the Process objects in the database in a list
     * sorted by start time.
     * The most recent one will be shown first
     * @param session   The current request's database context.
     * @return          The list of all Process objects in the database sorted by start time
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAllSortByStartTime(Session session) throws SQLException;

    /**
     * Returns a list of all Process objects in the database
     * @param session   The current request's database context.
     * @param limit     The limit for the amount of Processes returned
     * @param offset    The offset for the Processes to be returned
     * @return The list of all Process objects in the Database
     * @throws SQLException If something goes wrong
     */
    List<Process> findAll(Session session, int limit, int offset) throws SQLException;

    /**
     * Returns the total number of Process objects in the database.
     * @param session   The current request's database context.
     * @return          An integer that describes the amount of Process objects in the database
     * @throws SQLException If something goes wrong
     */
    int countRows(Session session) throws SQLException;

    /**
     * Returns a list of all Processes in the database which match the given
     * field requirements. If the requirements are not null, they will be
     * combined with an AND operation.
     * @param session          The current request's database context.
     * @param processQueryParameterContainer       The {@link ProcessQueryParameterContainer} containing all the values
     *                                             that the returned {@link Process} objects must adhere to
     * @param limit            The limit for the amount of Processes returned
     * @param offset           The offset for the Processes to be returned
     * @return The list of all Processes which match the metadata requirements
     * @throws SQLException If something goes wrong
     */
    List<Process> search(Session session, ProcessQueryParameterContainer processQueryParameterContainer, int limit,
                         int offset) throws SQLException;

    /**
     * Count all the processes which match the requirements. The requirements are evaluated like the search
     * method.
     * @param session       The current request's database context.
     * @param processQueryParameterContainer       The {@link ProcessQueryParameterContainer} containing all the values
     *                                             that the returned {@link Process} objects must adhere to
     * @return The number of results matching the query
     * @throws SQLException If something goes wrong
     */

    int countTotalWithParameters(Session session, ProcessQueryParameterContainer processQueryParameterContainer)
        throws SQLException;

    /**
     * Find all the processes with one of the given status and with a creation time
     * older than the specified date.
     *
     * @param  session      The current request's database context.
     * @param  statuses     the statuses of the processes to search for
     * @param  date         the creation date to search for
     * @return              The list of all Processes which match requirements
     * @throws SQLException If something goes wrong
     */
    List<Process> findByStatusAndCreationTimeOlderThan(Session session, List<ProcessStatus> statuses, Date date)
        throws SQLException;

    /**
     * Returns a list of all Process objects in the database by the given user.
     *
     * @param session The current request's database context.
     * @param user    The user to search for
     * @param limit   The limit for the amount of Processes returned
     * @param offset  The offset for the Processes to be returned
     * @return The list of all Process objects in the Database
     * @throws SQLException If something goes wrong
     */
    List<Process> findByUser(Session session, EPerson user, int limit, int offset) throws SQLException;

    /**
     * Count all the processes which is related to the given user.
     *
     * @param session The current request's database context.
     * @param user    The user to search for
     * @return The number of results matching the query
     * @throws SQLException If something goes wrong
     */
    int countByUser(Session session, EPerson user) throws SQLException;

}
