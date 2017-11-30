/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.io.IOException;

/**
 * Exception thrown in case of bad request syntax
 *
 * Example: invalid/missing parameters, ...
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class BadRequestException extends IOException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(Throwable t) {
        super(t);
    }

    public BadRequestException(String message, Throwable t) {
        super(message, t);
    }
}
