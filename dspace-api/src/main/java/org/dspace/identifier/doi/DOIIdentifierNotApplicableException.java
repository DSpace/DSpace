/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import org.dspace.identifier.IdentifierNotApplicableException;

/**
 *
 * Thrown when an identifier should not be applied to an item, eg. when it has been filtered by an item filter
 *
 *
 * @author Kim Shepherd
 */
public class DOIIdentifierNotApplicableException extends IdentifierNotApplicableException {

    public DOIIdentifierNotApplicableException() {
        super();
    }

    public DOIIdentifierNotApplicableException(String message) {
        super(message);
    }

    public DOIIdentifierNotApplicableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DOIIdentifierNotApplicableException(Throwable cause) {
        super(cause);
    }
}
