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
        this.cid = cid;                      // Client ID
        this.uip = uip;                      // User IP
        this.ua = ua;                        // User Agent
        this.dr = dr;                        // Document Referrer
        this.dp = dp;                        // Document Path
        this.dt = dt;                        // Document Title
        this.time = time;                    // Time of event
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getUip() {
        return uip;
    }

    public void setUip(String uip) {
        this.uip = uip;
    }

    public String getUa() {
        if (ua == null) {
            return "";
        } else {
            return ua;
        }
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public String getDr() {
        if (dr == null) {
            return "";
        } else {
            return dr;
        }
    }

    public void setDr(String dr) {
        this.dr = dr;
    }

    public String getDp() {
        return dp;
    }

    public void setDp(String dp) {
        this.dp = dp;
    }

    public String getDt() {
        return dt;
    }

    public void setDt(String dt) {
        this.dt = dt;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}