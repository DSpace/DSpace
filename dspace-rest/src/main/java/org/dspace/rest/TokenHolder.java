/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;
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

    private static Map<String, String> tokens = new HashMap<String, String>(); // Map with pair Email,token

    private static Map<String, EPerson> persons = new HashMap<String, EPerson>(); // Map with pair token,Eperson

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
        org.dspace.core.Context context = null;
        String token = null;

        try
        {
            context = new org.dspace.core.Context();
            EPerson dspaceUser = EPerson.findByEmail(context, user.getEmail());

            synchronized (TokenHolder.class) {
                if ((dspaceUser == null) || (!dspaceUser.checkPassword(user.getPassword())))
                {
                    token = null;
                }
                else if (tokens.containsKey(user.getEmail()))
                {
                    token = tokens.get(user.getEmail());
                }
                else
                {
                    token = generateToken();
                    persons.put(token, dspaceUser);
                    tokens.put(user.getEmail(), token);
                }
            }

            log.trace("User(" + user.getEmail() + ") has been logged.");
            context.complete();

        }
        catch (SQLException e)
        {
            context.abort();
            log.error("Could not read user from database. Message:" + e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        catch (AuthorizeException e)
        {
            context.abort();
            log.error("Could not find user, AuthorizeException. Message:" + e);
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
        return persons.get(token);
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
        if ((token == null) || (persons.get(token) == null))
        {
            return false;
        }
        String email = persons.get(token).getEmail();
        EPerson person = persons.remove(token);
        if (person == null)
        {
            return false;
        }
        tokens.remove(email);
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
