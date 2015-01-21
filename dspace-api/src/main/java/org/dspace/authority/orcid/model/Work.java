/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.model;

import java.util.Set;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Work {

    private WorkTitle workTitle;
    private String description;
    private Citation citation;
    private WorkType workType;
    private String publicationDate;
    private WorkExternalIdentifier workExternalIdentifier;
    private String url;
    private Set<Contributor> contributors;
    private String workSource;

    public WorkTitle getWorkTitle() {
        return workTitle;
    }

    public void setWorkTitle(WorkTitle workTitle) {
        this.workTitle = workTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Citation getCitation() {
        return citation;
    }

    public void setCitation(Citation citation) {
        this.citation = citation;
    }

    public WorkType getWorkType() {
        return workType;
    }

    public void setWorkType(WorkType workType) {
        this.workType = workType;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public WorkExternalIdentifier getWorkExternalIdentifier() {
        return workExternalIdentifier;
    }

    public void setWorkExternalIdentifier(WorkExternalIdentifier workExternalIdentifier) {
        this.workExternalIdentifier = workExternalIdentifier;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(Set<Contributor> contributors) {
        this.contributors = contributors;
    }

    public String getWorkSource() {
        return workSource;
    }

    public void setWorkSource(String workSource) {
        this.workSource = workSource;
    }

    @Override
    public String toString() {
        return "Work{" +
                "workTitle=" + workTitle +
                ", description='" + description + '\'' +
                ", citation=" + citation +
                ", workType=" + workType +
                ", publicationDate='" + publicationDate + '\'' +
                ", workExternalIdentifier=" + workExternalIdentifier +
                ", url='" + url + '\'' +
                ", contributors=" + contributors +
                ", workSource='" + workSource + '\'' +
                '}';
    }
}
