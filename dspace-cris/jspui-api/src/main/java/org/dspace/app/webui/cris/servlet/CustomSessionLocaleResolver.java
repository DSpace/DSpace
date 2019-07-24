/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.core.Config;

import org.dspace.app.webui.util.UIUtil;
import org.springframework.web.servlet.i18n.AbstractLocaleResolver;

/**
 * Extends AbstractLocaleResolver to manage only the Config.FMT_LOCALE used by DSpace application
 *
 */
public class CustomSessionLocaleResolver extends AbstractLocaleResolver
{
    public Locale resolveLocale(HttpServletRequest request)
    {
        return UIUtil.getSessionLocale(request);
    }

    public void setLocale(HttpServletRequest request,
            HttpServletResponse response, Locale locale)
    {
        Config.set(request.getSession(), Config.FMT_LOCALE, locale);
    }

}
