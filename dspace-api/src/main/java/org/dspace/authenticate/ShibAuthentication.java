/*
 * ShibAuthentication.java
 *
 * Version: $Revision: 4637 $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;

import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Shibboleth authentication for DSpace, tested on Shibboleth 1.3.x and
 * Shibboleth 2.x. Read <a href=
 * "https://mams.melcoe.mq.edu.au/zope/mams/pubs/Installation/dspace15/view"
 * >Shib DSpace 1.5</a> for installation procedure. Read dspace.cfg for details
 * on options available.
 * 
 * @author <a href="mailto:bliong@melcoe.mq.edu.au">Bruc Liong, MELCOE</a>
 * @author <a href="mailto:kli@melcoe.mq.edu.au">Xiang Kevin Li, MELCOE</a>
 * @version $Revision: 4637 $
 */
public class ShibAuthentication implements AuthenticationMethod
{
    /** log4j category */
    private static Logger log = Logger.getLogger(ShibAuthentication.class);

    public int authenticate(Context context, String username, String password,
            String realm, HttpServletRequest request) throws SQLException
    {
        if (request == null)
        {
            return BAD_ARGS;
        }
        log.info("Shibboleth login started...");

        java.util.Enumeration names = request.getHeaderNames();
        String name;
        while (names.hasMoreElements())
            log.debug("header:" + (name = names.nextElement().toString()) + "="
                    + request.getHeader(name));

        boolean isUsingTomcatUser = ConfigurationManager
                .getBooleanProperty("authentication.shib.email-use-tomcat-remote-user");
        String emailHeader = ConfigurationManager
                .getProperty("authentication.shib.email-header");
        String fnameHeader = ConfigurationManager
                .getProperty("authentication.shib.firstname-header");
        String lnameHeader = ConfigurationManager
                .getProperty("authentication.shib.lastname-header");

        String email = null;
        String fname = null;
        String lname = null;

        if (emailHeader != null)
        {
            // try to grab email from the header
            email = request.getHeader(emailHeader);

            // fail, try lower case
            if (email == null)
                email = request.getHeader(emailHeader.toLowerCase());
        }

        // try to pull the "REMOTE_USER" info instead of the header
        if (email == null && isUsingTomcatUser)
        {
            email = request.getRemoteUser();
            log.info("RemoteUser identified as: " + email);
        }

        // No email address, perhaps the eperson has been setup, better check it
        if (email == null)
        {
            EPerson p = context.getCurrentUser();
            if (p != null)
                email = p.getEmail();
        }

        if (email == null)
        {
            log
                    .error("No email is given, you're denied access by Shib, please release email address");
            return AuthenticationMethod.BAD_ARGS;
        }

        email = email.toLowerCase();

        if (fnameHeader != null)
        {
            // try to grab name from the header
            fname = request.getHeader(fnameHeader);

            // fail, try lower case
            if (fname == null)
                fname = request.getHeader(fnameHeader.toLowerCase());
        }
        if (lnameHeader != null)
        {
            // try to grab name from the header
            lname = request.getHeader(lnameHeader);

            // fail, try lower case
            if (lname == null)
                lname = request.getHeader(lnameHeader.toLowerCase());
        }

        // future version can offer auto-update feature, this needs testing
        // before inclusion to core code

        EPerson eperson = null;
        try
        {
            eperson = EPerson.findByEmail(context, email);
            context.setCurrentUser(eperson);
        }
        catch (AuthorizeException e)
        {
            log.warn("Fail to locate user with email:" + email, e);
            eperson = null;
        }

        // auto create user if needed
        if (eperson == null
                && ConfigurationManager
                        .getBooleanProperty("authentication.shib.autoregister"))
        {
            log.info(LogManager.getHeader(context, "autoregister", "email="
                    + email));

            // TEMPORARILY turn off authorisation
            context.setIgnoreAuthorization(true);
            try
            {
                eperson = EPerson.create(context);
                eperson.setEmail(email);
                if (fname != null)
                    eperson.setFirstName(fname);
                if (lname != null)
                    eperson.setLastName(lname);
                eperson.setCanLogIn(true);
                AuthenticationManager.initEPerson(context, request, eperson);
                eperson.update();
                context.commit();
                context.setCurrentUser(eperson);
            }
            catch (AuthorizeException e)
            {
                log.warn("Fail to authorize user with email:" + email, e);
                eperson = null;
            }
            finally
            {
                context.setIgnoreAuthorization(false);
            }
        }

        if (eperson == null)
        {
            return AuthenticationMethod.NO_SUCH_USER;
        }
        else
        {
            // the person exists, just return ok
            context.setCurrentUser(eperson);
            request.getSession().setAttribute("shib.authenticated",
                    new Boolean("true"));
        }

        return AuthenticationMethod.SUCCESS;
    }

    /**
     * Grab the special groups to be automatically provisioned for the current
     * user. Currently the mapping for the groups is done one-to-one, future
     * version can consider the usage of regex for such mapping.
     */
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
        // no user logged in or user not logged from shibboleth
        if (request == null || context.getCurrentUser() == null
                || request.getSession().getAttribute("shib.authenticated") == null)
        {
            return new int[0];
        }
                
        if (request.getSession().getAttribute("shib.specialgroup") != null)
        {
            return (int[]) request.getSession().getAttribute(
                    "shib.specialgroup");
        }

