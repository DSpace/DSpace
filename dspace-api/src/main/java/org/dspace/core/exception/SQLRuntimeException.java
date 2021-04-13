/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core.exception;

import java.sql.SQLException;

/**
 * An runtime exception that provides information on a database access error or
 * other errors.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class SQLRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SQLRuntimeException(String message, SQLException cause) {
        super(message, cause);
    }

    public SQLRuntimeException(String message) {
        super(message);
    }

    public SQLRuntimeException(SQLException cause) {
        super(cause);
    }

}
