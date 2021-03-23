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
 * Thrown when an identifier should not be applied to an item, eg. when it has been filtered by an item filter
 *
 *
 * @author Kim Shepherd
 */
public class IdentifierNotApplicableException extends IdentifierException {

    public IdentifierNotApplicableException() {
        super();
    }

    public IdentifierNotApplicableException(String message) {
        super(message);
    }

    public IdentifierNotApplicableException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentifierNotApplicableException(Throwable cause) {
        super(cause);
    }
}
