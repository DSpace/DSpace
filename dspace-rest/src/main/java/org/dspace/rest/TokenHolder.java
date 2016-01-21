/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.sql.SQLException;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.rest.common.User;

/**
 * This class provide token generation, token holding and logging user into rest
 * api. For login use method login with class org.dspace.rest.common.User. If
 * you want to be deleted from holder, use method for logout.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 */
public class TokenHolder
{


    private static final Logger log = Logger.getLogger(TokenHolder.class);

    public static String TOKEN_HEADER = "rest-dspace-token";

    /**
     * Collection holding the auth-token, and the corresponding EPerson's UUID
     */
    private static BiMap<String, UUID> tokenPersons = HashBiMap.create();

    /**
     * Login user into rest api. It check user credentials if they are okay.
     * 
     * @param user
     *            User which will be logged into rest api.
     * @return Returns generated token, which must be used in request header
     *         under rest-api-token. If password is bad or user does not exist,
     *         it returns NULL.
     * @throws WebApplicationException
     *             It is thrown by SQLException if user could not be read from
     *             database. And by Authorization exception if context has not
     *             permission to read eperson.
     */
    public static String login(User user) throws WebApplicationException
    {
        AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
        EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();

        org.dspace.core.Context context = null;
        String token = null;

        try
        {
            context = new org.dspace.core.Context();

            int status = authenticationService.authenticate(context, user.getEmail(), user.getPassword(), null, null);
            if (status == AuthenticationMethod.SUCCESS)
            {
                EPerson ePerson = epersonService.findByEmail(context, user.getEmail());
                synchronized (TokenHolder.class) {
                    if (tokenPersons.inverse().containsKey(ePerson.getID())) {
                        token = tokenPersons.inverse().get(ePerson.getID());
                    } else {
                        token = generateToken();
                        tokenPersons.put(token, ePerson.getID());
                    }
                }
            }

            log.trace("User(" + user.getEmail() + ") has been logged in.");
            context.complete();
        }
        catch (SQLException e)
        {
            context.abort();
            log.error("Could not read user from database. Message:" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        finally
        {
            if ((context != null) && (context.isValid()))
            {
                context.abort();
                log.error("Something get wrong. Aborting context in finally statement.");
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return token;
    }

    /**
     * Return EPerson for log into context.
     * 
     * @param token
     *            Token under which is stored eperson.
     * @return Return instance of EPerson if is token right, otherwise it
     *         returns NULL.
     */
    public static synchronized EPerson getEPerson(String token)
    {
        try {
            EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
            UUID epersonID = tokenPersons.get(token);
            Context context = new Context();
            return epersonService.find(context, epersonID);
        } catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    /**
     * Logout user from rest api. It delete token and EPerson from TokenHolder.
     * 
     * @param token
     *            Token under which is stored eperson.
     * @return Return true if was all okay, otherwise return false.
     */
    public static synchronized boolean logout(String token)
    {
        if ((token == null) || (! tokenPersons.containsKey(token)))
        {
            return false;
        }

        UUID personID = tokenPersons.remove(token);
        if (personID == null)
        {
            return false;
        }
        return true;
    }

    /**
     * It generates unique token.
     * 
     * @return String filled with unique token.
     */
    private static String generateToken()
    {
        return UUID.randomUUID().toString();
    }

}
