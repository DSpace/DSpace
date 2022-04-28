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
 * Plain java representation of a SHERPA System Metadata object, based on SHERPA API v2 responses.
 *
 * This data is included in both journal deposit policy and publisher searches and contains basic metadata
 * about the SHERPA record and API response, eg. creation and modification dates, internal IDs, permissions, etc.
 *
 * @see SHERPAResponse
 * @see SHERPAPublisherResponse
 *
 * @author Kim Shepherd
 */
public class SHERPASystemMetadata implements Serializable {

    private int id;
    private String uri;
    private String dateCreated;
    private String dateModified;
    private boolean isPubliclyVisible = false;
    private boolean inDOAJ = false;

    public SHERPASystemMetadata() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    public boolean isPubliclyVisible() {
        return isPubliclyVisible;
    }

    public void setPubliclyVisible(boolean publiclyVisible) {
        isPubliclyVisible = publiclyVisible;
    }

    public boolean isInDOAJ() {
        return inDOAJ;
    }

    public void setInDOAJ(boolean inDOAJ) {
        this.inDOAJ = inDOAJ;
    }
}
