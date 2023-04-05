/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

import java.io.Serializable;

/**
 * Plain java representation of a SHERPA Publisher object, based on SHERPA API v2 responses.
 *
 * In a search for SHERPA journal deposit policy, this publisher object will appear in a list of publishers
 * from the journal object, and as a single publisher member for the primary/current publisher of the journal.
 * In a search for SHERPA publisher information, this object will appear in a list of publishers from the main
 * SHERPA Publisher Response object
 *
 * @see SHERPAJournal
 * @see SHERPAPublisherResponse
 */
public class SHERPAPublisher implements Serializable {
    private String name = null;
    private String relationshipType;
    private String country;
    private String uri = null;
    private String identifier = null;
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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
