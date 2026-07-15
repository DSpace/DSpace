/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * This class represents a custom {@code JSONDeserializer} that converts the JSON into {@code MatomoResponse}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoResponseReader {

    private static final Logger log = LogManager.getLogger(MatomoResponseReader.class);
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    /**
     * Converts a String response into a {@code MatomoResponse} object
     * @param response
     * @return
     */
    MatomoResponse fromJSON(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(response, MatomoResponse.class);
        } catch (JacksonException e) {
            log.error("Cannot convert the Matomo response: {} properly!", response, e);
        }
        return null;
    }

}
