/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessLogLevel;
import org.dspace.scripts.ProcessQueryParameterContainer;

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
     * @param specialGroups Allows to set special groups, associated with application context when process is created,
     *                      other than the ones derived from the eperson membership.
     * @return The created process
     * @throws SQLException If something goes wrong
     */
    public Process create(Context context, EPerson ePerson, String scriptName,
                          List<DSpaceCommandLineParameter> parameters,
                          final Set<Group> specialGroups) throws SQLException;

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
     * The method will create a bitstream from the given inputstream with the given type as metadata and given name
     * as name and attach it to the given process
     * @param context       The relevant DSpace context
     * @param process       The process for which the bitstream will be made
     * @param is            The inputstream for the bitstream
     * @param type          The type of the bitstream
     * @param fileName      The name of the bitstream
     * @throws IOException  If something goes wrong
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public void appendFile(Context context, Process process, InputStream is, String type, String fileName)
        throws IOException, SQLException, AuthorizeException;

    /**
     * This method will delete the given Process object from the database
     * @param context   The relevant DSpace context
     * @param process   The Process object to be deleted
     * @throws SQLException If something goes wrong
     */
    public void delete(Context context, Process process) throws SQLException, IOException, AuthorizeException;

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
     * This method will return the Bitstream that matches the given name for the given Process
     * @param context           The relevant DSpace context
     * @param process           The process that should hold the requested Bitstream
     * @param bitstreamName     The name of the requested Bitstream
     * @return                  The Bitstream from the given Process that matches the given bitstream name
     */
    public Bitstream getBitstreamByName(Context context, Process process, String bitstreamName);

    /**
     * This method will return the Bitstream for a given process with a given type
     * @param context   The relevant DSpace context
     * @param process   The process that holds the Bitstreams to be searched in
     * @param type      The type that the Bitstream must have
     * @return          The Bitstream of the given type for the given Process
     */
    public Bitstream getBitstream(Context context, Process process, String type);

    /**
     * This method will return all the Bitstreams for a given process
     * @param context   The relevant DSpace context
     * @param process   The process that holds the Bitstreams to be searched in
     * @return          The list of Bitstreams
     */
    public List<Bitstream> getBitstreams(Context context, Process process);

    /**
     * Returns the total amount of Process objects in the dataase
     * @param context   The relevant DSpace context
     * @return          An integer that describes the amount of Process objects in the database
     * @throws SQLException If something goes wrong
     */
    int countTotal(Context context) throws SQLException;

    /**
     * This will return a list of Strings where each String represents the type of a Bitstream in the Process given
     * @param context   The DSpace context
     * @param process   The Process object that we'll use to find the bitstreams
     * @return          A list of Strings where each String represents a fileType that is in the Process
     */
    public List<String> getFileTypesForProcessBitstreams(Context context, Process process);

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
    int countSearch(Context context, ProcessQueryParameterContainer processQueryParameterContainer) throws SQLException;
    /**
     * This method will append the given output to the {@link Process} its logs
     * @param processId     The ID of the {@link Process} to append the log for
     * @param scriptName    The name of the Script that Process runs
     * @param output        The output to append
     * @param processLogLevel   The loglevel of the output
     * @throws IOException  If something goes wrong
     */
    void appendLog(int processId, String scriptName, String output, ProcessLogLevel processLogLevel) throws IOException;

    /**
     * This method will create a {@link Bitstream} containing the logs for the given {@link Process}
     * @param context       The relevant DSpace context
     * @param process       The {@link Process} for which we're making the {@link Bitstream}
     * @throws IOException  If something goes wrong
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    void createLogBitstream(Context context, Process process)
             throws IOException, SQLException, AuthorizeException;
}
