/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

import java.util.List;
import java.util.Map;

public class SHERPAPublisherPolicy {

    private int id;
    private boolean openAccessPermitted;
    private String uri;
    private String internalMoniker;
    private List<SHERPAPermittedVersion> permittedVersions;
    private Map<String, String> urls;
    private boolean openAccessProhibited;
    private int publicationCount;

    // The legacy "can" / "cannot" indicators
    private String preArchiving = "cannot";
    private String postArchiving = "cannot";
    private String pubArchiving = "cannot";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isOpenAccessPermitted() {
        return openAccessPermitted;
    }

    public void setOpenAccessPermitted(boolean openAccessPermitted) {
        this.openAccessPermitted = openAccessPermitted;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getInternalMoniker() {
        return internalMoniker;
    }

    public void setInternalMoniker(String internalMoniker) {
        this.internalMoniker = internalMoniker;
    }

    public List<SHERPAPermittedVersion> getPermittedVersions() {
        return permittedVersions;
    }

    public void setPermittedVersions(List<SHERPAPermittedVersion> permittedVersions) {
        this.permittedVersions = permittedVersions;
    }

    public Map<String, String> getUrls() {
        return urls;
    }

    public void setUrls(Map<String, String> urls) {
        this.urls = urls;
    }

    public boolean isOpenAccessProhibited() {
        return openAccessProhibited;
    }

    public void setOpenAccessProhibited(boolean openAccessProhibited) {
        this.openAccessProhibited = openAccessProhibited;
    }

    public int getPublicationCount() {
        return publicationCount;
    }

    public void setPublicationCount(int publicationCount) {
        this.publicationCount = publicationCount;
    }

    public String getPreArchiving() {
        return preArchiving;
    }

    public void setPreArchiving(String preArchiving) {
        this.preArchiving = preArchiving;
    }

    public String getPostArchiving() {
        return postArchiving;
    }

    public void setPostArchiving(String postArchiving) {
        this.postArchiving = postArchiving;
    }

    public String getPubArchiving() {
        return pubArchiving;
    }

    public void setPubArchiving(String pubArchiving) {
        this.pubArchiving = pubArchiving;
    }
}
