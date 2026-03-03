/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import java.util.List;

import org.dspace.matomo.factory.MatomoRequestDetailsEnricher;
import org.dspace.usage.UsageEvent;

/**
 * This builder can be used to create a proper request using the configured {@code List<MatomoRequestDetailsEnricher>}
 * and the proper {@code siteId}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestDetailsBuilder {

    final String siteId;
    final List<MatomoRequestDetailsEnricher> enrichers;

    /**
     * Constructs a new MatomoRequestDetailsBuilder with the specified enrichers and site ID.
     *
     * @param enrichers List of MatomoRequestDetailsEnricher objects to enrich the request details
     * @param siteId The Matomo site ID to be used for tracking
     */
    MatomoRequestDetailsBuilder(
        List<MatomoRequestDetailsEnricher> enrichers,
        String siteId
    ) {
        this.enrichers = enrichers;
        this.siteId = siteId;
    }

    /**
     * Builds a MatomoRequestDetails object for the given usage event.
     * This method initializes basic tracking parameters and applies all configured enrichers.
     *
     * @param usageEvent The usage event to build request details for
     * @return MatomoRequestDetails object containing all tracking parameters
     */
    public MatomoRequestDetails build(UsageEvent usageEvent) {
        MatomoRequestDetails requestDetails = new MatomoRequestDetails();

        requestDetails.addParameter("idsite", siteId)
                      .addParameter("rec", "1")
                      .addParameter("cookie", "1")
                      .addParameter("apiv", "1");

        return enrichers.stream()
                        .reduce(MatomoRequestDetailsEnricher.empty(), MatomoRequestDetailsEnricher::compose)
                        .enrich(usageEvent, requestDetails);
    }


}