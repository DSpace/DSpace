/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

/**
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class IdentifierException extends Exception{

    public IdentifierException() {
        super();
    }

    public IdentifierException(String message) {
        super(message);
    }

    public IdentifierException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentifierException(Throwable cause) {
        super(cause);
    }
}
