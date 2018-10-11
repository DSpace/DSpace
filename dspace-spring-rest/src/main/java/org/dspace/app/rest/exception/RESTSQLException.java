/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.springframework.dao.DataAccessException;

/**
 * Wrapper of the SQLException that extends Spring DataAccessException.
 *
 * @author Michael Spalti
 */
public class RESTSQLException extends DataAccessException {

    public RESTSQLException(String msg) {
        super(msg);
    }

    public RESTSQLException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
