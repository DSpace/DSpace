/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

/**
 * This class provides an exception to be used when trying to register a new EPerson
 * and Captcha validations failed.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class InvalidReCaptchaException extends RuntimeException {

    private static final long serialVersionUID = -5328794674744121744L;

    public InvalidReCaptchaException(String message) {
        super(message);
    }

    public InvalidReCaptchaException(String message, Exception cause) {
        super(message, cause);
    }

}