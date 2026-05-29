/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This record represents the response of the Matomo Tracking API.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
record MatomoResponse(String status, int tracked, int invalid, @JsonProperty("invalid_indices") int[] invalidIndices) {

    public static final String SUCCESS = "success";

}
