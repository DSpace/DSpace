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

/**
 * <p>Extend {@link UnprocessableEntityException} to provide a specific error message
 * in the REST response. The error message is added to the response in
 * {@link DSpaceApiExceptionControllerAdvice#handleCustomUnprocessableEntityException},
 * hence it should not contain sensitive or security-compromising info.</p>
 *
 * @author Jens Vannerum (jens.vannerum@atmire.com)
 */
public class RESTBitstreamNotFoundException extends UnprocessableEntityException implements TranslatableException {

    public static String uuid;

    /**
     * @param formatStr string with placeholders, ideally obtained using {@link I18nUtil}
     * @return message with bitstream id substituted
     */
    private static String formatMessage(String formatStr) {
        MessageFormat fmt = new MessageFormat(formatStr);
        return fmt.format(new String[]{uuid});
    }

    public static final String MESSAGE_KEY = "org.dspace.app.rest.exception.RESTBitstreamNotFoundException.message";

    public RESTBitstreamNotFoundException(String uuid) {
        super(formatMessage(I18nUtil.getMessage(MESSAGE_KEY)));
        RESTBitstreamNotFoundException.uuid = uuid;
    }

    public String getMessageKey() {
        return MESSAGE_KEY;
    }

    public String getLocalizedMessage(Context context) {
        return formatMessage(I18nUtil.getMessage(MESSAGE_KEY, context));
    }

}
