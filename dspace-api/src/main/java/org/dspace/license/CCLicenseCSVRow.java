/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

/**
 * Represents a single row in the Creative Commons license CSV configuration.
 *
 * <p>This class is used as an in-memory representation of the CSV schema and
 * acts as the source of truth for CC license metadata loaded at startup.</p>
 *
 * <p>Each instance maps directly to one row in the CSV file and is used to
 * build lookup indexes for license resolution (version, unit, jurisdiction).</p>
 */
public class CCLicenseCSVRow {
    private String category;
    private String version;
    private String unit;
    private String jurisdiction;
    private String url;
    private String identifier;
    private String title;
    private String entryPoint;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }
}
