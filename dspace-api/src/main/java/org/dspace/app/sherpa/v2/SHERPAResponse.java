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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.dspace.core.I18nUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Model class for the SHERPAv2 API (JSON) response
 * The structure and approached used is quite different to the simple v1 API used previously
 * 
 * @author Kim Shepherd
 * 
 */
public class SHERPAResponse
{
    // Is this response to be treated as an error?
    private boolean error;

    // Error message
    private String message;

    // Parsed system metadata from search results
    private SHERPASystemMetadata metadata;

    // List of parsed journal results
    private List<SHERPAJournal> journals;

    // Internal Sherpa ID
    private int id;

    // SHERPA URI (the human page version of this API response)
    private String uri;

    // Format enum - currently only JSON is supported
    public enum SHERPAFormat {
        JSON, XML
    };

    private static Logger log = Logger.getLogger(SHERPAResponse.class);

    /**
     * Parse SHERPA v2 API for a given format
     * @param input - input stream from the HTTP response content
     * @param format - requested format
     * @throws IOException
     */
    public SHERPAResponse(InputStream input, SHERPAFormat format) throws IOException {
        if (format == SHERPAFormat.JSON) {
            parseJSON(input);
        }
    }

    /**
     * Parse the SHERPA v2 API JSON and construct Romeo policy data for display
     * This method does not return a value, but rather populates the metadata and journals objects
     * with data parsed from the JSON.
     * @param jsonData - the JSON input stream from the API result response body
     */
    private void parseJSON(InputStream jsonData) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(jsonData);
        JSONTokener jsonTokener = new JSONTokener(streamReader);
        JSONObject httpResponse;
        try {
            httpResponse = new JSONObject(jsonTokener);
            if (httpResponse.has("items")) {
                JSONArray items = httpResponse.getJSONArray("items");

                // items array is search results, *not* journals or publishers - they are listed for each item
                // - however, we only ever want one result since we're passing an "equals ISSN" query
                if (items.length() > 0) {
                    metadata = new SHERPASystemMetadata();
                    this.journals = new LinkedList<>();
                    // Iterate search result items
                    for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
                        List<SHERPAPublisher> sherpaPublishers = new LinkedList<>();
                        List<SHERPAPublisherPolicy> policies = new ArrayList<>();
                        SHERPAPublisher sherpaPublisher = new SHERPAPublisher();
                        SHERPAJournal sherpaJournal = new SHERPAJournal();

                        JSONObject item = items.getJSONObject(0);

                        // Parse system metadata (per-item / result information)
                        if (item.has("system_metadata")) {
                            JSONObject systemMetadata = item.getJSONObject("system_metadata");
                            metadata = parseSystemMetadata(systemMetadata);
                        }

                        // Parse "publisher policy"
                        // note - most of the information that was previously under 'publisher' is now under here
                        if (item.has("publisher_policy")) {

                            // Parse main publisher policies node
                            JSONArray publisherPolicies = item.getJSONArray("publisher_policy");
                            for (int i = 0; i < publisherPolicies.length(); i++) {

                                JSONObject policy = publisherPolicies.getJSONObject(i);

                                // Special case - quickly check the policy for the 'paid access' option
                                // and continue if found, then parse the rest of the policy
                                String moniker = null;
                                if (policy.has("internal_moniker")) {
                                    moniker = policy.getString("internal_moniker");
                                }
                                // This seems to be usually policy(ies) for the journal proper
                                // and then an "Open access option" which contains some of the info
                                // that the 'paidaccess' node in the old API used to contain
                                // Look for: internal_moniker = "Open access option"
                                // Check if this is OA options (Paid Access) or not
                                if ("Open access option".equalsIgnoreCase(moniker)) {
                                    log.debug("This is the Open access options policy - a special case");
                                    if (policy.has("urls")) {
                                        JSONArray urls = policy.getJSONArray("urls");
                                        for (int u = 0; u < urls.length(); u++) {
                                            JSONObject url = urls.getJSONObject(u);
                                            if (url.has("description") &&
                                                "Open Access".equalsIgnoreCase(url.getString("description"))) {
                                                log.debug("Found OA paid access url: " + url.getString("url"));
                                                sherpaPublisher.setPaidAccessDescription(url.getString("description"));
                                                sherpaPublisher.setPaidAccessUrl(url.getString("url"));
                                                break;
                                            }
                                        }
                                    }
                                    // Continue the loop here - this "policy" is a bit different and we
                                    // don't want to add irrelevant conditions to the policy
                                    continue;
                                }

                                // Parse the main publisher policy object and add to the list
                                SHERPAPublisherPolicy sherpaPublisherPolicy = parsePublisherPolicy(policy);
                                policies.add(sherpaPublisherPolicy);
                            }

                            // set publisher name - note we're only looking for the first name here
                            // as per previous functionality (for simple display)
                            if (item.has("publishers")) {
                                JSONArray publishers = item.getJSONArray("publishers");
                                if (publishers.length() > 0) {
                                    JSONObject publisherElement = publishers.getJSONObject(0);
                                    if (publisherElement.has("publisher")) {
                                        JSONObject publisher = publisherElement.getJSONObject("publisher");
                                        sherpaPublisher.setName(parsePublisherName(publisher));
                                        sherpaPublisher.setUri(parsePublisherURL(publisher));
                                    }
                                }
                            }

                            // Parse journal data
                            sherpaJournal = parseJournal(item, sherpaPublisher.getName());
                        }

                        sherpaPublishers.add(sherpaPublisher);
                        sherpaJournal.setPublisher(sherpaPublisher);
                        sherpaJournal.setPublishers(sherpaPublishers);
                        sherpaJournal.setPolicies(policies);
                        this.journals.add(sherpaJournal);
                    }

                } else {
                    error = true;
                    message = "No results found";
                }
            } else {
                error = true;
                message = "No results found";
            }

        } catch(JSONException e) {
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
        // Is this item publicly visible?
        if (systemMetadata.has("publicly_visible")) {
            metadata.setPubliclyVisible ("yes".equals(systemMetadata
                .getString("publicly_visible")));
        }
        // Is this item listed in the DOAJ?
        if (systemMetadata.has("listed_in_doaj")) {
            metadata.setPubliclyVisible ("yes".equals(systemMetadata
                .getString("listed_in_doaj")));
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
    private SHERPAJournal parseJournal(JSONObject item, String publisherName) {

        SHERPAJournal sherpaJournal = new SHERPAJournal();

        // set journal title
        if (item.has("title")) {
            JSONArray titles = item.getJSONArray("title");
            if (titles.length() > 0) {
                List<String> titleList = new ArrayList<>();
                for (int t = 0; t < titles.length(); t++) {
                    JSONObject title = titles.getJSONObject(t);
                    if (title.has("title")) {
                        titleList.add(title.getString("title").trim());
                    }
                }
                sherpaJournal.setTitles(titleList);
                if (titleList.size() > 0) {
                    // Faking this a bit based on what I'd seen - not in the API v2 data
                    sherpaJournal.setRomeoPub(publisherName + ": "
                        + titleList.get(0));
                    sherpaJournal.setZetoPub(publisherName + ": "
                        + titleList.get(0));
                }
            }
        }

        // Journal URL
        if (item.has("url")) {
            sherpaJournal.setUrl(item.getString("url"));
        }

        // set ISSNs
        if (item.has("issns")) {
            JSONArray issns = item.getJSONArray("issns");
            // just get first - DSpace data model only allows for one
            List<String> issnList = new ArrayList<>();
            for (int ii = 0; ii < issns.length(); ii++) {
                JSONObject issn = issns.getJSONObject(ii);
                issnList.add(issn.getString("issn").trim());
            }
            sherpaJournal.setIssns(issnList);
        }

        // Is the item in DOAJ?
        if (item.has("listed_in_doaj")) {
            sherpaJournal.setInDOAJ(("yes".equals(item.getString("listed_in_doaj"))));
        }

        return sherpaJournal;
    }

    /**
     * Parse a publisher_policy JSON data and return a populated bean
     * @param policy - each publisher policy node in the JSON array
     * @return populated SHERPAPublisherPolicy object
     */
    private SHERPAPublisherPolicy parsePublisherPolicy(JSONObject policy) {

        SHERPAPublisherPolicy sherpaPublisherPolicy = new SHERPAPublisherPolicy();

        // Get and set monikers
        String moniker = null;
        if (policy.has("internal_moniker")) {
            moniker = policy.getString("internal_moniker");
            sherpaPublisherPolicy.setInternalMoniker(moniker);
        }

        // URLs (used to be Copyright Links)
        if (policy.has("urls")) {
            JSONArray urls = policy.getJSONArray("urls");
            Map<String, String> copyrightLinks = new TreeMap<>();
            for (int u = 0; u < urls.length(); u++) {
                JSONObject url = urls.getJSONObject(u);
                if (url.has("description") && url.has("url")) {
                    log.debug("Setting copyright URL: " + url.getString("url"));
                    copyrightLinks.put(url.getString("url"), url.getString("description"));
                }
            }
            sherpaPublisherPolicy.setUrls(copyrightLinks);
        }

        // Permitted OA options
        int submittedOption = 0;
        int acceptedOption = 0;
        int publishedOption = 0;
        int currentOption = 0;
        if (policy.has("permitted_oa")) {
            List<String> allowed = new ArrayList<>();
            JSONArray permittedOA = policy.getJSONArray("permitted_oa");
            List<SHERPAPermittedVersion> permittedVersions = new ArrayList<>();

            // Iterate each permitted OA version / option
            for (int p = 0; p < permittedOA.length(); p++) {
                JSONObject permitted = permittedOA.getJSONObject(p);
                SHERPAPermittedVersion permittedVersion = parsePermittedVersion(permitted);

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

                // Populate the old indicators into the publisher policy object
                if (allowed.contains("submitted")) {
                    sherpaPublisherPolicy.setPreArchiving("can");
                }
                if (allowed.contains("accepted")) {
                    sherpaPublisherPolicy.setPostArchiving("can");
                }
                if (allowed.contains("published")) {
                    sherpaPublisherPolicy.setPubArchiving("can");
                }

            }
            sherpaPublisherPolicy.setPermittedVersions(permittedVersions);
        }

        return sherpaPublisherPolicy;
    }

    /**
     * Parse permitted version JSON and populate new bean from the data
     * @param permitted - each 'permitted_oa' node in the JSON array
     * @return populated SHERPAPermittedVersion object
     */
    private SHERPAPermittedVersion parsePermittedVersion(JSONObject permitted) {

        SHERPAPermittedVersion permittedVersion = new SHERPAPermittedVersion();

        // Get the article version, which is ultimately used for the ticks / crosses
        // in the UI display. My assumptions around translation:
        // submitted = preprint
        // accepted = postprint
        // published = pdfversion
        String articleVersion = "unknown";
        String versionLabel = "Unknown";

        if (permitted.has("article_version")) {
            JSONArray versions = permitted.getJSONArray("article_version");
            articleVersion = versions.getString(0);
            permittedVersion.setArticleVersion(articleVersion);
            log.debug("Added allowed version: " + articleVersion + " to list");
        }

        if ("submitted".equals(articleVersion)) {
            versionLabel = I18nUtil.getMessage("jsp.sherpa.submitted-version-label");
        } else if("accepted".equals(articleVersion)) {
            versionLabel = I18nUtil.getMessage("jsp.sherpa.accepted-version-label");
        } else if("published".equals(articleVersion)) {
            versionLabel = I18nUtil.getMessage("jsp.sherpa.published-version-label");
        }

        // These are now child arrays, in old API they were explicit like
        // "preprint restrictions", etc., and just contained text rather than data
        if (permitted.has("conditions")) {
            List<String> conditionList = new ArrayList<>();
            JSONArray conditions = permitted.getJSONArray("conditions");
            for (int c = 0; c < conditions.length(); c++) {
                conditionList.add(conditions.getString(c).trim());
            }
            permittedVersion.setConditions(conditionList);
        }

        permittedVersion.setArticleVersionLabel(versionLabel);

        // Any prerequisites for this option (eg required by funder)
        List<String> prerequisites = new ArrayList<>();
        if (permitted.has("prerequisites")) {
            JSONObject prereqs = permitted.getJSONObject("prerequisites");
            if (prereqs.has("prerequisites_phrases")) {
                JSONArray phrases = prereqs.getJSONArray("prerequisites_phrases");
                for (int pp = 0; pp < phrases.length(); pp++) {
                    JSONObject phrase = phrases.getJSONObject(pp);
                    if (phrase.has("phrase")) {
                        prerequisites.add(phrase.getString("phrase").trim());
                    }
                }
            }
        }
        permittedVersion.setPrerequisites(prerequisites);

        // Locations where this version / option may be archived
        List<String> sherpaLocations = new ArrayList<>();
        if (permitted.has("location")) {
            JSONObject locations = permitted.getJSONObject("location");
            if (locations.has("location_phrases")) {
                JSONArray locationPhrases = locations.getJSONArray("location_phrases");
                if (locationPhrases.length() > 0) {
                    for (int l = 0; l < locationPhrases.length(); l++) {
                        JSONObject locationPhrase = locationPhrases.getJSONObject(l);
                        if (locationPhrase.has("phrase")) {
                            sherpaLocations.add(locationPhrase.getString("phrase").trim());
                        }
                    }
                }
            }
        }
        permittedVersion.setLocations(sherpaLocations);

        List<String> sherpaLicenses = new ArrayList<>();
        // required licences
        if (permitted.has("license")) {
            JSONArray licences = permitted.getJSONArray("license");
            for (int l = 0; l < licences.length(); l++) {
                JSONObject licence = licences.getJSONObject(l);
                if (licence.has("license_phrases")) {
                    JSONArray phrases = licence.getJSONArray("license_phrases");
                    for (int ll = 0; ll < phrases.length(); ll++) {
                        JSONObject phrase = phrases.getJSONObject(ll);
                        if (phrase.has("phrase")) {
                            sherpaLicenses.add(phrase.getString("phrase").trim());
                        }
                    }
                }
            }
        }
        permittedVersion.setLicenses(sherpaLicenses);

        return permittedVersion;
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
    public SHERPAResponse(String message)
    {
        this.message = message;
        this.error = true;
    }

    public boolean isError()
    {
        return error;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<SHERPAJournal> getJournals()
    {
        return journals;
    }

    public SHERPASystemMetadata getMetadata() {
        return metadata;
    }
}
