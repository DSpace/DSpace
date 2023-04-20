/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google;

import java.util.Objects;

import org.springframework.util.Assert;

/**
 * This is a dataholder class for an individual event to be sent to Google Analaytics
 *
 * @author April Herron
 */
public final class GoogleAnalyticsEvent {

    private final String clientId;
    private final String userIp;
    private final String userAgent;
    private final String documentReferrer;
    private final String documentPath;
    private final String documentTitle;
    private final long time;

    public GoogleAnalyticsEvent(String clientId, String userIp, String userAgent, String documentReferrer,
        String documentPath, String documentTitle) {
        Assert.notNull(clientId, "A client id is required to create a Google Analytics event");
        this.clientId = clientId;
        this.userIp = userIp;
        this.userAgent = userAgent;
        this.documentReferrer = documentReferrer;
        this.documentPath = documentPath;
        this.documentTitle = documentTitle;
        this.time = System.currentTimeMillis();
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserIp() {
        return userIp;
    }

    public String getUserAgent() {
        return userAgent != null ? userAgent : "";
    }

    public String getDocumentReferrer() {
        return documentReferrer != null ? documentReferrer : "";
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public long getTime() {
        return time;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, documentPath, documentReferrer, documentTitle, time, userAgent, userIp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GoogleAnalyticsEvent other = (GoogleAnalyticsEvent) obj;
        return Objects.equals(clientId, other.clientId) && Objects.equals(documentPath, other.documentPath)
            && Objects.equals(documentReferrer, other.documentReferrer)
            && Objects.equals(documentTitle, other.documentTitle) && time == other.time
            && Objects.equals(userAgent, other.userAgent) && Objects.equals(userIp, other.userIp);
    }

    @Override
    public String toString() {
        return "GoogleAnalyticsEvent [clientId=" + clientId + ", userIp=" + userIp + ", userAgent=" + userAgent
            + ", documentReferrer=" + documentReferrer + ", documentPath=" + documentPath + ", documentTitle="
            + documentTitle + ", time=" + time + "]";
    }

}
