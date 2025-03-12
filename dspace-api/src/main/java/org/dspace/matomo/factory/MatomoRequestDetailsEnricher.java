/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.usage.UsageEvent;

/**
 * This class encapsulate a functional interface that will be used to enrich the {@code MatomoRequestDetails}
 * with parameters.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@FunctionalInterface
public interface MatomoRequestDetailsEnricher {

    MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails);

    default MatomoRequestDetailsEnricher compose(MatomoRequestDetailsEnricher enricher) {
        return (usageEvent, details) -> enrich(usageEvent, enricher.enrich(usageEvent, details));
    }

    static MatomoRequestDetailsEnricher empty() {
        return ((usageEvent, matomoRequestDetails) -> matomoRequestDetails);
    }

}
