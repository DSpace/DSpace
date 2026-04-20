/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.scripts.handler.impl;

import java.util.ArrayList;
import java.util.List;

import org.dspace.scripts.handler.impl.CommandLineDSpaceRunnableHandler;

/**
 * This class will be used as a DSpaceRunnableHandler for the Tests so that we can stop the handler
 * from calling System.exit() when a script would throw an exception
 */
public class TestDSpaceRunnableHandler extends CommandLineDSpaceRunnableHandler {

    private Exception exception = null;

    private final List<String> infoMessages = new ArrayList<>();

    private final List<String> errorMessages = new ArrayList<>();

    private final List<String> warningMessages = new ArrayList<>();

    /**
     * We're overriding this method so that we can stop the script from doing the System.exit() if
     * an exception within the script is thrown
     */
    @Override
    public void handleException(String message, Exception e) {
        exception = e;
    }

    /**
     * Generic getter for the exception
     * @return the exception value of this TestDSpaceRunnableHandler
     */
    public Exception getException() {
        return exception;
    }

    @Override
    public void logInfo(String message) {
        super.logInfo(message);
        infoMessages.add(message);
    }

    @Override
    public void logWarning(String message) {
        super.logWarning(message);
        warningMessages.add(message);
    }

    @Override
    public void logError(String message) {
        super.logError(message);
        errorMessages.add(message);
    }

    public List<String> getInfoMessages() {
        return infoMessages;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public List<String> getWarningMessages() {
        return warningMessages;
    }
}
