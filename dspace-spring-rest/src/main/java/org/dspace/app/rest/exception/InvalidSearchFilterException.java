package org.dspace.app.rest.exception;

import java.sql.SQLException;

/**
 * Created by tom on 19/09/2017.
 */
public class InvalidSearchFilterException extends InvalidRequestException {
    public InvalidSearchFilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
