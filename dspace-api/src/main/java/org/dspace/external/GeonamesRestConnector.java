/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.external.exception.ExternalDataException;
import org.springframework.cache.annotation.Cacheable;

/**
 *
 * HTTP REST API connector for Geonames web API
 * The HTTP client used here is mockable - when the create method is called, if httpClient is not null
 * then the injected client will be used, otherwise a new CloseableHttpClient will be built and used
 *
 * @author Kim Shepherd
 */
public class GeonamesRestConnector extends AbstractRestConnector {
    /**
     * Geonames API username
     */
    private String username;

    /**
     * Logger
     */
    private final Logger log = LogManager.getLogger();

    /**
     * Constructor, accepting a URL
     * @param url base URL of API
     */
    public GeonamesRestConnector(String url, String username) {
        this.url = url;
        this.username = username;
    }

    /**
     * Get a single Geonames record by ID
     *
     * @param id the GND ID
     * @return a string containing the JSON record
     * @throws IOException
     */
    @Cacheable(cacheNames = "geonames.get")
    public String getById(String id) throws ExternalDataException {
        // Format identifier into http://api.geonames.org/get?geonameId=6547539&username=<username> format
        String requestUrl = url + "?geonameId=" + id + "&username=" + username;
        log.debug("Using request URL={}", requestUrl);
        return get(requestUrl);
    }
}
