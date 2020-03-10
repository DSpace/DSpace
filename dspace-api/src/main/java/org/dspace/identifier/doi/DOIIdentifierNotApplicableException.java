package org.dspace.identifier.doi;

/**
 *
 * Thrown when an identifier should not be applied to an item, eg. when it has been filtered by an item filter
 *
 *
 * @author Kim Shepherd
 */
public class DOIIdentifierNotApplicableException extends DOIIdentifierException {

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
