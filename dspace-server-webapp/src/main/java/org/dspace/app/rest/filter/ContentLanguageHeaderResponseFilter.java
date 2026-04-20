/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.filter;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.core.I18nUtil;
import org.springframework.stereotype.Component;

/**
 * This filter assures that when the dspace instance supports multiple languages
 * they are noted in the Content-Language Header of the response. Where
 * appropriate the single endpoint can set the Content-Language header directly
 * to note that the response is specific for a language
 * 
 * @author Mykhaylo Boychuk (at 4science.it)
 */
@Component
public class ContentLanguageHeaderResponseFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        Locale[] locales = I18nUtil.getSupportedLocales();
        StringBuilder locsStr = new StringBuilder();
        for (Locale locale : locales) {
            if (locsStr.length() > 0) {
                locsStr.append(",");
            }
            locsStr.append(locale.getLanguage());
        }
        httpServletResponse.setHeader("Content-Language", locsStr.toString());
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
