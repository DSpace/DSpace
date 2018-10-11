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
 * Wrapper of the dspace SubmissionConfigReaderException that makes it unchecked.
 *
 * @author Michael Spalti
 */
public class RESTSubmissionConfigReaderException extends RuntimeException {

    public RESTSubmissionConfigReaderException(String message) {
        super(message);
    }

    public RESTSubmissionConfigReaderException(SubmissionConfigReaderException ex) {
        super(ex);
    }

    public RESTSubmissionConfigReaderException(String message, Throwable io) {
        super(message, io);
    }
}
