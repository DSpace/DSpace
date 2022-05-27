/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.oidc;

import java.util.Map;

import org.dspace.authenticate.oidc.model.OidcTokenResponseDTO;

/**
 * Client to interact with the configured OIDC provider.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OidcClient {

    /**
     * Exchange the authorization code for a 3-legged access token. The
     * authorization code expires upon use.
     *
     * @param  code                the authorization code
     * @return                     the OIDC token
     * @throws OidcClientException if some error occurs during the exchange
     */
    OidcTokenResponseDTO getAccessToken(String code) throws OidcClientException;

    /**
     * Retrieve the info related to the user associated with the given accessToken
     * from the user info endpoint.
     *
     * @param  accessToken         the access token
     * @return                     a map with the user infos
     * @throws OidcClientException if some error occurs during the exchange
     */
    Map<String, Object> getUserInfo(String accessToken) throws OidcClientException;

}
