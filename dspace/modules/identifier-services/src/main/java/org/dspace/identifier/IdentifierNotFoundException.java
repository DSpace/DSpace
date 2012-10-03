package org.dspace.identifier;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/1/11
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class IdentifierNotFoundException extends IdentifierException {

    public IdentifierNotFoundException() {
        super();
    }

    public IdentifierNotFoundException(String message) {
        super(message);
    }

    public IdentifierNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentifierNotFoundException(Throwable cause) {
        super(cause);
    }
}
