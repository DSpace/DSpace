/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

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
    private boolean error;
    private String message;
    private String license;
    private String licenseURL;
    private String disclaimer;
    private SHERPASystemMetadata metadata;
    private List<SHERPAJournal> journals;


    // Internal Sherpa ID
    private int id;

    // SHERPA URI (the human page version of this API response)

    private String uri;

    // Journal / publisher URL
    private String journalUrl;

    public enum SHERPAFormat {
        XML, JSON
    };

    private static Logger log = Logger.getLogger(SHERPAResponse.class);

    public SHERPAResponse(InputStream inputStream) {
        this(inputStream, SHERPAFormat.XML);
    }

    public SHERPAResponse(InputStream input, SHERPAFormat format) {
        if (format == SHERPAFormat.JSON) {
            parseJSON(input);
        }
    }

    /**
     * Parse the SHERPA v2 API JSON and construct Romeo policy data for display
     * @param jsonData
     */
    private void parseJSON(InputStream jsonData) {
        JSONTokener jsonTokener = new JSONTokener(new InputStreamReader(jsonData));
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

                    for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
                        List<SHERPAPublisher> sherpaPublishers = new LinkedList<>();
                        List<SHERPAPublisherPolicy> policies = new ArrayList<>();
                        SHERPAPublisher sherpaPublisher = new SHERPAPublisher();
                        SHERPAJournal sherpaJournal = new SHERPAJournal();

                        JSONObject item = items.getJSONObject(0);

                        if (item.has("system_metadata")) {
                            JSONObject systemMetadata = item.getJSONObject("system_metadata");
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
                        }
                        if (item.has("url")) {
                            this.journalUrl = item.getString("url");
                        }

                        boolean inDOAJ = false;
                        if (item.has("listed_in_doaj")) {
                            inDOAJ = ("yes".equals(item.getString("listed_in_doaj")));
                        }

                        // Parse "publisher policy"
                        // note - most of the information that was previously under 'publisher' is now under here
                        if (item.has("publisher_policy")) {

                            // Parse main publisher policies node
                            JSONArray publisherPolicies = item.getJSONArray("publisher_policy");
                            for (int i = 0; i < publisherPolicies.length(); i++) {

                                JSONObject policy = publisherPolicies.getJSONObject(i);

                                // Make my new PublisherPolicy object
                                SHERPAPublisherPolicy sherpaPublisherPolicy = new SHERPAPublisherPolicy();

                                String moniker = null;
                                if (policy.has("internal_moniker")) {
                                    moniker = policy.getString("internal_moniker");
                                    sherpaPublisherPolicy.setInternalMoniker(moniker);
                                }
                                log.debug("Parsing publisher_policy number " + i + " (" + moniker + ")");

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
                                    // don't want to overwrite the other conditions, etc.
                                    continue;
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
                                    for (int p = 0; p < permittedOA.length(); p++) {
                                        JSONObject permitted = permittedOA.getJSONObject(p);
                                        // New PermittedVersion object
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
                                            allowed.add(articleVersion);
                                            permittedVersion.setArticleVersion(articleVersion);
                                            log.debug("Added allowed version: " + articleVersion + " to list");
                                        }

                                        if ("submitted".equals(articleVersion)) {
                                            versionLabel = I18nUtil.getMessage("jsp.sherpa.submitted-version-label");
                                            submittedOption++;
                                            currentOption = submittedOption;
                                        } else if("accepted".equals(articleVersion)) {
                                            versionLabel = I18nUtil.getMessage("jsp.sherpa.accepted-version-label");
                                            acceptedOption++;
                                            currentOption = acceptedOption;
                                        } else if("published".equals(articleVersion)) {
                                            versionLabel = I18nUtil.getMessage("jsp.sherpa.published-version-label");
                                            publishedOption++;
                                            currentOption = publishedOption;
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

                                        permittedVersion.setArticleVersion(versionLabel);
                                        permittedVersion.setOption(currentOption);

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

                                        // A rough attempt at guessing colour based on the doc in the DSpace page
                                        // SHERPA have confirmed we shouldn't really use this anymore
                                        if (allowed.contains("submitted") &&
                                            (allowed.contains("accepted") || allowed.contains("published"))) {
                                            sherpaPublisher.setRomeoColour("green");
                                        } else if (allowed.contains("accepted") || allowed.contains("published")) {
                                            sherpaPublisher.setRomeoColour("blue");
                                        } else if (allowed.contains("submitted")) {
                                            sherpaPublisher.setRomeoColour("yellow");
                                        } else if (inDOAJ) {
                                            sherpaPublisher.setRomeoColour("gray");
                                        } else {
                                            sherpaPublisher.setRomeoColour("white");
                                        }

                                        // Populate the old indicators
                                        if (allowed.contains("submitted")) {
                                            sherpaPublisherPolicy.setPreArchiving("can");
                                        }
                                        if (allowed.contains("accepted")) {
                                            sherpaPublisherPolicy.setPostArchiving("can");
                                        }
                                        if (allowed.contains("published")) {
                                            sherpaPublisherPolicy.setPubArchiving("can");
                                        }

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
                                        permittedVersions.add(permittedVersion);
                                    }
                                    sherpaPublisherPolicy.setPermittedVersions(permittedVersions);
                                }
                                policies.add(sherpaPublisherPolicy);
                            }

                            // set publisher name - note we're still in the if (publisher policy) block
                            // since this info is sort of combined, now.
                            // So I'll also just look for first publisher here, it's only for the name anyway
                            // (I imagine multiple is just for older owners/ name variants?)
                            if (item.has("publishers")) {
                                JSONArray publishers = item.getJSONArray("publishers");
                                if (publishers.length() > 0) {
                                    JSONObject publisherElement = publishers.getJSONObject(0);
                                    if (publisherElement.has("publisher")) {
                                        JSONObject publisher = publisherElement.getJSONObject("publisher");
                                        if (publisher.has("name")) {
                                            JSONArray publisherNames = publisher.getJSONArray("name");
                                            if (publisherNames.length() > 0) {
                                                JSONObject publisherName = publisherNames.getJSONObject(0);
                                                if (publisherName.has("name")) {
                                                    sherpaPublisher.setName(publisherName.getString("name").trim());
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // set title
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
                                        sherpaJournal.setRomeoPub(sherpaPublisher.getName() + ": "
                                            + titleList.get(0));
                                        sherpaJournal.setZetoPub(sherpaPublisher.getName() + ": "
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
                        }

                        sherpaPublishers.add(sherpaPublisher);
                        sherpaJournal.setPublisher(sherpaPublisher);
                        sherpaJournal.setPublishers(sherpaPublishers);
                        sherpaJournal.setPolicies(policies);
                        this.journals.add(sherpaJournal);
                    }

                    String licenceText = I18nUtil.getMessage("jsp.sherpa.license-default");
                    String disclaimerText = I18nUtil.getMessage("jsp.sherpa.disclaimer");
                    String licenceUrl = I18nUtil.getMessage("jsp.sherpa.license-url");
                    this.license = licenceText;
                    this.licenseURL = licenceUrl;
                    this.disclaimer = disclaimerText;

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
        }
    }

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

    public String getLicense()
    {
        return license;
    }

    public String getLicenseURL()
    {
        return licenseURL;
    }

    public String getDisclaimer()
    {
        return disclaimer;
    }

    public List<SHERPAJournal> getJournals()
    {
        return journals;
    }

    public SHERPASystemMetadata getMetadata() {
        return metadata;
    }
}
