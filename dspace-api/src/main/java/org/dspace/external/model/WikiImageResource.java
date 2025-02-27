/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.model;

import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * Representation of a Wikimedia image resource from Wikimedia commons
 *
 * @author Kim Shepherd
 */
public class WikiImageResource {

    // Private members

    // image title
    private String title;

    // image description
    private String imageDescription;

    // Licence name and URL (eg. CC-BY-SA, https://creativecommons.org/licenses/by-sa/3.0/)
    private String licenseShortName;
    private String licenseUrl;

    // Is attribution required?
    private boolean attributionRequired;

    // HTML snippet of attribution to include - 'artist' seem to be best, followed by optional 'attribution' field
    private String attribution;
    private String artist;

    // Is the work copyrighted?
    private boolean copyrighted;

    // Credit flag (eg. 'own work')
    private String credit;

    // Final attribution snippet for use in display - this might be artist, attribution, a combo, or a
    // constructed string from other fields depending on what information is available
    private String attributionSnippet;

    // Set up logger
    private static final Logger log = LogManager.getLogger();

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageDescription() {
        return imageDescription;
    }

    public void setImageDescription(String imageDescription) {
        this.imageDescription = imageDescription;
    }

    public String getLicenseShortName() {
        return licenseShortName;
    }

    public void setLicenseShortName(String licenseShortName) {
        this.licenseShortName = licenseShortName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public boolean isAttributionRequired() {
        return attributionRequired;
    }

    public void setAttributionRequired(boolean attributionRequired) {
        this.attributionRequired = attributionRequired;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public boolean isCopyrighted() {
        return copyrighted;
    }

    public void setCopyrighted(boolean copyrighted) {
        this.copyrighted = copyrighted;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public String getAttributionSnippet() {
        return attributionSnippet;
    }

    public void setAttributionSnippet(String attributionSnippet) {
        this.attributionSnippet = attributionSnippet;
    }

    /**
     * Parse a wikimedia API response and return constructed resource
     * @param resultEntity
     * @return
     */
    public static WikiImageResource parseWikimediaResponse(String resultEntity) {
        if (null != resultEntity) {
            // Construct org.json.JSONObject from result string
            org.json.JSONObject json = new JSONObject(resultEntity);
            // Construct and return new WikiImageResource from JSON
            return new WikiImageResource(json);
        }
        return null;
    }

    /**
     * Construct new WikiImageResource from JSON retrieved from API
     *
     * @param json the JSON object to parse
     */
    public WikiImageResource(JSONObject json) throws JSONException {
        // Parse JSON and set members for artist, attribution information
        parseJson(json);

        // Set up attribution snippet for display. Based on wikimedia usage documentation at:
        // https://commons.wikimedia.org/wiki/Commons:Credit_line#CC-BY_and_CC-BY-SA_licenses
        String attributionSnippet = "";

        // If copyrighted, prepend with copyright sign as per
        if (isCopyrighted()) {
            attributionSnippet += "© ";
        }

        if (getArtist() != null) {
            // Get artist attribution string eg.
            attributionSnippet += getArtist();
        } else if (getAttribution() != null) {
            // Get attribution string instead, eg.
            attributionSnippet += getAttribution();
        } else {
            // If we don't have artist information, I guess we just continue, and probably remove copyright?
            attributionSnippet = "";
        }

        if (!attributionSnippet.isEmpty()) {
            attributionSnippet += " | ";
        }

        String wikimediaUrl = "https://commons.wikimedia.org/wiki/" + getTitle().replaceAll(" ", "_");
        attributionSnippet += "<a target='_blank' href='" + wikimediaUrl + "'>Wikimedia Commons</a>";

        // If we have licence information, set that
        if (getLicenseShortName() != null) {
            String license = "";
            if (getLicenseUrl() != null) {
                license = " | <a target='_blank' href='" + getLicenseUrl() + "'>" + getLicenseShortName() + "</a>";
            } else {
                license = " | " + getLicenseShortName();
            }
            attributionSnippet += license;
        }

        setAttributionSnippet(attributionSnippet);
    }

    /**
     * Parses a JSON object to populate this Wikimedia resource object.
     *
     * @param json the JSON object to parse
     */
    private void parseJson(JSONObject json) {
       if (json.has("query")) {
           JSONObject query = json.getJSONObject("query");
           if (query.has("pages")) {
               JSONObject pages = query.getJSONObject("pages");
               // The pages object is a list of IDs rather than a name, but we only expect one
               Iterator pagesKeys = pages.keys();
               String key;
               key = (String)pagesKeys.next();
               if (pages.has(key)) {
                   JSONObject page = pages.getJSONObject(key);
                   if (page.has("title")) {
                       setTitle(page.getString("title"));
                   }
                   // Set image info
                   setImageInfo(page);
               }
           }
       }
    }

    /**
     * Sets the metadata and attribution details for a given Wikimedia image resource
     *
     * @param page The JSON object representing a Wikimedia image.
     */
    private void setImageInfo(JSONObject page) {
        if (page.has("imageinfo")) {
            JSONArray imageinfo = page.getJSONArray("imageinfo");
            if (imageinfo.length() > 0) {
                JSONObject info = imageinfo.getJSONObject(0);
                if (info.has("extmetadata")) {
                    JSONObject extmetadata = info.getJSONObject("extmetadata");
                    // Set License Short Name (eg. Public Domain)
                    if (extmetadata.has("LicenseShortName")) {
                        JSONObject licenseShortName = extmetadata.getJSONObject("LicenseShortName");
                        if (licenseShortName.has("value")) {
                            setLicenseShortName(licenseShortName.getString("value"));
                        }
                    }
                    // Set License URL (eg. URL to CC licence)
                    if (extmetadata.has("LicenseUrl")) {
                        JSONObject licenseUrl = extmetadata.getJSONObject("LicenseUrl");
                        if (licenseUrl.has("value")) {
                            setLicenseUrl(licenseUrl.getString("value"));
                        }
                    }
                    // Set Image Description (html) eg.
                    // <a href=\"//commons.wikimedia.org/wiki/Mark_Twain\" title=\"Mark Twain\">
                    //        Mark Twain
                    // </a> photo portrait.
                    if (extmetadata.has("ImageDescription")) {
                        JSONObject imageDescription = extmetadata.getJSONObject("ImageDescription");
                        if (imageDescription.has("value")) {
                            setImageDescription(imageDescription.getString("value"));
                        }
                    }
                    // Set AttributionRequired node - a simple true or false flag
                    if (extmetadata.has("AttributionRequired")) {
                        JSONObject attributionRequired =
                                extmetadata.getJSONObject("AttributionRequired");
                        if (attributionRequired.has("value")) {
                            setAttributionRequired(attributionRequired.getBoolean("value"));
                        }
                    }
                    // Set Attribution, if it exists
                    if (extmetadata.has("Attribution")) {
                        JSONObject attribution = extmetadata.getJSONObject("Attribution");
                        if (attribution.has("value")) {
                            setAttribution(attribution.getString("value"));
                        }
                    }
                    // Set Artist, if it exists
                    if (extmetadata.has("Artist")) {
                        JSONObject artist = extmetadata.getJSONObject("Artist");
                        if (artist.has("value")) {
                            setArtist(artist.getString("value"));
                        }
                    }
                    // Set Copyrighted indicator
                    if (extmetadata.has("Copyrighted")) {
                        JSONObject copyrighted = extmetadata.getJSONObject("Copyrighted");
                        if (copyrighted.has("value")) {
                            setCopyrighted(copyrighted.getBoolean("value"));
                        }
                    }
                    // Set Credit indicator
                    if (extmetadata.has("Credit")) {
                        JSONObject credit = extmetadata.getJSONObject("Credit");
                        if (credit.has("value")) {
                            setCredit(credit.getString("value"));
                        }
                    }
                }
            }
        }
    }

    /**
     * @return string representation of Wikimedia image resource
     */
    @Override
    public String toString() {
        return "WikiImageResource{" +
            "title='" + title + '\'' +
            ", imageDescription='" + imageDescription + '\'' +
            ", licenseShortName='" + licenseShortName + '\'' +
            ", licenseUrl='" + licenseUrl + '\'' +
            ", attributionRequired=" + attributionRequired +
            ", attribution='" + attribution + '\'' +
            ", artist='" + artist + '\'' +
            ", copyrighted=" + copyrighted +
            ", credit='" + credit + '\'' +
            ", attributionSnippet='" + attributionSnippet + '\'' +
            '}';
    }
}
