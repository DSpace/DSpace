/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google;

/**
 * This is a dataholder class for an individual event to be sent to Google Analaytics
 *
 * @author April Herron
 */
public class GoogleAnalyticsEvent {

    private String cid;
    private String uip;
    private String ua;
    private String dr;
    private String dp;
    private String dt;
    private long time;

    GoogleAnalyticsEvent(String cid, String uip, String ua, String dr, String dp, String dt, long time) {
        this.cid = cid;
        this.uip = uip;
        this.ua = ua;
        this.dr = dr;
        this.dp = dp;
        this.dt = dt;
        this.time = time;
    }

    /**
     * Return Client ID
     */
    public String getCid() {
        return cid;
    }

    /**
     * Set Client ID
     */
    public void setCid(String cid) {
        this.cid = cid;
    }

    /**
     * Return User IP
     */
    public String getUip() {
        return uip;
    }

    /**
     * Set User IP
     */
    public void setUip(String uip) {
        this.uip = uip;
    }

    /**
     * Returns User Agent
     */
    public String getUa() {
        if (ua == null) {
            return "";
        } else {
            return ua;
        }
    }

    /**
     * Set User Agent
     */
    public void setUa(String ua) {
        this.ua = ua;
    }

    /**
     * Return Document Referrer
     */
    public String getDr() {
        if (dr == null) {
            return "";
        } else {
            return dr;
        }
    }

    /**
     * Set Document Referrer
     */
    public void setDr(String dr) {
        this.dr = dr;
    }

    /**
     * Return Document Path
     */
    public String getDp() {
        return dp;
    }

    /**
     * Set Document Path
     */
    public void setDp(String dp) {
        this.dp = dp;
    }

    /**
     * Return Document Title
     */
    public String getDt() {
        return dt;
    }

    /**
     * Set Document Title
     */
    public void setDt(String dt) {
        this.dt = dt;
    }

    /**
     * Return Time of event
     */
    public long getTime() {
        return time;
    }

    /**
     * Set Time of event
     */
    public void setTime(long time) {
        this.time = time;
    }
}
