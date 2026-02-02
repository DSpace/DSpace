/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.wrapper;

/**
 * This class represents a model implementation for {@link org.dspace.app.rest.model.SubmissionCCLicenseUrlRest}
 * This will simply store a url and an id. it'll be used to create an object with these variables out of information
 * that came from the back-end. This object will then be used in the
 * {@link org.dspace.app.rest.converter.SubmissionCCLicenseUrlConverter} to turn it into its REST object
 */
public class SubmissionCCLicenseUrl {

    /**
     * The url for this object
     */
    private String url;
    /**
     * The id for this object
     */
    private String id;

    /**
     * Default constructor with two parameters, url and id
     * @param url   The url of this object
     * @param id    The id of this object
     */
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
