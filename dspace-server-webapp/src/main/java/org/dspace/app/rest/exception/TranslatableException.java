/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.exception;

import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

/**
 * <p>Implement TranslatableException to make Exceptions or RuntimeExceptions translatable.</p>
 *
 * <p>In most cases, only {@link #getMessageKey()} should be implemented;
 * {@link #getMessage()} and {@link #getLocalizedMessage()} are already provided by {@link Throwable}
 * and the default implementation of {@link #getLocalizedMessage(Context)} is usually sufficient.</p>
 *
 * <p>A locale-aware message can be obtained by calling {@link #getLocalizedMessage(Context)}.</p>
 *
 * @author Bruno Roemers (bruno.roemers at atmire.com)
 */
public interface TranslatableException {

    /**
     * @return message key (used for lookup with {@link I18nUtil})
     */
    String getMessageKey();

    /**
     * Already implemented by {@link Throwable}.
     * @return message for default locale
     */
    String getMessage();

    /**
     * Already implemented by {@link Throwable}.
     * @return message for default locale
     */
    String getLocalizedMessage();

    /**
     * @param context current DSpace context (used to infer current locale)
     * @return message for current locale (or default locale if current locale did not yield a result)
     */
    default String getLocalizedMessage(Context context) {
        return I18nUtil.getMessage(getMessageKey(), context);
    }

}
