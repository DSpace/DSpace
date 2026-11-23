/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.openpolicyfinder.v2;

import java.io.Serializable;
import java.util.List;

/**
 * Plain java representation of an Open Policy Finder Permitted Version object,
 * based on Open Policy Finder API responses.
 *
 * In a Open Policy Finder search for journal deposit policies, this data is contained within a publisher policy.
 * Each permitted version is for a particular article version (e.g. submitted, accepted, published) and contains:
 *
 * <ul>
 *   <li>A list of general conditions / terms for deposit of this version of work</li>
 *   <li>A list of allowed locations (e.g. institutional repository, personal homepage, non-commercial repository)</li>
 *   <li>A list of prerequisite conditions for deposit (e.g. attribution, linking to published version)</li>
 *   <li>A list of required licenses for the deposited work (e.g. CC-BY-NC)</li>
 *   <li>Embargo requirements, if any</li>
 * </ul>
 *
 * This class also has some helper data for labels, which can be used with i18n
 * when displaying policy information.
 *
 * @see OpenPolicyFinderPublisherPolicy
 */
public class OpenPolicyFinderPermittedVersion implements Serializable {

    private static final long serialVersionUID = 4992181606327727442L;

    // Version (submitted, accepted, published)
    private String articleVersion;

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
    private OpenPolicyFinderEmbargo embargo;

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

    public OpenPolicyFinderEmbargo getEmbargo() {
        return embargo;
    }

    public void setEmbargo(OpenPolicyFinderEmbargo embargo) {
        this.embargo = embargo;
    }

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

}
