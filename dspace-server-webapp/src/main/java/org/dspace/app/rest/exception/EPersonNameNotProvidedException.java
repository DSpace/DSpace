/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * <p>Extend {@link UnprocessableEntityException} to provide a specific error message
 * in the REST response. The error message is added to the response in
 * {@link DSpaceApiExceptionControllerAdvice#handleCustomUnprocessableEntityException},
 * hence it should not contain sensitive or security-compromising info.</p>
 *
 * @author Bruno Roemers (bruno.roemers at atmire.com)
 */
public class EPersonNameNotProvidedException extends UnprocessableEntityException {

    public static final String message = "The eperson.firstname and eperson.lastname values need to be filled in";

    public EPersonNameNotProvidedException() {
        super(message);
    }

    public EPersonNameNotProvidedException(Throwable cause) {
        super(message, cause);
    }

}
