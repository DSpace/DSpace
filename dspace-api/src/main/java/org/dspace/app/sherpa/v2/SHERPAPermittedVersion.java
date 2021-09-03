/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

import java.util.List;

public class SHERPAPermittedVersion {

    // Version (submitted, accepted, published)
    private String articleVersion;

    // Version label
    private String articleVersionLabel;

    // Option number
    private int option;

    // General conditions
    private List<String> conditions;
    // Prerequisites (eg. if required by funder)
    private List<String> prerequisites;
    // Allowed locations
    private List<String> locations;
    // Required license(s)
    private List<String> licenses;
    // Embargo
    private SHERPAEmbargo embargo;

    protected class SHERPAEmbargo {
        String units;
        int amount;
    }

    public String getArticleVersion() {
        return articleVersion;
    }

    public void setArticleVersion(String articleVersion) {
        this.articleVersion = articleVersion;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<String> licenses) {
        this.licenses = licenses;
    }

    public SHERPAEmbargo getEmbargo() {
        return embargo;
    }

    public void setEmbargo(SHERPAEmbargo embargo) {
        this.embargo = embargo;
    }

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public String getArticleVersionLabel() {
        return articleVersionLabel;
    }

    public void setArticleVersionLabel(String articleVersionLabel) {
        this.articleVersionLabel = articleVersionLabel;
    }
}
