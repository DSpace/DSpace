/*
 */
package org.datadryad.rest.auth;

import java.sql.SQLException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.OAuthToken;
import org.datadryad.rest.storage.AuthorizationStorageInterface;
import org.datadryad.rest.storage.OAuthTokenStorageInterface;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.rdbms.AuthorizationDatabaseStorageImpl;
import org.datadryad.rest.storage.rdbms.OAuthTokenDatabaseStorageImpl;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AuthHelper {
    private static final Logger log = Logger.getLogger(AuthHelper.class);

    static EPerson getEPerson(Integer id) throws SQLException {
        if(id == OAuthToken.INVALID_PERSON_ID) {
            return null;
        }
        Context context = new Context();
        EPerson eperson = EPerson.find(context, id);
        return eperson;
    }

    static Integer getEPersonIdFromToken(String accessToken) {
        // TODO: switch storage to provider.
        OAuthTokenStorageInterface storage = new OAuthTokenDatabaseStorageImpl();
        try {
            OAuthToken oAuthToken = storage.getToken(accessToken);
            if(oAuthToken == null || !oAuthToken.isValid()) {
                // Token not found or invalid
                // TODO: Handle expirations separately;
                return OAuthToken.INVALID_PERSON_ID;
            } else {
                return oAuthToken.getEPersonId();
            }
        } catch (StorageException ex) {
            log.error("Exception getting Token", ex);
            return OAuthToken.INVALID_PERSON_ID;
        }
    }


    static EPersonUserPrincipal getPrincipalFromToken(String accessToken) {
        EPersonUserPrincipal principal = null;
        if(accessToken == null) {
            return null;
        }
        try {
            Integer ePersonId = getEPersonIdFromToken(accessToken);
            EPerson person = getEPerson(ePersonId);
            if(person != null) {
                principal = new EPersonUserPrincipal(person);
            }
        } catch (SQLException ex) {
            log.error("SQL Exception getting EPerson", ex);
        }
        return principal;
    }

    static void throwExceptionResponse(Throwable throwable, Response.Status status, String responseString) throws WebApplicationException {
        Response.ResponseBuilder builder;
        builder = Response.status(status).entity(responseString);
        if(throwable == null) {
            throw new WebApplicationException(builder.build());
        } else {
            throw new WebApplicationException(throwable, builder.build());
        }
    }

    static Boolean isAuthorized(AuthorizationTuple tuple) {
        if(tuple == null) {
            return Boolean.FALSE;
        }
        if(!tuple.isComplete()) {
            return Boolean.FALSE;
        }
        if(tuple.ePersonId == OAuthToken.INVALID_PERSON_ID) {
            return Boolean.FALSE;
        }
        // TODO: switch storage to provider.
        AuthorizationStorageInterface storage = new AuthorizationDatabaseStorageImpl();
        try {
            return storage.isAuthorized(tuple);
        } catch(StorageException ex) {
            log.error("Exception checking auth", ex);
            return Boolean.FALSE;
        }
    }
}
