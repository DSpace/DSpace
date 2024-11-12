/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * This class represents a DTO that will be used to share a submission link. It will be used to return the share link
 * to the user in the UI.
 *
 * @author Milan Majchrak (dspace at dataquest.sk)
 */
public class ShareSubmissionLinkDTO {

    private String shareLink;

    public ShareSubmissionLinkDTO() { }

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }
}
