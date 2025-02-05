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
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestDetailsBuilder {

    final String siteId;
    final List<MatomoRequestDetailsEnricher> enrichers;

    MatomoRequestDetailsBuilder(
        List<MatomoRequestDetailsEnricher> enrichers,
        String siteId
    ) {
        this.enrichers = enrichers;
        this.siteId = siteId;
    }

    public MatomoRequestDetails build(UsageEvent usageEvent) {
        MatomoRequestDetails requestDetails = new MatomoRequestDetails();

        requestDetails.addParameter("idsite", siteId)
                      .addParameter("rec", "1")
                      .addParameter("apiv", "1");

        return enrichers.stream()
                        .reduce(MatomoRequestDetailsEnricher.empty(), MatomoRequestDetailsEnricher::compose)
                        .enrich(usageEvent, requestDetails);
    }


}
