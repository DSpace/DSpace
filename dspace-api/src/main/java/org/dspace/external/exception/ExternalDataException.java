/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.exception;

/**
 * General exception for throwable errors encountered in the External Data framework
 *
 * @author Kim Shepherd
 */
public class ExternalDataException extends Exception {

    public ExternalDataException() {
        super();
    }

    public ExternalDataException(Throwable throwable) {
        super(throwable);
    }

    public ExternalDataException(String message) {
        super(message);
    }

}
