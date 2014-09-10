/*
 */
package org.datadryad.rest.auth;

import org.datadryad.rest.models.OAuthToken;

/**
 * Encapsulates the 3 things we need to check to determine if a request is authorized
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AuthorizationTuple {
    final Integer ePersonId;
    final String httpMethod;
    final String path;

    public AuthorizationTuple(Integer ePersonId, String httpMethod, String path) {
        this.ePersonId = ePersonId;
        this.httpMethod = httpMethod;
        this.path = path;
    }

    public final Boolean isComplete() {
        if(this.ePersonId == null) {
            return false;
        }
        if(this.httpMethod == null) {
            return false;
        }
        if(this.path == null) {
            return false;
        }
        return true;

    }
}
