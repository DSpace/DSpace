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

public interface DSpaceRunnableHandler {

    public void start() throws SQLException;

    public void handleCompletion() throws SQLException;

    public void handleException(Exception e);

    public void handleException(String message);

    public void handleException(String message, Exception e);

    public void logDebug(String message);

    public void logInfo(String message);

    public void logWarning(String message);

    public void logError(String message);

    public void printHelp(Options options, String name);
}
