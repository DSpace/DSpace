package org.dspace.identifier;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/1/11
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class IdentifierNotResolvableException extends IdentifierException {

    public IdentifierNotResolvableException() {
        super();
    }

    public IdentifierNotResolvableException(String message) {
        super(message);
    }

    public IdentifierNotResolvableException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentifierNotResolvableException(Throwable cause) {
        super(cause);
    }
}
