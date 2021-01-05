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
 * {@link DSpaceApiExceptionControllerAdvice#handleEmptyWorkflowGroupException}.</p>
 *
 * <p>Note there is a similarly named error in the DSpace API module.</p>
 *
 * @author Bruno Roemers (bruno.roemers at atmire.com)
 */
public class RESTEmptyWorkflowGroupException extends UnprocessableEntityException {

    public RESTEmptyWorkflowGroupException(String message, Throwable cause) {
        super(message, cause);
    }

    public RESTEmptyWorkflowGroupException(String message) {
        super(message);
    }

}
