/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

/**
 * This model class represent reCaptcha Google credentials
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class CaptchaSettings {

    private String site;
    private String secret;
    private float threshold;
    private String siteVerify;
    private String captchaVersion;

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public String getSiteVerify() {
        return siteVerify;
    }

    public void setSiteVerify(String siteVerify) {
        this.siteVerify = siteVerify;
    }

    public String getCaptchaVersion() {
        return captchaVersion;
    }

    public void setCaptchaVersion(String captchaVersion) {
        this.captchaVersion = captchaVersion;
    }
}
