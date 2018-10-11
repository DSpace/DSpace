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
 * Wrapper of the dspace DCInputsReaderException that makes it unchecked.
 *
 * @author Michael Spalti
 */
public class RESTDCInputsReaderException extends RuntimeException {

    public RESTDCInputsReaderException(String message) {
        super(message);
    }

    public RESTDCInputsReaderException(SubmissionConfigReaderException ex) {
        super(ex);
    }

    public RESTDCInputsReaderException(String message, Throwable io) {
        super(message, io);
    }
}

