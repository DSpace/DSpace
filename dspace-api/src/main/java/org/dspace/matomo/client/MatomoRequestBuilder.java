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
 * This class is a {@code JSONSerializer} that will convert a {@code MatomoBulkRequest} into a proper JSON
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestBuilder {

    private static final Logger log = LogManager.getLogger(MatomoRequestBuilder.class);
    ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    /**
     * This method converts a {@code MatomoBulkRequest} request into a JSON
     * @param request a {@code MatomoBulkRequest} object
     * @return String
     */
    String buildJSON(MatomoBulkRequest request) {
        if (request == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Cannot convert the Matomo request properly!", e);
        }
        return null;
    }

}
