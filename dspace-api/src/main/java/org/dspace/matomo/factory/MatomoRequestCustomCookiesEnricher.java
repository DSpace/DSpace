/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import java.util.Set;

import jakarta.servlet.http.Cookie;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.usage.UsageEvent;

/**
 * This class extends the {@code MatomoRequestDetailsEnricher} interface and provides a concrete implementation
 * to enrich the {@code MatomoRequestDetails} with custom cookies from the {@code UsageEvent}.
 * <br/
 * The format of each cookie is: _pk_ref.1.1fff, so we need to extract the base name and check that is configured.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestCustomCookiesEnricher implements MatomoRequestDetailsEnricher {

    // "_pk_ref", "_pk_hsr", "_pk_ses"
    private final Set<String> customCookies;

    public MatomoRequestCustomCookiesEnricher(String customCookies) {
        this.customCookies = Set.of(customCookies.split(","));
    }

    /**
     * Enriches the {@code MatomoRequestDetails} with custom cookies from the {@code UsageEvent}.
     *
     * @param usageEvent           The {@code UsageEvent} containing the request.
     * @param matomoRequestDetails The {@code MatomoRequestDetails} to be enriched.
     * @return The enriched {@code MatomoRequestDetails}.
     */
    @Override
    public MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails) {
        Cookie[] cookies = usageEvent.getRequest().getCookies();
        if (cookies == null) {
            return matomoRequestDetails;
        }
        for (Cookie cookie : cookies) {
            String cookieName = cookie.getName();
            String baseName = null;
            if (cookieName != null && cookieName.contains(".")) {
                baseName = cookieName.substring(0, cookieName.indexOf("."));
            }
            if (baseName != null && customCookies.contains(baseName)) {
                matomoRequestDetails.addCookie(cookieName, cookie.getValue());
            }
        }
        return matomoRequestDetails;
    }
}
