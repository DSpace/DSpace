/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.Enumeration;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the base class for any Rest Repository. It provides utility method to
 * access the DSpaceContext
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class AbstractDSpaceRestRepository {

    @Autowired
    protected Utils utils;

    @Autowired
    protected ConverterService converter;

    protected RequestService requestService = new DSpace().getRequestService();

    protected Context obtainContext() {
        Context context = null;
        Request currentRequest = requestService.getCurrentRequest();
        context = ContextUtil.obtainContext(currentRequest.getServletRequest());
        Locale currentLocale = getLocale(context, currentRequest);
        context.setCurrentLocale(currentLocale);
        return context;
    }

    public RequestService getRequestService() {
        return requestService;
    }

    private Locale getLocale(Context context, Request request) {
        Locale userLocale = null;
        Locale supportedLocale = null;

        // Locales requested from client
        String locale = request.getHttpServletRequest().getHeader("Accept-Language");
        if (StringUtils.isNotBlank(locale)) {
            Enumeration<Locale> locales = request.getHttpServletRequest().getLocales();
            if (locales != null) {
                while (locales.hasMoreElements()) {
                    Locale current = locales.nextElement();
                    if (I18nUtil.isSupportedLocale(current)) {
                        userLocale = current;
                        break;
                    }
                }
            }
        }
        if (userLocale == null && context.getCurrentUser() != null) {
            String userLanguage = context.getCurrentUser().getLanguage();
            if (userLanguage != null) {
                userLocale = new Locale(userLanguage);
            }
        }
        if (userLocale == null) {
            return I18nUtil.getDefaultLocale();
        }
        supportedLocale = I18nUtil.getSupportedLocale(userLocale);
        return supportedLocale;
    }

}
