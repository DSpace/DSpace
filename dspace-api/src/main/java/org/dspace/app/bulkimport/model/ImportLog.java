/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

public final class ImportLog {

    private final LogLevel logLevel;
    private final String message;

    public ImportLog(LogLevel logLevel, String message) {
        super();
        this.logLevel = logLevel;
        this.message = message;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public String getMessage() {
        return message;
    }

    public static enum LogLevel {
        INFO,
        ERROR,
        WARNING
    }

    @Override
    public String toString() {
        return this.logLevel + " - " + this.message;
    }

}
