/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Model class for the SHERPAv2 API (JSON) response for a publisher search
 * The structure and approached used is quite different to the simple v1 API used previously
 *
 * @see SHERPAPublisher
 *
 * @author Kim Shepherd
 *
 */
public class SHERPAPublisherResponse {
    // Is this response to be treated as an error?
    private boolean error;

    // Error message
    private String message;

    // Parsed system metadata from search results
    private SHERPASystemMetadata metadata;

    // List of parsed publisher results
    private List<SHERPAPublisher> publishers;

    // Internal Sherpa ID
    private int id;

    // SHERPA URI (the human page version of this API response)
    private String uri;

    // Format enum - currently only JSON is supported
    public enum SHERPAFormat {
        JSON, XML
    };

    private static Logger log = LogManager.getLogger();

    /**
     * Parse SHERPA v2 API for a given format
     * @param input - input stream from the HTTP response content
     * @param format - requested format
     * @throws IOException
     */
    public SHERPAPublisherResponse(InputStream input, SHERPAFormat format) throws IOException {
        if (format == SHERPAFormat.JSON) {
            parseJSON(input);
        }
    }

    /**
     * Parse the SHERPA v2 API JSON and construct simple list of publisher objects
     * This method does not return a value, but rather populates the metadata and publishers objects
     * with data parsed from the JSON.
     * @param jsonData - the JSON input stream from the API result response body
     */
    private void parseJSON(InputStream jsonData) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(jsonData, StandardCharsets.UTF_8);
        JSONTokener jsonTokener = new JSONTokener(streamReader);
        JSONObject httpResponse;
        try {
            httpResponse = new JSONObject(jsonTokener);
            if (httpResponse.has("items")) {
                JSONArray items = httpResponse.getJSONArray("items");

                // items array in this context is publisher results - parsing is more simple than
                // parsing the full journal / policy responses
                if (items.length() > 0) {
                    metadata = new SHERPASystemMetadata();
                    this.publishers = new ArrayList<>();
                    // Iterate search result items
                    for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
                        SHERPAPublisher sherpaPublisher = new SHERPAPublisher();

                        JSONObject item = items.getJSONObject(itemIndex);

                        // Parse system metadata (per-item / result information)
                        if (item.has("system_metadata")) {
                            JSONObject systemMetadata = item.getJSONObject("system_metadata");
                            metadata = parseSystemMetadata(systemMetadata);
                            if (metadata.getId() >= 0) {
                                // Set publisher identifier to be the internal SHERPA ID
                                // eg. '30' (Elsevier)
                                sherpaPublisher.setIdentifier(String.valueOf(metadata.getId()));
                            }
                        }

                        // Set publisher name
                        sherpaPublisher.setName(parsePublisherName(item));

                        // Set publisher URL
                        sherpaPublisher.setUri(parsePublisherURL(item));

                        this.publishers.add(sherpaPublisher);
                    }

                } else {
                    error = true;
                    message = "No results found";
                }
            } else {
                error = true;
                message = "No results found";
            }

        } catch (JSONException e) {
            log.error("Failed to parse SHERPA response", e);
            error = true;
        } finally {
            streamReader.close();
        }
    }

    /**
     * Parse system metadata and return populated SHERPASystemMetadata object
     * @param systemMetadata
     */
    private SHERPASystemMetadata parseSystemMetadata(JSONObject systemMetadata) {

        SHERPASystemMetadata metadata = new SHERPASystemMetadata();

        if (systemMetadata.has("uri")) {
            this.uri = systemMetadata.getString("uri");
            metadata.setUri(this.uri);
        } else {
            log.error("SHERPA URI missing for API response item");
        }
        if (systemMetadata.has("id")) {
            this.id = systemMetadata.getInt("id");
            metadata.setId(this.id);
        } else {
            log.error("SHERPA internal ID missing for API response item");
        }
        // Get date created and added - DSpace expects this in the publisher object, though
        if (systemMetadata.has("date_created")) {
            metadata.setDateCreated(systemMetadata.getString("date_created"));
        }
        if (systemMetadata.has("date_modified")) {
            metadata.setDateModified(systemMetadata.getString("date_modified"));
        }

        return metadata;
    }

    /**
     * Parse publisher array and return the first name string found
     * @param publisher - array of publisher JSON data
     * @return first publisher name found (trimmed String)
     */
    private String parsePublisherName(JSONObject publisher) {
        String name = null;
        if (publisher.has("name")) {
            JSONArray publisherNames = publisher.getJSONArray("name");
            if (publisherNames.length() > 0) {
                JSONObject publisherName = publisherNames.getJSONObject(0);
                if (publisherName.has("name")) {
                    name = publisherName.getString("name").trim();
                }
            }
        }
        return name;
    }


    /**
     * Parse publisher URL from the json data
     * @param publisher - publisher object (from JSON array)
     * @return publisher URL as string
     */
    private String parsePublisherURL(JSONObject publisher) {
        if (publisher.has("url")) {
            return publisher.getString("url");
        }
        return null;
    }

    /**
     * Create new response object to be handled as an error
     * @param message - the message to render in logs or error pages
     */
    public SHERPAPublisherResponse(String message) {
        this.message = message;
        this.error = true;
    }

    public boolean isError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SHERPASystemMetadata getMetadata() {
        return metadata;
    }

    public List<SHERPAPublisher> getPublishers() {
        return publishers;
    }
}
