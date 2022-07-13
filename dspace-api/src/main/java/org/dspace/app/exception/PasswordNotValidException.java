/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.exception;

/**
 * This class provides an exception to be used when trying to create an EPerson
 * with password that not match regular expression configured in this
 * variable "validate-password-reg-expression" in dspace.cfg or during the patch of password.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class PasswordNotValidException extends RuntimeException {

    private static final long serialVersionUID = -4294543847989250566L;

    public PasswordNotValidException() {}

    public PasswordNotValidException(String message) {
        super(message);
    }

}