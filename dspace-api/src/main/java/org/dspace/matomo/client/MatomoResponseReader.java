/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class represents a custom {@code JSONDeserializer} that converts the JSON into {@code MatomoResponse}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoResponseReader {

    private static final Logger log = LogManager.getLogger(MatomoResponseReader.class);
    ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    /**
     * Converts a String response into a {@code MatomoResponse} object
     * @param response
     * @return
     */
    MatomoResponse fromJSON(String response) {
        if (response == null) {
            return null;
        }
        try {
            return objectMapper.readValue(response, MatomoResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Cannot convert the Matomo response: {} properly!", response, e);
        }
        return null;
    }

}
