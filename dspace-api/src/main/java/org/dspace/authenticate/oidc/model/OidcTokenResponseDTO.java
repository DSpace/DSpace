/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.oidc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class map the response from and OpenID Connect token endpoint.
 * {@link https://openid.net/specs/openid-connect-core-1_0.html}
 *
 * Response example:
 *
 * { "access_token": "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCIsIm9yZ...", "id_token":
 * "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGki...", "token_type": "bearer",
 * "expires_in": 28800, "scope": "pgc-role email openid profile" }
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class OidcTokenResponseDTO {

    /**
     * The access token release by the authorization server this is the most
     * relevant item, because it allow the server to access to the user resources as
     * defined in the scopes {@link https://tools.ietf.org/html/rfc6749#section-1.4}
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * The id token as defined in the OpenID connect standard
     * {@link https://openid.net/specs/openid-connect-core-1_0.html#IDToken}
     */
    @JsonProperty("id_token")
    private String idToken;

    /**
     * The refresh token as defined in the OAuth standard
     * {@link https://tools.ietf.org/html/rfc6749#section-1.5}
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * It will be "bearer"
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * The expiration timestamp in millis
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * List of scopes {@link https://tools.ietf.org/html/rfc6749#section-3.3}
     */
    @JsonProperty("scope")
    private String scope;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

}