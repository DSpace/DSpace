/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

public class SHERPAPublisher {
    private String name;
    private String relationshipType;
    private String country;
    private String uri;
    private int publicationCount;

    // this is not technically in the same place in SHERPA data model but it makes more sense to apply it here
    // is it is treated as a 'special case' - just for printing links to paid OA access policies
    private String paidAccessDescription;
    private String paidAccessUrl;

    public SHERPAPublisher() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getPublicationCount() {
        return publicationCount;
    }

    public void setPublicationCount(int publicationCount) {
        this.publicationCount = publicationCount;
    }

    public String getPaidAccessDescription() {
        return paidAccessDescription;
    }

    public void setPaidAccessDescription(String paidAccessDescription) {
        this.paidAccessDescription = paidAccessDescription;
    }

    public String getPaidAccessUrl() {
        return paidAccessUrl;
    }

    public void setPaidAccessUrl(String paidAccessUrl) {
        this.paidAccessUrl = paidAccessUrl;
    }

}
