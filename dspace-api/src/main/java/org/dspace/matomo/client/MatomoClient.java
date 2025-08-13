/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import java.util.List;

import org.dspace.matomo.model.MatomoRequestDetails;

/**
 * This interface can be used to implement a HTTP client that will connect
 * to the Matomo Tracking api.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public interface MatomoClient {

    /**
     * This method sends tracked resources to Matomo Tracking API
     * @param details
     */
    void sendDetails(MatomoRequestDetails... details);

    /**
     * This method is an overload of the {@code sendDetails} above
     * @param details
     */
    void sendDetails(List<MatomoRequestDetails> details);
}
