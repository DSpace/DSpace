/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.openpolicyfinder.v2;

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
 * Model class for the Open Policy Finder API (JSON) response for a publisher search
 * The structure and approached used is quite different to the simple v1 API used previously
 *
 * @see OpenPolicyFinderPublisher
 *
 * @author Kim Shepherd
 *
 */
public class OpenPolicyFinderPublisherResponse {
    // Is this response to be treated as an error?
    private boolean error;

    // Error message
    private String message;

    // Parsed system metadata from search results
    private OpenPolicyFinderSystemMetadata metadata;

    // List of parsed publisher results
    private List<OpenPolicyFinderPublisher> publishers;

    // Internal Open Policy Finder ID
    private int id;

    // Open Policy Finder URI (the human page version of this API response)
    private String uri;

    // Format enum - currently only JSON is supported
    public enum ResponseFormat {
        JSON, XML
    };

    private static Logger log = LogManager.getLogger();

    /**
     * Parse Open Policy Finder API for a given format
     * @param input - input stream from the HTTP response content
     * @param format - requested format
     * @throws IOException
     */
    public OpenPolicyFinderPublisherResponse(InputStream input, ResponseFormat format) throws IOException {
        if (format == ResponseFormat.JSON) {
            parseJSON(input);
        }
    }

    /**
     * Parse the Open Policy Finder API JSON and construct simple list of publisher objects
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
                    metadata = new OpenPolicyFinderSystemMetadata();
                    this.publishers = new ArrayList<>();
                    // Iterate search result items
                    for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
                        OpenPolicyFinderPublisher publisher = new OpenPolicyFinderPublisher();

                        JSONObject item = items.getJSONObject(itemIndex);

                        // Parse system metadata (per-item / result information)
                        if (item.has("system_metadata")) {
                            JSONObject systemMetadata = item.getJSONObject("system_metadata");
                            metadata = parseSystemMetadata(systemMetadata);
                            if (metadata.getId() >= 0) {
                                // Set publisher identifier to be the internal Open Policy Finder ID
                                // eg. '30' (Elsevier)
                                publisher.setIdentifier(String.valueOf(metadata.getId()));
                            }
                        }

                        // Set publisher name
                        publisher.setName(parsePublisherName(item));

                        // Set publisher URL
                        publisher.setUri(parsePublisherURL(item));

                        this.publishers.add(publisher);
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
            log.error("Failed to parse Open Policy Finder response", e);
            error = true;
        } finally {
            streamReader.close();
        }
    }

    /**
     * Parse system metadata and return populated OpenPolicyFinderSystemMetadata object
     * @param systemMetadata
     */
    private OpenPolicyFinderSystemMetadata parseSystemMetadata(JSONObject systemMetadata) {

        OpenPolicyFinderSystemMetadata metadata = new OpenPolicyFinderSystemMetadata();

        if (systemMetadata.has("uri")) {
            this.uri = systemMetadata.getString("uri");
            metadata.setUri(this.uri);
        } else {
            log.error("Open Policy Finder URI missing for API response item");
        }
        if (systemMetadata.has("id")) {
            this.id = systemMetadata.getInt("id");
            metadata.setId(this.id);
        } else {
            log.error("Open Policy Finder internal ID missing for API response item");
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
    public OpenPolicyFinderPublisherResponse(String message) {
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

    public OpenPolicyFinderSystemMetadata getMetadata() {
        return metadata;
    }

    public List<OpenPolicyFinderPublisher> getPublishers() {
        return publishers;
    }
}
