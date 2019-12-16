/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;

/**
 * An interface for the ProcessService with methods regarding the Process workload
 */
public interface ProcessService {

    /**
     * This method will create a Process object in the database
     * @param context       The relevant DSpace context
     * @param ePerson       The ePerson for which this process will be created on
     * @param scriptName    The script name to be used for the process
     * @param parameters    The parameters to be used for the process
     * @return The created process
     * @throws SQLException If something goes wrong
     */
    public Process create(Context context, EPerson ePerson, String scriptName,
                          List<DSpaceCommandLineParameter> parameters) throws SQLException;

    /**
     * This method will retrieve a Process object from the Database with the given ID
     * @param context   The relevant DSpace context
     * @param processId The process id on which we'll search for in the database
     * @return The process that holds the given process id
     * @throws SQLException If something goes wrong
     */
    public Process find(Context context, int processId) throws SQLException;

    /**
     * Returns a list of all Process objects in the database
     * @param context   The relevant DSpace context
     * @return The list of all Process objects in the Database
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAll(Context context) throws SQLException;

    /**
     * Returns a list of all Process objects in the database
     * @param context   The relevant DSpace context
     * @param limit     The limit for the amount of Processes returned
     * @param offset    The offset for the Processes to be returned
     * @return The list of all Process objects in the Database
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAll(Context context, int limit, int offset) throws SQLException;


    /**
     * Returns a list of all Process objects in the database sorted by script name
     * @param context   The relevant DSpace context
     * @return The list of all Process objects in the database sorted by script name
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAllSortByScript(Context context) throws SQLException;

    /**
     * Returns a list of all Process objects in the database sorted by start time
     * The most recent one will be shown first
     * @param context   The relevant DSpace context
     * @return The list of all Process objects sorted by start time
     * @throws SQLException If something goes wrong
     */
    public List<Process> findAllSortByStartTime(Context context) throws SQLException;

    /**
     * This method will perform the logic needed to update the Process object in the database to represent a
     * started state. A started state refers to {@link org.dspace.content.ProcessStatus#RUNNING}
     * @param context   The relevant DSpace context
     * @param process   The Process object to be updated
     * @throws SQLException If something goes wrong
     */
    public void start(Context context, Process process) throws SQLException;

    /**
     * This method will perform the logic needed to update the Process object in the database to represent
     * a failed state
     * @param context   The relevant DSpace context
     * @param process   The Process object to be updated
     * @throws SQLException If something goes wrong
     */
    public void fail(Context context, Process process) throws SQLException;

    /**
     * This method will perform the logic needed to update the Process object in the database to represent
     * a complete state
     * @param context   The relevant DSpace context
     * @param process   The Process object to be updated
     * @throws SQLException If something goes wrong
     */
    public void complete(Context context, Process process) throws SQLException;

    /**
     * This method will delete the given Process object from the database
     * @param context   The relevant DSpace context
     * @param process   The Process object to be deleted
     * @throws SQLException If something goes wrong
     */
    public void delete(Context context, Process process) throws SQLException;

    /**
     * This method will be used to update the given Process object in the database
     * @param context   The relevant DSpace context
     * @param process   The Process object to be updated
     * @throws SQLException If something goes wrong
     */
    public void update(Context context, Process process) throws SQLException;

    /**
     * This method will retrieve the list of parameters from the Process in its String format and it will parse
     * these parameters to a list of {@link DSpaceCommandLineParameter} objects for better usability throughout DSpace
     * @param process   The Process object for which we'll return the parameters
     * @return The list of parsed parameters from the Process object
     */
    public List<DSpaceCommandLineParameter> getParameters(Process process);

    /**
     * Returns the total amount of Process objects in the dataase
     * @param context   The relevant DSpace context
     * @return          An integer that describes the amount of Process objects in the database
     * @throws SQLException If something goes wrong
     */
    int countTotal(Context context) throws SQLException;

}
