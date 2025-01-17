/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.external.exception.ExternalDataException;
import org.springframework.cache.annotation.Cacheable;

/**
 *
 * HTTP REST API connector for LOBID GND API
 * The HTTP client used here is mockable - when the create method is called, if httpClient is not null
 * then the injected client will be used, otherwise a new CloseableHttpClient will be built and used
 *
 * <a href="https://lobid.org/gnd/api">https://lobid.org/gnd/api</a> has documentation for the API itself
 *
 * @author Kim Shepherd
 */
public class LobidGNDRestConnector extends AbstractRestConnector {

    /**
     * Logger
     */
    private final Logger log = LogManager.getLogger();

    /**
     * Constructor, accepting a URL
     * @param url base URL of API
     */
    public LobidGNDRestConnector(String url) {
        super(url);
    }

    /**
     * Get a single LOBID GND record by ID
     *
     * @param id the GND ID
     * @return a string containing the JSON record
     * @throws IOException
     */
    @Cacheable(cacheNames = "lobid.getById")
    public String getById(String id) throws ExternalDataException {
        // Format identifier into https://lobid.org/12345.json format
        String requestUrl = GNDUtils.formatObjectURI(id);
        log.debug("Using request URL={}", requestUrl);
        return get(requestUrl);
    }

    /**
     * Search LOBID API for records, given a simple query
     *
     * @param query query string
     * @param from start record (in paginated results)
     * @param size page size
     * @param filter filter to apply to search query (taken from 'hint' in the data provider)
     * @param sort sort order of results
     * @param format result format
     * @return A string containing result contents for parsing
     * @throws IOException
     */
    @Cacheable(cacheNames = "lobid.search")
    public String search(String query, int from, int size, String filter, String sort, String format)
            throws ExternalDataException {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("Query string must not be null or empty");
        }
        // https://lobid.org/gnd/search?q=london&from=0&size=2&format=json
        StringBuilder sb = new StringBuilder();
        sb.append(this.url);
        sb.append("search?q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        if (from > 0) {
            sb.append("&from=").append(from);
        }
        if (size > 0) {
            sb.append("&size=").append(size);
        }
        if (StringUtils.isNotEmpty(filter)) {
            sb.append("&filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
        }
        if (StringUtils.isNotEmpty(sort)) {
            sb.append("&sort=").append(sort);
        }
        if (StringUtils.isEmpty(format)) {
            format = "json";
        }
        sb.append("&format=").append(format);
        String requestUrl = sb.toString();
        log.debug("Making search request to LOBID API, url={}", requestUrl);
        // Make closeable HTTP request
        return get(requestUrl);
    }

}
