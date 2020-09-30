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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ListDSpaceRunnableHandler extends TestDSpaceRunnableHandler {

    private static final Logger log = LogManager.getLogger(ListDSpaceRunnableHandler.class);

    private final List<String> infoMessages = new ArrayList<>();

    private final List<String> errorMessages = new ArrayList<>();

    private final List<String> waringMessages = new ArrayList<>();

    @Override
    public void handleException(String message, Exception e) {
        super.handleException(message, e);
        if (message != null) {
            System.err.println(message);
            errorMessages.add(message);
        }
        if (e != null) {
            message = ExceptionUtils.getRootCauseMessage(e);
            System.err.println(message);
            errorMessages.add(message);
        }
    }

    @Override
    public void logInfo(String message) {
        System.out.println(message);
        infoMessages.add(message);
    }

    @Override
    public void logWarning(String message) {
        System.out.println(message);
        waringMessages.add(message);
    }

    @Override
    public void logError(String message) {
        System.err.println(message);
        errorMessages.add(message);
    }

    public List<String> getInfoMessages() {
        return infoMessages;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public List<String> getWarningMessages() {
        return waringMessages;
    }

}