        java.util.Set groups = new java.util.HashSet();
        String roleHeader = ConfigurationManager
                .getProperty("authentication.shib.role-header");
        boolean roleHeader_ignoreScope = ConfigurationManager
                .getBooleanProperty("authentication.shib.role-header.ignore-scope");
        if (roleHeader == null || roleHeader.trim().length() == 0)
            roleHeader = "Shib-EP-UnscopedAffiliation"; // fall back to default
        String affiliations = request.getHeader(roleHeader);

        // try again with all lower case...maybe has better luck
        if (affiliations == null)
            affiliations = request.getHeader(roleHeader.toLowerCase());

        // default role when fully authN but not releasing any roles?
        String defaultRoles = ConfigurationManager
                .getProperty("authentication.shib.default-roles");
        if (affiliations == null && defaultRoles != null)
        {
            affiliations = defaultRoles;
        }

        if (affiliations != null)
        {
            java.util.StringTokenizer st = new java.util.StringTokenizer(
                    affiliations, ";,");
            // do the mapping here
            while (st.hasMoreTokens())
            {
                String affiliation = st.nextToken().trim();

                // strip scope if present and roleHeader_ignoreScope
                if (roleHeader_ignoreScope) 
                {
                        int index = affiliation.indexOf("@");
                        if (index != -1) affiliation = affiliation.substring(0,index);
                }

                // perform mapping here if necessary
                String groupLabels = ConfigurationManager
                        .getProperty("authentication.shib.role." + affiliation);
                if (groupLabels == null || groupLabels.trim().length() == 0)
                    groupLabels = ConfigurationManager
                            .getProperty("authentication.shib.role."
                                    + affiliation.toLowerCase());

                // revert back to original entry when no mapping is provided
                if (groupLabels == null)
                    groupLabels = affiliation;

                String[] labels = groupLabels.split(",");
                for (int i = 0; i < labels.length; i++)
                    addGroup(groups, context, labels[i].trim());
            }
        }

        int ids[] = new int[groups.size()];
        java.util.Iterator it = groups.iterator();
        for (int i = 0; it.hasNext(); i++)
            ids[i] = ((Integer) it.next()).intValue();

        // store the special group, if already transformed from headers
        // since subsequent header may not have the values anymore
        if (ids.length != 0)
        {
            request.getSession().setAttribute("shib.specialgroup", ids);
        }

        return ids;
    }

    /** Find dspaceGroup in DSpace database, if found, include it into groups */
    private void addGroup(Collection groups, Context context, String dspaceGroup)
    {
        try
        {
            Group g = Group.findByName(context, dspaceGroup);
            if (g == null)
            {
                // oops - no group defined
                log.warn(LogManager.getHeader(context, dspaceGroup
                        + " group is not found!! Admin needs to create one!",
                        "requiredGroup=" + dspaceGroup));
                groups.add(new Integer(0));
            }
            else
            {
                groups.add(new Integer(g.getID()));
            }
            log.info("Mapping group: " + dspaceGroup + " to groupID: "
                    + (g == null ? 0 : g.getID()));
        }
        catch (SQLException e)
        {
            log.error("Mapping group:" + dspaceGroup + " failed with error", e);
        }
    }

    /**
     * Indicate whether or not a particular self-registering user can set
     * themselves a password in the profile info form.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case anything in that is used to decide
     * @param email
     *            e-mail address of user attempting to register
     * 
     */
    public boolean allowSetPassword(Context context,
            HttpServletRequest request, String email) throws SQLException
    {
        // don't use password at all
        return false;
    }

    /**
     * Predicate, is this an implicit authentication method. An implicit method
     * gets credentials from the environment (such as an HTTP request or even
     * Java system properties) rather than the explicit username and password.
     * For example, a method that reads the X.509 certificates in an HTTPS
     * request is implicit.
     * 
     * @return true if this method uses implicit authentication.
     */
    public boolean isImplicit()
    {
        return true;
    }

    /**
     * Indicate whether or not a particular user can self-register, based on
     * e-mail address.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case anything in that is used to decide
     * @param email
     *            e-mail address of user attempting to register
     * 
     */
    public boolean canSelfRegister(Context context, HttpServletRequest request,
            String username) throws SQLException
    {
        return true;
    }

    /**
     * Initialise a new e-person record for a self-registered new user.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case it's needed
     * @param eperson
     *            newly created EPerson record - email + information from the
     *            registration form will have been filled out.
     * 
     */
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson) throws SQLException
    {

    }

    /**
     * Get login page to which to redirect. Returns URL (as string) to which to
     * redirect to obtain credentials (either password prompt or e.g. HTTPS port
     * for client cert.); null means no redirect.
     * 
     * @param context
     *            DSpace context, will be modified (ePerson set) upon success.
     * 
     * @param request
     *            The HTTP request that started this operation, or null if not
     *            applicable.
     * 
     * @param response
     *            The HTTP response from the servlet method.
     * 
     * @return fully-qualified URL or null
     */
    public String loginPageURL(Context context, HttpServletRequest request,
            HttpServletResponse response)
    {
        return response.encodeRedirectURL(request.getContextPath()
                + "/shibboleth-login");
    }

    /**
     * Get title of login page to which to redirect. Returns a <i>message
     * key</i> that gets translated into the title or label for "login page" (or
     * null, if not implemented) This title may be used to identify the link to
     * the login page in a selection menu, when there are multiple ways to
     * login.
     * 
     * @param context
     *            DSpace context, will be modified (ePerson set) upon success.
     * 
     * @return title text.
     */
    public String loginPageTitle(Context context)
    {
        return "org.dspace.authenticate.ShibAuthentication.title";
    }
}
