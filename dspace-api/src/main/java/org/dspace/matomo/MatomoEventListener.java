/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This EventListener handles {@code UsageEvent}s and send them to all the {@code List<MatomoUsageEventHandler>}
 * configured
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoEventListener extends AbstractUsageEventListener {

    private static final Logger log = LogManager.getLogger(MatomoEventListener.class);

    private final ConfigurationService configurationService;
    private final BitstreamService bitstreamService;
    private final List<MatomoUsageEventHandler> matomoUsageEventHandlers;

    public MatomoEventListener(
        @Autowired List<MatomoUsageEventHandler> matomoUsageEventHandlers,
        @Autowired ConfigurationService configurationService,
        @Autowired BitstreamService bitstreamService
    ) {
        this.matomoUsageEventHandlers = matomoUsageEventHandlers;
        this.configurationService = configurationService;
        this.bitstreamService = bitstreamService;
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

            if (!isContentBitstream(usageEvent)) {
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

    /**
     * Verifies if the usage event is a content bitstream view event, by checking if:
     * <ul>
     *   <li>the usage event is a view event</li>
     *   <li>the object of the usage event is a bitstream</li>
     *   <li>the bitstream belongs to one of the configured bundles (fallback: ORIGINAL bundle)</li>
     * </ul>
     */
    private boolean isContentBitstream(UsageEvent usageEvent) {
        // check if event is a VIEW event and object is a Bitstream
        if (!isBitstreamView(usageEvent)) {
            return false;
        }
        // check if bitstream belongs to a configured bundle
        Set<String> allowedBundles = getTrackedBundles();
        if (allowedBundles.contains("none")) {
            // events for bitstream views were turned off in config
            return false;
        }
        return isInBundle(((Bitstream) usageEvent.getObject()), allowedBundles);
    }

    private Set<String> getTrackedBundles() {
        return Set.of(
            configurationService.getArrayProperty(
                "matomo.track.bundles",
                new String[] {Constants.CONTENT_BUNDLE_NAME}
            )
        );
    }

    protected boolean isInBundle(Bitstream bitstream, Set<String> allowedBundles) {
        try {
            return this.bitstreamService.isInBundle(bitstream, allowedBundles);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean isBitstreamView(UsageEvent usageEvent) {
        return usageEvent.getAction() == UsageEvent.Action.VIEW
               && usageEvent.getObject().getType() == Constants.BITSTREAM;
    }

}
