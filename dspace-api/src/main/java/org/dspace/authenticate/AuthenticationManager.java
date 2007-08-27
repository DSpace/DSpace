/*
 * AuthenticationManager.java
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
package org.dspace.authenticate;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * Access point for the stackable authentication methods.
 * <p>
 * This class initializes the "stack" from the DSpace configuration,
 * and then invokes methods in the appropriate order on behalf of clients.
 * <p>
 * See the AuthenticationMethod interface for details about what each
 * function does.
 * <p>
 * <b>Configuration</b><br>
 * The stack of authentication methods is defined by one property in the DSpace configuration:
 * <pre>
 *   plugin.sequence.org.dspace.eperson.AuthenticationMethod = <em>a list of method class names</em>
 *     <em>e.g.</em>
 *   plugin.sequence.org.dspace.eperson.AuthenticationMethod = \
 *       org.dspace.eperson.X509Authentication, \
 *       org.dspace.eperson.PasswordAuthentication
 * </pre>
 * <p>
 * The "stack" is always traversed in order, with the methods
 * specified first (in the configuration) thus getting highest priority.
 *
 * @see AuthenticationMethod
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class AuthenticationManager
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AuthenticationManager.class);

    /** List of authentication methods, highest precedence first. */
    private static AuthenticationMethod methodStack[] =
        (AuthenticationMethod[])PluginManager.getPluginSequence(AuthenticationMethod.class);

    /**
     * Test credentials for authenticity.
     * Apply the given credentials to each authenticate() method in
     * the stack.  Returns upon the first <code>SUCCESS</code>, or otherwise
     * returns the most favorable outcome from one of the methods.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
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
     * <br>BAD_CREDENTIALS - user exists, but credenitals (e.g. passwd) don't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - user not found using this method.
     * <br>BAD_ARGS        - user/pw not appropriate for this method
     */
    public static int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
    {
        return authenticateInternal(context, username, password, realm,
                                     request, false);
    }

    /**
     * Test credentials for authenticity, using only Implicit methods.
     * Just like <code>authenticate()</code>, except it only invokes the
     * <em>implicit</em> authentication methods the stack.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
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
     * <br>BAD_CREDENTIALS - user exists, but credenitals (e.g. passwd) don't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - user not found using this method.
     * <br>BAD_ARGS        - user/pw not appropriate for this method
     */
    public static int authenticateImplicit(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
    {
        return authenticateInternal(context, username, password, realm,
                                     request, true);
    }

    private static int authenticateInternal(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request,
                            boolean implicitOnly)
    {
        // better is lowest, so start with the highest.
        int bestRet = AuthenticationMethod.BAD_ARGS;

        // return on first success, otherwise "best" outcome.
        for (int i = 0; i < methodStack.length; ++i)
        {
            if (!implicitOnly || methodStack[i].isImplicit())
            {
                int ret = 0;
                try
                {
                    ret = methodStack[i].authenticate(context, username, password, realm, request);
                }
                catch (SQLException e)
                {
                    ret = AuthenticationMethod.NO_SUCH_USER;
                }
                if (ret == AuthenticationMethod.SUCCESS)
                    return ret;
                if (ret < bestRet)
                    bestRet = ret;
            }
        }
        return bestRet;
    }

    /**
     * Predicate, can a new EPerson be created.
     * Invokes <code>canSelfRegister()</code> of every authentication
     * method in the stack, and returns true if any of them is true.
     *
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case it's needed. Can be null.
     * @param username
     *            Username, if available.  Can be null.
     * @return true if new ePerson should be created.
     */
    public static boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
        throws SQLException
    {
        for (int i = 0; i < methodStack.length; ++i)
            if (methodStack[i].canSelfRegister(context, request, username))
                return true;
        return false;
    }

    /**
     * Predicate, can user set EPerson password.
     * Returns true if the <code>allowSetPassword()</code> method of any
     * member of the stack returns true.
     *
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case it's needed. Can be null.
     * @param username
     *            Username, if available.  Can be null.
     * @return true if this method allows user to change ePerson password.
     */
    public static boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException
    {
        for (int i = 0; i < methodStack.length; ++i)
            if (methodStack[i].allowSetPassword(context, request, username))
                return true;
        return false;
    }

    /**
     * Initialize a new e-person record for a self-registered new user.
     * Give every authentication method in the stack a chance to
     * initialize the new ePerson by calling its <code>initEperson()</code>
     *
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case it's needed. Can be null.
     * @param eperson
     *            newly created EPerson record - email + information from the
     *            registration form will have been filled out.
     */
    public static void initEPerson(Context context,
                                   HttpServletRequest request,
                                   EPerson eperson)
        throws SQLException
    {
        for (int i = 0; i < methodStack.length; ++i)
            methodStack[i].initEPerson(context, request, eperson);
    }

    /**
     * Get list of extra groups that user implicitly belongs to.
     * Returns accumulation of groups of all the <code>getSpecialGroups()</code>
     * methods in the stack.
     *
     * @param context
     *  A valid DSpace context.
     *
     * @param request
     *  The request that started this operation, or null if not applicable.
     *
     * @return Returns IDs of any groups the user authenticated by this
     * request is in implicitly -- checks for e.g. network-address dependent
     * groups.
     */
    public static int[] getSpecialGroups(Context context,
                                         HttpServletRequest request)
        throws SQLException
    {
        ArrayList gll = new ArrayList();
        int totalLen = 0;

        for (int i = 0; i < methodStack.length; ++i)
        {
            int gl[] = methodStack[i].getSpecialGroups(context, request);
            if (gl.length > 0)
            {
                gll.add(gl);
                totalLen += gl.length;
            }
        }

        // Maybe this is over-optimized but it's called on every
        // request, and most sites will only have 0 or 1 auth methods
        // actually returning groups, so it pays..
        if (totalLen == 0)
            return new int[0];
        else if (gll.size() == 1)
            return (int [])gll.get(0);
        else
        {
            // Have to do it this painful way since list.toArray() doesn't
            // work on int[].  stupid Java ints aren't first-class objects.
            int result[] = new int[totalLen];
            int k = 0;
            for (int i = 0; i < gll.size(); ++i)
            {
                int gl[] = (int [])gll.get(i);
                for (int j = 0; j < gl.length; ++j)
                    result[k++] = gl[j];
            }
            return result;
        }
    }

    /**
     * Get stack of authentication methods.
     * Return an <code>Iterator</code> that steps through each configured
     * authentication method, in order of precedence.
     *
     * @return Iterator object.
     */
    public static Iterator authenticationMethodIterator()
    {
        return Arrays.asList(methodStack).iterator();
    }
}
