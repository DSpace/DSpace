/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.external.exception.ExternalDataException;
import org.dspace.external.exception.ExternalDataRestClientException;
import org.dspace.external.model.WikiImageResource;
import org.springframework.cache.annotation.Cacheable;

/**
 * Wikimedia REST connector to retrieve image attribution and licence metadata
 * for images used in e.g. GND external objects
 */
public class WikimediaRestConnector extends AbstractRestConnector {

    private static final Logger log = LogManager.getLogger();

    public WikimediaRestConnector(String url) {
        super(url);
    }

    /**
     * Get a single Wikimedia image metadata record by image title
     *
     * @param imageTitle the image title eg. MarkTwain.LOC.jpg
     * @return a string containing the JSON record
     * @throws IOException
     */
    @Cacheable(cacheNames = "wikimedia_image.get")
    public String getByImageTitle(String imageTitle) throws ExternalDataException {
        Optional<WikiImageResource> resource;
        // Set up URI for API call
        // Final URL should look like:
        // https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=extmetadata
        // &titles=File:MarkTwain.LOC.jpg
        try {
        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.addParameter("action", "query");
        uriBuilder.addParameter("format", "json");
        uriBuilder.addParameter("prop", "imageinfo");
        uriBuilder.addParameter("iiprop", "extmetadata");
        uriBuilder.addParameter("titles", "File:" + imageTitle);
        URI uri = uriBuilder.build();
            // Get HTTP result as string
            return get(uri.toString());
        } catch(URISyntaxException e) {
            log.error("Error constructing Wikimedia image request URI: {}", e.getMessage());
            throw new ExternalDataRestClientException(e);
        } catch(NullPointerException e) {
            log.error("Error building Wikimedia response from JSON or result stream: {}", e.getMessage());
            throw new ExternalDataRestClientException(e);
        }
    }

}
