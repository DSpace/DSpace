/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.handler;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

/**
 * This is an interface meant to be implemented by any DSpaceRunnableHandler to specify specific execution methods
 * of the script depending on where it was called from
 */
public interface DSpaceRunnableHandler {

    /**
     * This method handles the start of the script
     * @throws SQLException If something goes wrong
     */
    public void start() throws SQLException;

    /**
     * This method handles the completion of the script
     * @throws SQLException If something goes wrong
     */
    public void handleCompletion() throws SQLException;

    /**
     * This method handles an exception thrown by the script
     * @param e The exception thrown by the script
     */
    public void handleException(Exception e);

    /**
     * This method handles an exception thrown by the script
     * @param message   The String message for the exception thrown by the script
     */
    public void handleException(String message);

    /**
     * This method handles an exception thrown by the script
     * @param message   The String message for the exception thrown by the script
     * @param e         The exception thrown by the script
     */
    public void handleException(String message, Exception e);

    /**
     * This method will perform the debug logging of the message given
     * @param message   The message to be logged as debug
     */
    public void logDebug(String message);

    /**
     * This method will perform the info logging of the message given
     * @param message   The message to be logged as info
     */
    public void logInfo(String message);

    /**
     * This method will perform the warning logging of the message given
     * @param message   The message to be logged as warning
     */
    public void logWarning(String message);

    /**
     * This method will perform the error logging of the message given
     * @param message   The message to be logged as an error
     */
    public void logError(String message);

    /**
     * This method will perform the error logging of the message given along with a stack trace
     * @param message   The message to be logged as an error
     * @param throwable The original exception
     */
    public void logError(String message, Throwable throwable);

    /**
     * This method will print the help for the options and name
     * @param options   The options for the script
     * @param name      The name of the script
     */
    public void printHelp(Options options, String name);

    /**
     * This method will grab the InputStream for the file defined by the given file name. The exact implementation will
     * differ based on whether it's a REST call or CommandLine call. The REST Call will look for Bitstreams in the
     * Database whereas the CommandLine call will look on the filesystem
     * @param context       The relevant DSpace context
     * @param fileName      The filename for the file that holds the InputStream
     * @return              The InputStream for the file defined by the given file name
     * @throws IOException  If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    public Optional<InputStream> getFileStream(Context context, String fileName) throws IOException, AuthorizeException;

    /**
     * This method will write the InputStream to either a file on the filesystem or a bitstream in the database
     * depending on whether it's coming from a CommandLine call or REST call respectively
     * @param context       The relevant DSpace context
     * @param fileName      The filename
     * @param inputStream   The inputstream to be written
     * @param type          The type of the file
     * @throws IOException  If something goes wrong
     */
    public void writeFilestream(Context context, String fileName, InputStream inputStream, String type)
        throws IOException, SQLException, AuthorizeException;

    /**
     * This method will return a List of UUIDs for the special groups
     * associated with the processId contained by specific implementations of this interface.
     * Otherwise, it returns an empty collection.
     * @return List containing UUIDs of Special Groups of the associated Process.
     */
    public List<UUID> getSpecialGroups();
}
