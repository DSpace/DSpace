/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

/**
 * Client that exchanges a refresh token for an access token.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public interface OAuth2TokenClient {

    /**
     * Request a new access token using the refresh-token grant.
     *
     * @param settings     the provider settings
     * @param refreshToken the refresh token to exchange
     * @return the new token
     * @throws OAuth2Exception when the request fails or the response is invalid
     */
    OAuth2Token requestToken(OAuth2Settings settings, String refreshToken);
}
