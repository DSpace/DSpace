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

    /**
     * Enriches the provided MatomoRequestDetails with additional parameters based on the UsageEvent.
     *
     * @param usageEvent The usage event containing information to enrich the request details
     * @param matomoRequestDetails The request details object to be enriched with additional parameters
     * @return The enriched MatomoRequestDetails object
     */
    MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails);

    /**
     * Composes this enricher with another enricher to create a new enricher that applies both in sequence.
     * The provided enricher is applied first, followed by this enricher.
     *
     * @param enricher The enricher to compose with this one
     * @return A new enricher that applies both enrichers in sequence
     */
    default MatomoRequestDetailsEnricher compose(MatomoRequestDetailsEnricher enricher) {
        return (usageEvent, details) -> enrich(usageEvent, enricher.enrich(usageEvent, details));
    }

    /**
     * Creates an empty enricher that returns the input MatomoRequestDetails unchanged.
     *
     * @return An enricher that performs no modifications to the input
     */
    static MatomoRequestDetailsEnricher empty() {
        return ((usageEvent, matomoRequestDetails) -> matomoRequestDetails);
    }
}
