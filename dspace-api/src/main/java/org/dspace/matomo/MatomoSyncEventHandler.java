/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.matomo.client.MatomoClient;
import org.dspace.matomo.model.MatomoRequestDetailsBuilder;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class represents a sync event handler that will send details one by one using the {@code MatomoClient}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoSyncEventHandler extends MatomoAbstractHandler {

    private static final Logger log = LogManager.getLogger(MatomoSyncEventHandler.class);

    public MatomoSyncEventHandler(
        @Autowired MatomoClient matomoClient,
        @Autowired MatomoRequestDetailsBuilder builder
    ) {
        super(matomoClient, builder);
    }

    @Override
    public void handleEvent(UsageEvent usageEvent) {
        if (usageEvent == null) {
            log.error("UsageEvent is null");
            return;
        }
        matomoClient.sendDetails(toDetails(usageEvent));
    }

    @Override
    public void sendEvents() {
        log.warn("Events are sent synchronously, you don't need to use this method!");
    }
}
