package ar.edu.unlp.sedici.xmlui.exception;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.util.location.Location;

public class BadRequestException extends ProcessingException {

    /**
     * Construct a new <code>BadRequestException</code> instance.
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Construct a new <code>BadRequestException</code> that references
     * a parent Exception.
     */
    public BadRequestException(String message, Throwable t) {
        super(message, t);
    }
    
    public BadRequestException(String message, Location location) {
        super(message, location);
    }
    
    public BadRequestException(String message, Throwable t, Location loc) {
        super(message, t, loc);
    }

}
