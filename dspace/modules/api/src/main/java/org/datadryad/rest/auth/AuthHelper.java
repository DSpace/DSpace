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
    private final OAuthTokenStorageInterface tokenStorage;
    private final AuthorizationStorageInterface authzStorage;

    public AuthHelper(OAuthTokenStorageInterface tokenStorage, AuthorizationStorageInterface authzStorage) {
        this.tokenStorage = tokenStorage;
        this.authzStorage = authzStorage;
    }

    public static EPerson getEPerson(Integer id) throws SQLException {
        if(id == OAuthToken.INVALID_PERSON_ID) {
            return null;
        }
        Context context = new Context();
        EPerson eperson = EPerson.find(context, id);
        context.abort();
        return eperson;
    }

    public Integer getEPersonIdFromToken(String accessToken) {
        try {
            OAuthToken oAuthToken = tokenStorage.getToken(accessToken);
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


    public EPersonUserPrincipal getPrincipalFromToken(String accessToken) {
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

    public static void throwExceptionResponse(Throwable throwable, Response.Status status, String responseString) throws WebApplicationException {
        Response.ResponseBuilder builder;
        builder = Response.status(status).entity(responseString);
        if(throwable == null) {
            throw new WebApplicationException(builder.build());
        } else {
            throw new WebApplicationException(throwable, builder.build());
        }
    }

    public Boolean isAuthorized(AuthorizationTuple tuple) {
        if(tuple == null) {
            return Boolean.FALSE;
        }
        try {
            return authzStorage.isAuthorized(tuple);
        } catch(StorageException ex) {
            log.error("Exception checking auth", ex);
            return Boolean.FALSE;
        }
    }
}
