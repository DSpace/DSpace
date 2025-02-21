/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.usage.UsageEvent;

/**
 * This class adds the {@code country} parameter to the {@code MatomoRequestDetails}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestCountryEnricher implements MatomoRequestDetailsEnricher {

    private static final Logger log = LogManager.getLogger(MatomoRequestCountryEnricher.class);

    @Override
    public MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails) {
        return matomoRequestDetails.addParameter("country", getCountry(usageEvent.getRequest()));
    }

    private String getCountry(HttpServletRequest request) {
        String country = "";
        if (request != null) {
            try {
                Locale locale = request.getLocale();
                if (locale != null) {
                    country = locale.getCountry().toLowerCase();
                }
            } catch (Exception e) {
                log.error("Cannot get locale of request!", e);
            }
        }
        return country;
    }
}
