/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * This exception is thrown when any exception happens
 * while extracting Metadata for patch operation
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class ExtractMetadataStepException extends RuntimeException {

    public ExtractMetadataStepException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtractMetadataStepException(String message) {
        super(message);
    }

}
