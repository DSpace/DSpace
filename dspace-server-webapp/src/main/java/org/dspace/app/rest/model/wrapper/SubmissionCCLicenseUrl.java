/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.wrapper;

public class SubmissionCCLicenseUrl {

    private String url;
    private String id;

    public SubmissionCCLicenseUrl(String url, String id) {
        this.url = url;
        this.id = id;
    }

    /**
     * Generic getter for the url
     * @return the url value of this SubmissionCCLicenseUrl
     */
    public String getUrl() {
        return url;
    }

    /**
     * Generic setter for the url
     * @param url   The url to be set on this SubmissionCCLicenseUrl
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Generic getter for the id
     * @return the id value of this SubmissionCCLicenseUrl
     */
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this SubmissionCCLicenseUrl
     */
    public void setId(String id) {
        this.id = id;
    }
}
