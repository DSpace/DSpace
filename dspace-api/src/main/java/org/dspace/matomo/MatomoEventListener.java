/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoEventListener extends AbstractUsageEventListener {

    private static final Logger log = LogManager.getLogger(MatomoEventListener.class);

    private final ConfigurationService configurationService;
    private final List<MatomoUsageEventHandler> matomoUsageEventHandlers;

    public MatomoEventListener(
        @Autowired List<MatomoUsageEventHandler> matomoUsageEventHandlers,
        @Autowired ConfigurationService configurationService
    ) {
        this.matomoUsageEventHandlers = matomoUsageEventHandlers;
        this.configurationService = configurationService;
    }

    @Override
    public void receiveEvent(Event event) {
        if (!(event instanceof UsageEvent usageEvent)) {
            return;
        }

        try {
            if (!matomoEnabled() || matomoUsageEventHandlers == null || matomoUsageEventHandlers.isEmpty()) {
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("Usage event received {}", event.getName());
            }

            this.matomoUsageEventHandlers.forEach(handler -> handler.handleEvent(usageEvent));

        } catch (Exception e) {
            log.error("Failed to add the UsageEvent to Matomo Handlers {} ", usageEvent,  e);
        }
    }

    private boolean matomoEnabled() {
        return this.configurationService.getBooleanProperty("matomo.enabled", false);
    }

}
