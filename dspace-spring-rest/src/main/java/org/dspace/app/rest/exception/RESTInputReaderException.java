/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.dspace.app.util.SubmissionConfigReaderException;

/**
 * Wrapper for dspace input reader exceptions that makes them unchecked
 * (e.g. DCInputsReaderException or SubmissionConfigReaderException).
 *
 * @author Michael Spalti
 */
public class RESTInputReaderException extends RuntimeException {

    public RESTInputReaderException(String message) {
        super(message);
    }

    public RESTInputReaderException(SubmissionConfigReaderException ex) {
        super(ex);
    }

    public RESTInputReaderException(String message, Throwable io) {
        super(message, io);
    }
}
