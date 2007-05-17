/*
 * PasswordAuthentication.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.eperson;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * A stackable authentication method
 * based on the DSpace internal "EPerson" database.
 * See the <code>AuthenticationMethod</code> interface for more details.
 * <p>
 * The <em>username</em> is the E-Person's email address,
 * and and the <em>password</em> (given to the <code>authenticate()</code>
 * method) must match the EPerson password.
 * <p>
 * This is the default method for a new DSpace configuration.
 * If you are implementing a new "explicit" authentication method,
 * use this class as a model.
 * <p>
 * You can use this (or another explict) method in the stack to
 * implement HTTP Basic Authentication for servlets, by passing the
 * Basic Auth username and password to the <code>AuthenticationManager</code>.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class PasswordAuthentication
    implements AuthenticationMethod {

    /** log4j category */
    private static Logger log = Logger.getLogger(PasswordAuthentication.class);

    /**
     * Look to see if this email address is allowed to register.
     * <p>
     * The configuration key authentication.password.domain.valid is examined
     * in dspace.cfg to see what doamins are valid.
     * <p>
     * Example - aber.ac.uk domains : @aber.ac.uk
     * Example - MIT domain and all .ac.uk domains: @mit.edu, .ac.uk
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String email)
                                                 throws SQLException
    {
        // Is there anything set in authentication.password.domain.valid?
        String domains = ConfigurationManager.getProperty("authentication.password.domain.valid");
        if ((domains == null) || (domains.trim().equals("")))
        {
            // No conditions set, so must be able to self register
            return true;
        }
        else
        {
            // Itterate through all domains
            String[] options = domains.trim().split(",");
            String check;
            email = email.trim().toLowerCase();
            for (int i = 0; i < options.length; i++)
            {
                check = options[i].trim().toLowerCase();
                if (email.endsWith(check))
                {
                    // A match, so we can register this user
                    return true;
                }
            }
            
            // No match
            return false;
        }    
    }

    /**
     *  Nothing extra to initialize.
     */
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson)
        throws SQLException
    {
    }

    /**
     * We always allow the user to change their password.
     */
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException
    {
        return true;
    }

    /**
     * This is an explicit method, since it needs username and password
     * from some source.
     * @return false
     */
    public boolean isImplicit()
    {
        return false;
    }

    /**
     * No special groups.
     */
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
        return new int[0];
    }

    /**
     * Check credentials: username must match the email address of an
     * EPerson record, and that EPerson must be allowed to login.
     * Password must match its password.  Also checks for EPerson that
     * is only allowed to login via an implicit method
     * and returns <code>CERT_REQUIRED</code> if that is the case.
     *
     * @param context
     *  DSpace context, will be modified (EPerson set) upon success.
     *
     * @param username
     *  Username (or email address) when method is explicit. Use null for
     *  implicit method.
     *
     * @param password
     *  Password for explicit auth, or null for implicit method.
     *
     * @param realm
     *  Realm is an extra parameter used by some authentication methods, leave null if
     *  not applicable.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @return One of:
     *   SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>BAD_CREDENTIALS - user exists, but assword doesn't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - no EPerson with matching email address.
     * <br>BAD_ARGS        - missing username, or user matched but cannot login.
     */
    public int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
        throws SQLException
    {
        if (username != null && password != null)
        {
            EPerson eperson = null;
            log.info(LogManager.getHeader(context, "authenticate", "attempting password auth of user="+username));
            try
            {
                eperson = EPerson.findByEmail(context, username.toLowerCase());
            }
            catch (AuthorizeException e)
            {
                // ignore exception, treat it as lookup failure.
            }

            // lookup failed.
            if (eperson == null)
                return NO_SUCH_USER;

            // cannot login this way
            else if (!eperson.canLogIn())
                return BAD_ARGS;

            // this user can only login with x.509 certificate
            else if (eperson.getRequireCertificate())
            {
                log.warn(LogManager.getHeader(context, "authenticate", "rejecting PasswordAuthentication because "+username+" requires certificate."));
                return CERT_REQUIRED;
            }

            // login is ok if password matches:
            else if (eperson.checkPassword(password))
            {
                context.setCurrentUser(eperson);
                log.info(LogManager.getHeader(context, "authenticate", "type=PasswordAuthentication"));
                return SUCCESS;
            }
            else
                return BAD_CREDENTIALS;
        }

        // BAD_ARGS always defers to the next authentication method.
        // It means this method cannot use the given credentials.
        else
            return BAD_ARGS;
    }

    /**
     * Returns URL of password-login servlet.
     *
     * @param context
     *  DSpace context, will be modified (EPerson set) upon success.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @param response
     *  The HTTP response from the servlet method.
     *
     * @return fully-qualified URL
     */
    public String loginPageURL(Context context,
                            HttpServletRequest request,
                            HttpServletResponse response)
    {
        return response.encodeRedirectURL(request.getContextPath() +
                                          "/password-login");
    }

    /**
     * Returns message key for title of the "login" page, to use
     * in a menu showing the choice of multiple login methods.
     *
     * @param context
     *  DSpace context, will be modified (EPerson set) upon success.
     *
     * @return Message key to look up in i18n message catalog.
     */
    public String loginPageTitle(Context context)
    {
        return "org.dspace.eperson.PasswordAuthentication.title";
    }
}
