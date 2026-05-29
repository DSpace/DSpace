/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

/**
 * Exception thrown when there are issues with ORCID service connections.
 *
 * @author Boychuk Mykhaylo (mykhaylo.boychuk@4science.com)
 */
public class OrcidConnectionException extends Exception {

    private final int statusCode;

    public OrcidConnectionException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public OrcidConnectionException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

}
