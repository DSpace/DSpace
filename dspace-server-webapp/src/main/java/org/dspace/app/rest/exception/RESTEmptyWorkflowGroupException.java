/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import java.text.MessageFormat;

import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EmptyWorkflowGroupException;

/**
 * <p>Extend {@link UnprocessableEntityException} to provide a specific error message
 * in the REST response. The error message is added to the response in
 * {@link DSpaceApiExceptionControllerAdvice#handleCustomUnprocessableEntityException},
 * hence it should not contain sensitive or security-compromising info.</p>
 *
 * <p>Note there is a similarly named error in the DSpace API module.</p>
 *
 * @author Bruno Roemers (bruno.roemers at atmire.com)
 */
public class RESTEmptyWorkflowGroupException extends UnprocessableEntityException implements TranslatableException {

    /**
     * @param formatStr string with placeholders, ideally obtained using {@link I18nUtil}
     * @param cause {@link EmptyWorkflowGroupException}, from which EPerson id and group id are obtained
     * @return message with EPerson id and group id substituted
     */
    private static String formatMessage(String formatStr, EmptyWorkflowGroupException cause) {
        MessageFormat fmt = new MessageFormat(formatStr);
        String[] values = {
            cause.getEPersonId().toString(), // {0} in formatStr
            cause.getGroupId().toString(), // {1} in formatStr
        };
        return fmt.format(values);
    }

    public static final String MESSAGE_KEY = "org.dspace.app.rest.exception.RESTEmptyWorkflowGroupException.message";

    private final EmptyWorkflowGroupException cause;

    public RESTEmptyWorkflowGroupException(EmptyWorkflowGroupException cause) {
        super(formatMessage(
            I18nUtil.getMessage(MESSAGE_KEY), cause
        ), cause);
        this.cause = cause;
    }

    public String getMessageKey() {
        return MESSAGE_KEY;
    }

    public String getLocalizedMessage(Context context) {
        return formatMessage(
            I18nUtil.getMessage(MESSAGE_KEY, context), cause
        );
    }

}
