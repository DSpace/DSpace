/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.matomo.model.MatomoRequestDetailsListConverter;

/**
 * Request that will be sent to Matomo tracking endpoint
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
record MatomoBulkRequest(
    @JsonProperty(value = "token_auth", required = true) String token,
    @JsonSerialize(using = MatomoRequestDetailsListConverter.class) List<MatomoRequestDetails> requests) {

}