/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.dspace.app.sherpa.v2.SHERPAPublisherResponse;
import org.dspace.app.sherpa.v2.SHERPAResponse;

/**
 * Mock implementation for SHERPA API service (used by SHERPA submit service to check
 * journal policies).
 * This class will return mock SHERPA responses so they can be parsed and turned
 * into external data objects downstream.
 *
 * @author Kim Shepherd
 */
public class MockSHERPAService extends SHERPAService {

    /**
     * Simple overridden performRequest so that we do attempt to build the URI but rather than make
     * an actual HTTP call, return parsed SHERPAResponse for The Lancet based on known-good JSON stored with our
     * test resources.
     * If URI creation, parsing, or IO fails along the way, a SHERPAResponse with an error message set will be
     * returned.
     * @param value a journal / publication name, or ID, etc.
     * @return  SHERPAResponse
     */
    @Override
    public SHERPAResponse performRequest(String type, String field, String predicate, String value,
                                         int start, int limit) {
        try {
            String endpoint = configurationService.getProperty("sherpa.romeo.url",
                "https://v2.sherpa.ac.uk/cgi/retrieve");
            String apiKey = configurationService.getProperty("sherpa.romeo.apikey");

            // Rather than search, we will simply attempt to build the URI using the real pepare method
            // so that any errors there are caught, and will return a valid response for The Lancet
            InputStream content = null;
            try {
                // Prepare the URI - this will not be used but should be evaluated
                // in case a syntax exception is thrown
                URI uri = prepareQuery(value, endpoint, apiKey);
                if (uri == null) {
                    return new SHERPAResponse("Error building URI");
                }

                // Get mock JSON
                // if a file with the name contained in the value does not exist, returns thelancet.json
                content = getContent(value.concat(".json"));
                if (Objects.isNull(content)) {
                    content = getContent("thelancet.json");
                }

                // Parse JSON input stream and return response for later evaluation
                return new SHERPAResponse(content, SHERPAResponse.SHERPAFormat.JSON);

            } catch (URISyntaxException e) {
                // This object will be marked as having an error for later evaluation
                return new SHERPAResponse(e.getMessage());
            } finally {
                // Close input stream
                if (content != null) {
                    content.close();
                }
            }
        } catch (IOException e) {
            // This object will be marked as having an error for later evaluation
            return new SHERPAResponse(e.getMessage());
        }
    }

    private InputStream getContent(String fileName) {
        return getClass().getResourceAsStream(fileName);
    }

    /**
     * Simple overridden performPublisherRequest so that we do attempt to build the URI but rather than make
     * an actual HTTP call, return parsed SHERPAPublisherResponse for PLOS based on known-good JSON stored with our
     * test resources.
     * If URI creation, parsing, or IO fails along the way, a SHERPAPublisherResponse with an error message set will be
     * returned.
     * @param value a journal / publication name, or ID, etc.
     * @return  SHERPAResponse
     */
    @Override
    public SHERPAPublisherResponse performPublisherRequest(String type, String field, String predicate, String value,
                                                           int start, int limit) {
        try {
            String endpoint = configurationService.getProperty("sherpa.romeo.url",
                "https://v2.sherpa.ac.uk/cgi/retrieve");
            String apiKey = configurationService.getProperty("sherpa.romeo.apikey");

            // Rather than search, we will simply attempt to build the URI using the real pepare method
            // so that any errors there are caught, and will return a valid response for The Lancet
            InputStream content = null;
            try {
                // Prepare the URI - this will not be used but should be evaluated
                // in case a syntax exception is thrown
                URI unuseduri = prepareQuery(value, endpoint, apiKey);

                // Get mock JSON - in this case, a known good result for PLOS
                content = getClass().getResourceAsStream("plos.json");

                // Parse JSON input stream and return response for later evaluation
                return new SHERPAPublisherResponse(content, SHERPAPublisherResponse.SHERPAFormat.JSON);

            } catch (URISyntaxException e) {
                // This object will be marked as having an error for later evaluation
                return new SHERPAPublisherResponse(e.getMessage());
            } finally {
                // Close input stream
                if (content != null) {
                    content.close();
                }
            }
        } catch (IOException e) {
            // This object will be marked as having an error for later evaluation
            return new SHERPAPublisherResponse(e.getMessage());
        }
    }

}
