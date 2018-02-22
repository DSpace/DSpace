/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

public class ExitException extends SecurityException {
    private final int status;

    public ExitException(int status) {
        super("Process exited with code: " + status + ", you can ignore this exception");
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}