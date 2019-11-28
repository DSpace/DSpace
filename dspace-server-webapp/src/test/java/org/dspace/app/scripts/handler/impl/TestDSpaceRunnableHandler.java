/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.scripts.handler.impl;

import org.dspace.scripts.handler.impl.CommandLineDSpaceRunnableHandler;

/**
 * This class will be used as a DSpaceRunnableHandler for the Tests so that we can stop the handler
 * from calling System.exit() when a script would throw an exception
 */
public class TestDSpaceRunnableHandler extends CommandLineDSpaceRunnableHandler {

    private Exception exception = null;

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
}