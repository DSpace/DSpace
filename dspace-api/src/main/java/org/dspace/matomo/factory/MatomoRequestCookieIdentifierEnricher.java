/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.servlet.http.Cookie;
import org.apache.commons.lang3.StringUtils;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.usage.UsageEvent;

/**
 * Enricher that extracts any {@code _pk_id} cookie sent inside the request
 * to track the same {@code id} used to track interaction on the angular side. <br>
 * The cookie will have a similar format: {@code _pk_id.1.1fff=3225aebdb98b13f9.1740076196.} <br>
 * Where only the first 16 hexadecimal characters represents the id for Matomo.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestCookieIdentifierEnricher implements MatomoRequestDetailsEnricher  {

    static final String _PK_ID_NAME = "_pk_id";
    static final Pattern _pk_id = Pattern.compile("(^([a-f]|[0-9]){16})");

    @Override
    public MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails) {
        Cookie[] cookies = usageEvent.getRequest().getCookies();
        if (cookies == null || cookies.length == 0) {
            return matomoRequestDetails;
        }
        return getCookie(cookies).filter(StringUtils::isNotEmpty)
                                 .map(id -> matomoRequestDetails.addParameter("_id", id))
                                 .orElse(matomoRequestDetails);
    }

    public static boolean hasCookie(Cookie[] cookies) {
        return cookies != null && getCookie(cookies).isPresent();
    }

    public static Optional<String> getCookie(Cookie[] cookies) {
        return Stream.of(cookies)
                     .filter(cookie -> cookie.getName().startsWith(_PK_ID_NAME))
                     .map(cookie -> _pk_id.matcher(cookie.getValue()))
                     .filter(Matcher::find)
                     .findFirst()
                     .map(Matcher::group);
    }

}
