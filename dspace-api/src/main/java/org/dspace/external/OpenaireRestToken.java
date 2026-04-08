/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.time.Instant;

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
     * @param accessToken the access token string
     * @param expiresIn   the token expiration period in seconds
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
     * @return true if the token is valid and not near expiration, false otherwise
     */
    public boolean isValidToken() {
        if (this.accessToken == null) {
            return false;
        }

        return ((accessTokenExpiration - (60 * 1000)) > Instant.now().toEpochMilli());
    }

    private void setExpirationDate(Long expiresIn) {
        accessTokenExpiration = Instant.now().toEpochMilli() + (expiresIn * 1000L);
    }
}
