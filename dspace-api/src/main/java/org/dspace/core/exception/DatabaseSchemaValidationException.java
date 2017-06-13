/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core.exception;

/**
 * Runtime exception that indicates that there is a database schema validation problem.
 */
public class DatabaseSchemaValidationException extends RuntimeException {

    public DatabaseSchemaValidationException(final String message) {
        super(message);
    }
}
