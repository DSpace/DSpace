/*
 */
package org.datadryad.rest.models;

import java.util.Date;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class OAuthToken {
    public static final Integer INVALID_PERSON_ID = -1;
    
    private final Integer epersonId;
    private final String tokenString;
    private final Date expires;

    public OAuthToken(Integer epersonId, String token, Date expires) {
        this.epersonId = epersonId;
        this.tokenString = token;
        this.expires = expires;
    }

    /**
     * Tokens are valid if they have a valid eperson, token string, and expiry
     * @return True if above criteria match
     */
    public Boolean isValid() {
        if(expires == null || tokenString == null || epersonId == null || epersonId == INVALID_PERSON_ID) {
            return Boolean.FALSE;
        } else {
            return expires.after(new Date());
        }
    }

    /**
     * Gets the EPerson ID of the token.
     * @return ID for the DSpace EPerson table
     */
    public Integer getEPersonId() {
        return epersonId;
    }

}
