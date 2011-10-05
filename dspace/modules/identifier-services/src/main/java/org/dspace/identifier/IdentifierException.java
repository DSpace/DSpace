package org.dspace.identifier;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 24-mrt-2011
 * Time: 15:05:05
 *
 * An exception used by the identifier framework
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
