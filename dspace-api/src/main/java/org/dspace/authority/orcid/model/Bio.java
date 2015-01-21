/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority.orcid.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Bio {

    protected String orcid;

    protected BioName name;

    protected String country;

    protected Set<String> keywords;

    protected Set<BioExternalIdentifier> bioExternalIdentifiers;

    protected Set<BioResearcherUrl> researcherUrls;

    protected String biography;

    public Bio() {
        this.name = new BioName();
        keywords = new LinkedHashSet<String>();
        bioExternalIdentifiers = new LinkedHashSet<BioExternalIdentifier>();
        researcherUrls = new LinkedHashSet<BioResearcherUrl>();
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public BioName getName() {
        return name;
    }

    public void setName(BioName name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void addKeyword(String keyword) {
        this.keywords.add(keyword);
    }

    public Set<BioExternalIdentifier> getBioExternalIdentifiers() {
        return bioExternalIdentifiers;
    }

    public void addExternalIdentifier(BioExternalIdentifier externalReference) {
        bioExternalIdentifiers.add(externalReference);
    }

    public Set<BioResearcherUrl> getResearcherUrls() {
        return researcherUrls;
    }

    public void addResearcherUrl(BioResearcherUrl researcherUrl) {
        researcherUrls.add(researcherUrl);
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    @Override
    public String toString() {
        return "Bio{" +
                "orcid='" + orcid + '\'' +
                ", name=" + name +
                ", country='" + country + '\'' +
                ", keywords=" + keywords +
                ", bioExternalIdentifiers=" + bioExternalIdentifiers +
                ", researcherUrls=" + researcherUrls +
                ", biography='" + biography + '\'' +
                '}';
    }
}

