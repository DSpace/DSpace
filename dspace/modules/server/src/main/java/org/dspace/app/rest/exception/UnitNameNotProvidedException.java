package org.dspace.app.rest.exception;

import org.dspace.core.I18nUtil;

/**
 * <p>Extend {@link UnprocessableEntityException} to provide a specific error message
 * in the REST response. The error message is added to the response in
 * {@link DSpaceApiExceptionControllerAdvice#handleCustomUnprocessableEntityException},
 * hence it should not contain sensitive or security-compromising info.</p>
 */
public class UnitNameNotProvidedException extends UnprocessableEntityException implements TranslatableException {
    public static final String MESSAGE_KEY = "org.dspace.app.rest.exception.UnitNameNotProvidedException.message";

    /**
     * Default constructor
     */
    public UnitNameNotProvidedException() {
        super(I18nUtil.getMessage(MESSAGE_KEY));
    }

    /**
     * Constructs instance with given Throwable
     * @param cause a Throwable indicating the cause of the exception.
     */
    public UnitNameNotProvidedException(Throwable cause) {
        super(I18nUtil.getMessage(MESSAGE_KEY), cause);
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
