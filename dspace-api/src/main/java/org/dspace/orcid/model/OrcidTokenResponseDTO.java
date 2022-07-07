/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * This class map the response from and ORCID token endpoint.
 *
 * Response example:
 *
 * {
 *  "access_token":"f5af9f51-07e6-4332-8f1a-c0c11c1e3728",
 *  "token_type":"bearer",
 *  "refresh_token":"f725f747-3a65-49f6-a231-3e8944ce464d",
 *  "expires_in":631138518,
 *  "scope":"/read-limited",
 *  "name":"Sofia Garcia",
 *  "orcid":"0000-0001-2345-6789"
 * }
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science.it)
 *
 */
public class OrcidTokenResponseDTO {

    /**
     * The access token release by the authorization server this is the most
     * relevant item, because it allow the server to access to the user resources as
     * defined in the scopes.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * The refresh token as defined in the OAuth standard.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * It will be "bearer".
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * The expiration timestamp in millis.
     */
    @JsonProperty("expires_in")
    private int expiresIn;

    /**
     * List of scopes.
     */
    private String scope;

    /**
     * The ORCID user name.
     */
    private String name;

    /**
     * The ORCID user id.
     */
    private String orcid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @JsonIgnore
    public String[] getScopeAsArray() {
        return StringUtils.isEmpty(getScope()) ? new String[] {} : getScope().split(" ");
    }
}
