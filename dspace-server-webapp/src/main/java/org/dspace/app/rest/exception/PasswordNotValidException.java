/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.dspace.core.I18nUtil;

/**
 * This class provides an exception to be used when trying to create an EPerson
 * with password that not match regular expression configured in this
 * variable "authentication-password.regex-validation.pattern" in dspace.cfg or during the patch of password.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class PasswordNotValidException extends UnprocessableEntityException implements TranslatableException {

    private static final long serialVersionUID = -4294543847989250566L;

    public static final String MESSAGE_KEY = "org.dspace.app.rest.exception.PasswordNotValidException.message";

    public PasswordNotValidException() {
        super(I18nUtil.getMessage(MESSAGE_KEY));
    }

    public PasswordNotValidException(Throwable cause) {
        super(I18nUtil.getMessage(MESSAGE_KEY), cause);
    }

    public String getMessageKey() {
        return MESSAGE_KEY;
    }

}