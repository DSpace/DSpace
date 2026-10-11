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
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Model class for the Open Policy Finder API (JSON) response for a publication (journal) search
 * The structure and approached used is quite different to the simple v1 API used previously
 * The structure is based on journal data, which in turn contains data about publishers and policies
 *
 * @see OpenPolicyFinderJournal
 *
 * @author Kim Shepherd
 *
 */
public class OpenPolicyFinderResponse implements Serializable {

    private static final long serialVersionUID = 2732963970169240597L;

    // Is this response to be treated as an error?
    private boolean error;

    // Error message
    private String message;

    // Parsed system metadata from search results
    private OpenPolicyFinderSystemMetadata metadata;

    // List of parsed journal results
    private List<OpenPolicyFinderJournal> journals;

    // Internal Open Policy Finder ID
    private int id;

    // Open Policy Finder URI (the human page version of this API response)
    private String uri;

    @JsonIgnore
    private Instant retrievalTime = Instant.now();

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
    public OpenPolicyFinderResponse(InputStream input, ResponseFormat format) throws IOException {
        if (format == ResponseFormat.JSON) {
            parseJSON(input);
        }
    }

    /**
     * Create an empty OpenPolicyFinderResponse representation
     */
    public OpenPolicyFinderResponse() {}

    /**
     * Parse the Open Policy Finder API JSON and construct Romeo policy data for display
     * This method does not return a value, but rather populates the metadata and journals objects
     * with data parsed from the JSON.
     * @param jsonData - the JSON input stream from the API result response body
     */
    private void parseJSON(InputStream jsonData) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(jsonData, StandardCharsets.UTF_8);
        JsonNode httpResponse;
        try {
            httpResponse = new ObjectMapper().readTree(streamReader);
            if (httpResponse.has("items")) {
                JsonNode items = httpResponse.required("items");

                // items array is search results, *not* journals or publishers - they are listed for each item
                // - however, we only ever want one result since we're passing an "equals ISSN" query
                if (items.size() > 0) {
                    metadata = new OpenPolicyFinderSystemMetadata();
                    this.journals = new ArrayList<>();
                    // Iterate search result items
                    for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                        List<OpenPolicyFinderPublisher> publishers = new ArrayList<>();
                        List<OpenPolicyFinderPublisherPolicy> policies = new ArrayList<>();
                        OpenPolicyFinderPublisher publisher = new OpenPolicyFinderPublisher();
                        OpenPolicyFinderJournal journal = new OpenPolicyFinderJournal();

                        JsonNode item = items.required(itemIndex);

                        // Parse system metadata (per-item / result information)
                        if (item.has("system_metadata")) {
                            JsonNode systemMetadata = item.required("system_metadata");
                            metadata = parseSystemMetadata(systemMetadata);
                        }

                        // Parse "publisher policy"
                        // note - most of the information that was previously under 'publisher' is now under here
                        if (item.has("publisher_policy")) {

                            // Parse main publisher policies node
                            JsonNode publisherPolicies = item.required("publisher_policy");
                            for (int i = 0; i < publisherPolicies.size(); i++) {

                                JsonNode policy = publisherPolicies.required(i);

                                // Special case - quickly check the policy for the 'paid access' option
                                // and continue if found, then parse the rest of the policy
                                String moniker = null;
                                if (policy.has("internal_moniker")) {
                                    moniker = policy.required("internal_moniker").asString();
                                }
                                // This seems to be usually policy(ies) for the journal proper
                                // and then an "Open access option" which contains some of the info
                                // that the 'paidaccess' node in the old API used to contain
                                // Look for: internal_moniker = "Open access option"
                                // Check if this is OA options (Paid Access) or not
                                if ("Open access option".equalsIgnoreCase(moniker)) {
                                    log.debug("This is the Open access options policy - a special case");
                                    if (policy.has("urls")) {
                                        JsonNode urls = policy.required("urls");
                                        for (int u = 0; u < urls.size(); u++) {
                                            JsonNode url = urls.required(u);
                                            String description = url.has("description")
                                                ? url.required("description").asString() : null;
                                            if ("Open Access".equalsIgnoreCase(description)) {
                                                String paidAccessUrl = url.required("url").asString();
                                                log.debug("Found OA paid access url: " + paidAccessUrl);
                                                publisher.setPaidAccessDescription(description);
                                                publisher.setPaidAccessUrl(paidAccessUrl);
                                                break;
                                            }
                                        }
                                    }
                                    // Continue the loop here - this "policy" is a bit different and we
                                    // don't want to add irrelevant conditions to the policy
                                    continue;
                                }

                                // Parse the main publisher policy object and add to the list
                                OpenPolicyFinderPublisherPolicy publisherPolicy = parsePublisherPolicy(policy);
                                policies.add(publisherPolicy);
                            }

                            // set publisher name - note we're only looking for the first name here
                            // as per previous functionality (for simple display)
                            if (item.has("publishers")) {
                                JsonNode publishersArray = item.required("publishers");
                                if (publishersArray.size() > 0) {
                                    JsonNode publisherElement = publishersArray.required(0);
                                    if (publisherElement.has("publisher")) {
                                        JsonNode publisherJson = publisherElement.required("publisher");
                                        publisher.setName(parsePublisherName(publisherJson));
                                        publisher.setUri(parsePublisherURL(publisherJson));
                                    }
                                }
                            }

                            // Parse journal data
                            journal = parseJournal(item, publisher.getName());
                        }

                        publishers.add(publisher);
                        journal.setPublisher(publisher);
                        journal.setPublishers(publishers);
                        journal.setPolicies(policies);
                        this.journals.add(journal);
                    }

                } else {
                    error = true;
                    message = "No results found";
                }
            } else {
                error = true;
                message = "No results found";
            }

        } catch (JacksonException e) {
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
    private OpenPolicyFinderSystemMetadata parseSystemMetadata(JsonNode systemMetadata) {

        OpenPolicyFinderSystemMetadata metadata = new OpenPolicyFinderSystemMetadata();

        if (systemMetadata.has("uri")) {
            this.uri = systemMetadata.required("uri").asString();
            metadata.setUri(this.uri);
        } else {
            log.error("Open Policy Finder URI missing for API response item");
        }
        if (systemMetadata.has("id")) {
            this.id = systemMetadata.required("id").asInt();
            metadata.setId(this.id);
        } else {
            log.error("Open Policy Finder internal ID missing for API response item");
        }
        // Get date created and added - DSpace expects this in the publisher object, though
        if (systemMetadata.has("date_created")) {
            metadata.setDateCreated(systemMetadata.required("date_created").asString());
        }
        if (systemMetadata.has("date_modified")) {
            metadata.setDateModified(systemMetadata.required("date_modified").asString());
        }
        // Is this item publicly visible?
        if (systemMetadata.has("publicly_visible")) {
            metadata.setPubliclyVisible ("yes".equals(systemMetadata
                .required("publicly_visible").asString()));
        }
        // Is this item listed in the DOAJ?
        if (systemMetadata.has("listed_in_doaj")) {
            metadata.setPubliclyVisible ("yes".equals(systemMetadata
                .required("listed_in_doaj").asString()));
        }

        return metadata;
    }

    /**
     * Parse journal JSON data and return populated bean
     * This method also takes publisherName as a string to help construct some
     * legacy labels
     * @param item - the main result item JSON (which is the closest thing to an actual 'journal')
     * @param publisherName - the parsed publisher name
     * @return
     */
    private OpenPolicyFinderJournal parseJournal(JsonNode item, String publisherName) {

        OpenPolicyFinderJournal journal = new OpenPolicyFinderJournal();

        // set journal title
        if (item.has("title")) {
            JsonNode titles = item.required("title");
            if (titles.size() > 0) {
                List<String> titleList = new ArrayList<>();
                for (int t = 0; t < titles.size(); t++) {
                    JsonNode title = titles.required(t);
                    if (title.has("title")) {
                        titleList.add(title.required("title").asString().trim());
                    }
                }
                journal.setTitles(titleList);
                if (titleList.size() > 0) {
                    // Faking this a bit based on what I'd seen - not in the API v2 data
                    journal.setRomeoPub(publisherName + ": "
                        + titleList.get(0));
                    journal.setZetoPub(publisherName + ": "
                        + titleList.get(0));
                    log.debug("Found journal title: " + titleList.get(0));
                }
            }
        }

        // Journal URL
        if (item.has("url")) {
            journal.setUrl(item.required("url").asString());
        }

        // set ISSNs
        if (item.has("issns")) {
            JsonNode issns = item.required("issns");
            // just get first - DSpace data model only allows for one
            List<String> issnList = new ArrayList<>();
            for (int ii = 0; ii < issns.size(); ii++) {
                JsonNode issn = issns.required(ii);
                issnList.add(issn.required("issn").asString().trim());
            }
            journal.setIssns(issnList);
        }

        // Is the item in DOAJ?
        if (item.has("listed_in_doaj")) {
            journal.setInDOAJ("yes".equals(item.required("listed_in_doaj").asString()));
        }

        return journal;
    }

    /**
     * Parse a publisher_policy JSON data and return a populated bean
     * @param policy - each publisher policy node in the JSON array
     * @return populated OpenPolicyFinderPublisherPolicy object
     */
    private OpenPolicyFinderPublisherPolicy parsePublisherPolicy(JsonNode policy) {

        OpenPolicyFinderPublisherPolicy publisherPolicy = new OpenPolicyFinderPublisherPolicy();

        // Get and set monikers
        String moniker = null;
        if (policy.has("internal_moniker")) {
            moniker = policy.required("internal_moniker").asString();
            publisherPolicy.setInternalMoniker(moniker);
        }

        // URLs (used to be Copyright Links)
        if (policy.has("urls")) {
            JsonNode urls = policy.required("urls");
            Map<String, String> copyrightLinks = new TreeMap<>();
            for (int u = 0; u < urls.size(); u++) {
                JsonNode url = urls.required(u);
                if (url.has("description") && url.has("url")) {
                    log.debug("Setting copyright URL: " + url.required("url").asString());
                    copyrightLinks.put(url.required("url").asString(), url.required("description").asString());
                }
            }
            publisherPolicy.setUrls(copyrightLinks);
        }

        // Permitted OA options
        int submittedOption = 0;
        int acceptedOption = 0;
        int publishedOption = 0;
        int currentOption = 0;
        if (policy.has("permitted_oa")) {
            List<String> allowed = new ArrayList<>();
            JsonNode permittedOA = policy.required("permitted_oa");
            List<OpenPolicyFinderPermittedVersion> permittedVersions = new ArrayList<>();

            // Iterate each permitted OA version / option. The permitted_oa node is also known as a 'pathway' --
            // essentially "a way to get a work into a repository". Each pathway could refer to one article version
            // like a pre-print, or multiple versions might have the same acceptable locations and conditions.
            // As described below, where multiple versions are referenced in a single permitted_oa pathway, they will
            // be split out and treated separately. This keeps processing simple, especially later in display or
            // compliance checking when it is preferred to group / indicate rules by the article version
            for (int p = 0; p < permittedOA.size(); p++) {
                JsonNode permitted = permittedOA.required(p);
                // Although it adds redundancy, we will treat each 'article version' within
                // the permitted_oa ("pathway") node as a separate version altogether to keep the rest of our display
                // handled nicely. This was confirmed as an appropriate approach by JISC
                if (permitted.has("article_version")) {
                    JsonNode versions = permitted.required("article_version");
                    for (int v = 0; v < versions.size(); v++) {
                        // Parse this permitted_oa node but specifically looking for the article_version 'v'
                        OpenPolicyFinderPermittedVersion permittedVersion = parsePermittedVersion(permitted, v);

                        // To determine which option # we are, inspect article versions and set
                        allowed.add(permittedVersion.getArticleVersion());
                        if ("submitted".equals(permittedVersion.getArticleVersion())) {
                            submittedOption++;
                            currentOption = submittedOption;
                        } else if ("accepted".equals(permittedVersion.getArticleVersion())) {
                            acceptedOption++;
                            currentOption = acceptedOption;
                        } else if ("published".equals(permittedVersion.getArticleVersion())) {
                            publishedOption++;
                            currentOption = publishedOption;
                        }
                        permittedVersion.setOption(currentOption);
                        permittedVersions.add(permittedVersion);
                    }
                }

                // Populate the old indicators into the publisher policy object
                if (allowed.contains("submitted")) {
                    publisherPolicy.setPreArchiving("can");
                }
                if (allowed.contains("accepted")) {
                    publisherPolicy.setPostArchiving("can");
                }
                if (allowed.contains("published")) {
                    publisherPolicy.setPubArchiving("can");
                }

            }
            publisherPolicy.setPermittedVersions(permittedVersions);
        }

        return publisherPolicy;
    }

    /**
     * Parse permitted version JSON and populate new bean from the data
     * @param permitted - each 'permitted_oa' node in the JSON array
     * @return populated OpenPolicyFinderPermittedVersion object
     */
    private OpenPolicyFinderPermittedVersion parsePermittedVersion(JsonNode permitted, int index) {

        OpenPolicyFinderPermittedVersion permittedVersion = new OpenPolicyFinderPermittedVersion();

        // Get the article version, which is ultimately used for the ticks / crosses
        // in the UI display. My assumptions around translation:
        // submitted = preprint
        // accepted = postprint
        // published = pdfversion
        // These strings can be used to construct i18n messages.
        String articleVersion = "unknown";

        // Each 'permitted OA' can actually refer to multiple versions
        if (permitted.has("article_version")) {
            JsonNode versions = permitted.required("article_version");

            // Get one particular article version to return as a PermittedVersion. The outer loop calling this
            // is iterating all permitted_oa and permitted_oa->article_version array members
            articleVersion = versions.required(index).asString();
            permittedVersion.setArticleVersion(articleVersion);
            log.debug("Added allowed version: " + articleVersion + " to list");
        }

        // These are now child arrays, in old API they were explicit like
        // "preprint restrictions", etc., and just contained text rather than data
        if (permitted.has("conditions")) {
            List<String> conditionList = new ArrayList<>();
            JsonNode conditions = permitted.required("conditions");
            for (int c = 0; c < conditions.size(); c++) {
                conditionList.add(conditions.required(c).asString().trim());
            }
            permittedVersion.setConditions(conditionList);
        }

        // Any prerequisites for this option (eg required by funder)
        List<String> prerequisites = new ArrayList<>();
        if (permitted.has("prerequisites")) {
            JsonNode prereqs = permitted.required("prerequisites");
            if (prereqs.has("prerequisites_phrases")) {
                JsonNode phrases = prereqs.required("prerequisites_phrases");
                for (int pp = 0; pp < phrases.size(); pp++) {
                    JsonNode phrase = phrases.required(pp);
                    if (phrase.has("phrase")) {
                        prerequisites.add(phrase.required("phrase").asString().trim());
                    }
                }
            }
        }
        permittedVersion.setPrerequisites(prerequisites);

        // Locations where this version / option may be archived
        List<String> allowedLocations = new ArrayList<>();
        if (permitted.has("location")) {
            JsonNode locations = permitted.required("location");
            if (locations.has("location_phrases")) {
                JsonNode locationPhrases = locations.required("location_phrases");
                if (locationPhrases.size() > 0) {
                    for (int l = 0; l < locationPhrases.size(); l++) {
                        JsonNode locationPhrase = locationPhrases.required(l);
                        if (locationPhrase.has("phrase")) {
                            allowedLocations.add(locationPhrase.required("phrase").asString().trim());
                        }
                    }
                }
            }
        }
        permittedVersion.setLocations(allowedLocations);

        List<String> licenses = new ArrayList<>();
        // required licences
        if (permitted.has("license")) {
            JsonNode licences = permitted.required("license");
            for (int l = 0; l < licences.size(); l++) {
                JsonNode licence = licences.required(l);
                if (licence.has("license_phrases")) {
                    JsonNode phrases = licence.required("license_phrases");
                    for (int ll = 0; ll < phrases.size(); ll++) {
                        JsonNode phrase = phrases.required(ll);
                        if (phrase.has("phrase")) {
                            licenses.add(phrase.required("phrase").asString().trim());
                        }
                    }
                }
            }
        }
        permittedVersion.setLicenses(licenses);

        if (permitted.has("embargo")) {
            JsonNode embargo = permitted.required("embargo");
            OpenPolicyFinderEmbargo OpenPolicyFinderEmbargo =
                    new OpenPolicyFinderEmbargo(embargo.required("amount").asInt(),
                            embargo.required("units").asString());
            permittedVersion.setEmbargo(OpenPolicyFinderEmbargo);
        }

        return permittedVersion;
    }

    /**
     * Parse publisher array and return the first name string found
     * @param publisher - array of publisher JSON data
     * @return first publisher name found (trimmed String)
     */
    private String parsePublisherName(JsonNode publisher) {
        String name = null;
        if (publisher.has("name")) {
            JsonNode publisherNames = publisher.required("name");
            if (publisherNames.size() > 0) {
                JsonNode publisherName = publisherNames.required(0);
                if (publisherName.has("name")) {
                    name = publisherName.required("name").asString().trim();
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
    private String parsePublisherURL(JsonNode publisher) {
        if (publisher.has("url")) {
            return publisher.required("url").asString();
        }
        return null;
    }

    /**
     * Create new response object to be handled as an error
     * @param message - the message to render in logs or error pages
     */
    public OpenPolicyFinderResponse(String message) {
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

    public List<OpenPolicyFinderJournal> getJournals() {
        return journals;
    }

    public OpenPolicyFinderSystemMetadata getMetadata() {
        return metadata;
    }

    public Instant getRetrievalTime() {
        return retrievalTime;
    }
}
