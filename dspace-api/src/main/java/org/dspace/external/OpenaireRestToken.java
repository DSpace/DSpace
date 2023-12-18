/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

/**
 * Openaire rest API token to be used when grabbing an accessToken.<br/>
 * Based on https://develop.openaire.eu/basic.html
 * 
 * @author paulo-graca
 *
 */
public class OpenaireRestToken {

    /**
     * Stored access token
     */
    private String accessToken;

    /**
     * Stored expiration period (in seconds)
     */
    private Long accessTokenExpiration = 0L;

    /**
     * Stores the grabbed token
     * 
     * @param accessToken
     * @param expiresIn
     */
    public OpenaireRestToken(String accessToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.setExpirationDate(expiresIn);
    }

    /**
     * Returns the stored
     * 
     * @return String with the stored token
     */
    public String getToken() {
        return this.accessToken;
    }

    /**
     * If the existing token has an expiration date and if it is at a minute of
     * expiring
     * 
     * @return
     */
    public boolean isValidToken() {
        if (this.accessToken == null) {
            return false;
        }

        return ((accessTokenExpiration - (60 * 1000)) > System.currentTimeMillis());
    }

    private void setExpirationDate(Long expiresIn) {
        accessTokenExpiration = System.currentTimeMillis() + (expiresIn * 1000L);
    }
}
