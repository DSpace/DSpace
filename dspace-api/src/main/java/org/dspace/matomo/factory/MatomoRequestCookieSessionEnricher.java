/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import java.util.Locale;

import jakarta.servlet.http.Cookie;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.usage.UsageEvent;

/**
 * This class adds the {@code MATOMO_SESSID} cookie to the {@code MatomoRequestDetails}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestCookieSessionEnricher implements MatomoRequestDetailsEnricher {

    @Override
    public MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails) {
        Cookie[] cookies = usageEvent.getRequest().getCookies();
        if (cookies == null) {
            return matomoRequestDetails;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().toLowerCase(Locale.ROOT).equalsIgnoreCase("MATOMO_SESSID")) {
                return matomoRequestDetails.addCookie("MATOMO_SESSID", cookie.getValue());
            }
        }
        return matomoRequestDetails;
    }
}
