/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.handler;

import java.sql.SQLException;

import org.apache.commons.cli.Options;

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
     * This method will print the help for the options and name
     * @param options   The options for the script
     * @param name      The name of the script
     */
    public void printHelp(Options options, String name);
}
