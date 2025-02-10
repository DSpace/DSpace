/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import org.dspace.usage.UsageEvent;

/**
 * This interface represents an Handler to track {@code UsageEvent} towards Matomo.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface MatomoUsageEventHandler {
    /**
     * This method handles the {@code UsageEvent} in order to send that request
     * to Matomo
     *
     * @param usageEvent
     */
    void handleEvent(UsageEvent usageEvent);

    /**
     * Sends all the pending request to the Matomo tracking endpoint.
     */
    void sendEvents();
}
