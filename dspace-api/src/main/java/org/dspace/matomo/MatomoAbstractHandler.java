/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import org.dspace.matomo.client.MatomoClient;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.matomo.model.MatomoRequestDetailsBuilder;
import org.dspace.usage.UsageEvent;

/**
 * This class represents an abstract class that will be used to handle the {@code UsageEvent}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class MatomoAbstractHandler implements MatomoUsageEventHandler {
    protected final MatomoClient matomoClient;
    protected final MatomoRequestDetailsBuilder builder;

    public MatomoAbstractHandler(MatomoClient matomoClient, MatomoRequestDetailsBuilder builder) {
        this.matomoClient = matomoClient;
        this.builder = builder;
    }

    /**
     * Converts a DSpace usage event into a Matomo request details object.
     *
     * @param usageEvent The DSpace usage event to convert
     * @return MatomoRequestDetails object containing the converted event details
     */
    protected MatomoRequestDetails toDetails(UsageEvent usageEvent) {
        return this.builder.build(usageEvent);
    }
    @Override
    public abstract void handleEvent(UsageEvent usageEvent);

    @Override
    public abstract void sendEvents();
}
