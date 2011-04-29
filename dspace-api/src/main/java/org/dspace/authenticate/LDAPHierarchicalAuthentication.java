/*
 * LDAPHierarchicalAuthentication.java
 *
 * Version: $Revision: 4690 $
 *
 * Date: $Date: 2010-01-13 07:16:24 -0500 (Wed, 13 Jan 2010) $
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
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * This LDAP authentication method is more complex than the simple 'LDAPAuthentication'
 * in that it allows authentication against structured heirarchical LDAP trees of
 * users. An initial bind is required using a user name and password in order to
 * searchthe tree and find the DN of the user. A second bind is then required to
 * chack the credentials of the user by binding directly to their DN.
 *
 * @author Stuart Lewis, Chris Yates, Alex Barbieri, Flavio Botelho, Reuben Pasquini
 * @version $Revision: 4690 $
 */
public class LDAPHierarchicalAuthentication
    implements AuthenticationMethod {

    /** log4j category */
    private static Logger log = Logger.getLogger(LDAPHierarchicalAuthentication.class);

	/**
     * Let a real auth method return true if it wants.
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
        throws SQLException
    {
        // Looks to see if webui.ldap.autoregister is set or not
        return ConfigurationManager.getBooleanProperty("webui.ldap.autoregister");
    }

    /**
     *  Nothing here, initialization is done when auto-registering.
     */
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson)
        throws SQLException
    {
        // XXX should we try to initialize netid based on email addr,
        // XXX  for eperson created by some other method??
    }

    /**
     * Cannot change LDAP password through dspace, right?
     */
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException
    {
        // XXX is this right?
        return false;
    }

    /*
     * This is an explicit method.
     */
    public boolean isImplicit()
    {
        return false;
    }

    /*
     * Add authenticated users to the group defined in dspace.cfg by
     * the ldap.login.specialgroup key.
     */
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
		// Prevents anonymous users from being added to this group, and the second check
		// ensures they are LDAP users
		try
		{
			if (!context.getCurrentUser().getNetid().equals(""))
			{
				String groupName = ConfigurationManager.getProperty("ldap.login.specialgroup");
				if ((groupName != null) && (!groupName.trim().equals("")))
				{
					Group ldapGroup = Group.findByName(context, groupName);
					if (ldapGroup == null)
					{
						// Oops - the group isn't there.
						log.warn(LogManager.getHeader(context,
								"ldap_specialgroup",
								"Group defined in ldap.login.specialgroup does not exist"));
						return new int[0];
					} else
					{
						return new int[] { ldapGroup.getID() };
					}
				}
			}
		}
		catch (Exception npe) {
			// The user is not an LDAP user, so we don't need to worry about them
		}
		return new int[0];
    }

    /*
     * Authenticate the given credentials.
     * This is the heart of the authentication method: test the
     * credentials for authenticity, and if accepted, attempt to match
     * (or optionally, create) an <code>EPerson</code>.  If an <code>EPerson</code> is found it is
     * set in the <code>Context</code> that was passed.
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
     * <br>BAD_CREDENTIALS - user exists, but credentials (e.g. passwd) don't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - user not found using this method.
     * <br>BAD_ARGS        - user/pw not appropriate for this method
     */
    public int authenticate(Context context,
                            String netid,
                            String password,
                            String realm,
                            HttpServletRequest request)
        throws SQLException
    {
        log.info(LogManager.getHeader(context, "auth", "attempting trivial auth of user="+netid));

        // Skip out when no netid or password is given.
        if (netid == null || password == null)
        	return BAD_ARGS;

        // Locate the eperson
        EPerson eperson = null;
        try
        {
        		eperson = EPerson.findByNetid(context, netid.toLowerCase());
        }
        catch (SQLException e)
        {
        }
        SpeakerToLDAP ldap = new SpeakerToLDAP(log);

		// Get the DN of the user
		String adminUser = ConfigurationManager.getProperty("ldap.search.user");
		String adminPassword = ConfigurationManager.getProperty("ldap.search.password");
		String dn = ldap.getDNOfUser(adminUser, adminPassword, context, netid);

		// Check a DN was found
		if ((dn == null) || (dn.trim().equals("")))
		{
			log.info(LogManager
				.getHeader(context, "failed_login", "no DN found for user " + netid));
			return BAD_CREDENTIALS;
		}

		// if they entered a netid that matches an eperson
        if (eperson != null)
        {
            // e-mail address corresponds to active account
            if (eperson.getRequireCertificate())
                return CERT_REQUIRED;
            else if (!eperson.canLogIn())
                return BAD_ARGS;
            {
                if (ldap.ldapAuthenticate(dn, password, context))
                {
                    context.setCurrentUser(eperson);
                    log.info(LogManager
                        .getHeader(context, "authenticate", "type=ldap"));
                    return SUCCESS;
                }
                else
                   return BAD_CREDENTIALS;
            }
        }

        // the user does not already exist so try and authenticate them
        // with ldap and create an eperson for them
        else
        {
            if (ldap.ldapAuthenticate(dn, password, context))
            {
                // Register the new user automatically
                log.info(LogManager.getHeader(context,
                                "autoregister", "netid=" + netid));

				if ((ldap.ldapEmail!=null)&&(!ldap.ldapEmail.equals("")))
				{
					try
					{
						eperson = EPerson.findByEmail(context, ldap.ldapEmail);
						if (eperson!=null)
						{
							log.info(LogManager.getHeader(context,
									"type=ldap-login", "type=ldap_but_already_email"));
							context.setIgnoreAuthorization(true);
							eperson.setNetid(netid.toLowerCase());
							eperson.update();
							context.commit();
							context.setIgnoreAuthorization(false);
							context.setCurrentUser(eperson);
							return SUCCESS;
						}
						else
						{
							if (canSelfRegister(context, request, netid))
							{
								// TEMPORARILY turn off authorisation
								try
								{
									context.setIgnoreAuthorization(true);
									eperson = EPerson.create(context);
									if ((ldap.ldapEmail != null) && (!ldap.ldapEmail.equals("")))
									{
										eperson.setEmail(ldap.ldapEmail);
									}
									else
									{
										eperson.setEmail(netid + ConfigurationManager.getProperty("ldap.netid_email_domain"));
									}
									if ((ldap.ldapGivenName!=null) && (!ldap.ldapGivenName.equals("")))
									{
										eperson.setFirstName(ldap.ldapGivenName);
									}
									if ((ldap.ldapSurname!=null) && (!ldap.ldapSurname.equals("")))
									{
										eperson.setLastName(ldap.ldapSurname);
									}
									if ((ldap.ldapPhone!=null)&&(!ldap.ldapPhone.equals("")))
									{
										eperson.setMetadata("phone", ldap.ldapPhone);
									}
									eperson.setNetid(netid.toLowerCase());
									eperson.setCanLogIn(true);
									AuthenticationManager.initEPerson(context, request, eperson);
									eperson.update();
									context.commit();
									context.setCurrentUser(eperson);
								}
								catch (AuthorizeException e)
								{
									return NO_SUCH_USER;
								}
								finally
								{
									context.setIgnoreAuthorization(false);
								}

								log.info(LogManager.getHeader(context, "authenticate",
											"type=ldap-login, created ePerson"));
								return SUCCESS;
							}
							else
							{
								// No auto-registration for valid certs
								log.info(LogManager.getHeader(context,
												"failed_login", "type=ldap_but_no_record"));
								return NO_SUCH_USER;
							}
						}
					}
					catch (AuthorizeException e)
					{
						eperson = null;
					}
					finally
					{
						context.setIgnoreAuthorization(false);
					}
				}
			}
        }
        return BAD_ARGS;
    }

    /**
     * Internal class to manage LDAP query and results, mainly
     * because there are multiple values to return.
     */
    public class SpeakerToLDAP {

        private Logger log = null;

        protected String ldapEmail = null;
        protected String ldapGivenName = null;
        protected String ldapSurname = null;
        protected String ldapPhone = null;

		/** LDAP settings */
		String ldap_provider_url = ConfigurationManager.getProperty("ldap.provider_url");
		String ldap_id_field = ConfigurationManager.getProperty("ldap.id_field");
		String ldap_search_context = ConfigurationManager.getProperty("ldap.search_context");
		String ldap_object_context = ConfigurationManager.getProperty("ldap.object_context");
		String ldap_search_scope = ConfigurationManager.getProperty("ldap.search_scope");

		String ldap_email_field = ConfigurationManager.getProperty("ldap.email_field");
		String ldap_givenname_field = ConfigurationManager.getProperty("ldap.givenname_field");
		String ldap_surname_field = ConfigurationManager.getProperty("ldap.surname_field");
		String ldap_phone_field = ConfigurationManager.getProperty("ldap.phone_field");

		SpeakerToLDAP(Logger thelog)
        {
            log = thelog;
        }

		protected String getDNOfUser(String adminUser, String adminPassword, Context context, String netid)
		{
			// The resultant DN
			String resultDN;

			// The search scope to use (default to 0)
			int ldap_search_scope_value = 0;
			try
			{
				ldap_search_scope_value = Integer.parseInt(ldap_search_scope.trim());
			}
			catch (NumberFormatException e)
			{
				// Log the error if it has been set but is invalid
				if (ldap_search_scope != null)
				{
					log.warn(LogManager.getHeader(context,
							"ldap_authentication", "invalid search scope: " + ldap_search_scope));
				}
			}

			// Set up environment for creating initial context
			Hashtable env = new Hashtable(11);
			env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(javax.naming.Context.PROVIDER_URL, ldap_provider_url);

            if ((adminUser != null) && (!adminUser.trim().equals("")) &&
                (adminPassword != null) && (!adminPassword.trim().equals("")))
            {
                // Use admin credencials for search// Authenticate
                env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
                env.put(javax.naming.Context.SECURITY_PRINCIPAL, adminUser);
                env.put(javax.naming.Context.SECURITY_CREDENTIALS, adminPassword);
            }
            else
            {
                // Use anonymous authentication
                env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "none");
            }

			DirContext ctx = null;
			try
			{
				// Create initial context
				ctx = new InitialDirContext(env);

				Attributes matchAttrs = new BasicAttributes(true);
				matchAttrs.put(new BasicAttribute(ldap_id_field, netid));

				// look up attributes
				try
				{
					SearchControls ctrls = new SearchControls();
					ctrls.setSearchScope(ldap_search_scope_value);

					NamingEnumeration<SearchResult> answer = ctx.search(
							ldap_provider_url + ldap_search_context,
							"(&({0}={1}))", new Object[] { ldap_id_field,
									netid }, ctrls);

					while (answer.hasMoreElements()) {
						SearchResult sr = answer.next();
                        if (StringUtils.isEmpty(ldap_search_context)) {
                            resultDN = sr.getName();
                        } else {
                            resultDN = (sr.getName() + "," + ldap_search_context);
                        }

						String attlist[] = {ldap_email_field, ldap_givenname_field,
								            ldap_surname_field, ldap_phone_field};
						Attributes atts = sr.getAttributes();
						Attribute att;

						if (attlist[0] != null) {
							att = atts.get(attlist[0]);
							if (att != null)
								ldapEmail = (String) att.get();
						}

						if (attlist[1] != null) {
							att = atts.get(attlist[1]);
							if (att != null)
								ldapGivenName = (String) att.get();
						}

						if (attlist[2] != null) {
							att = atts.get(attlist[2]);
							if (att != null)
								ldapSurname = (String) att.get();
						}

						if (attlist[3] != null) {
							att = atts.get(attlist[3]);
							if (att != null)
								ldapPhone = (String) att.get();
						}

						if (answer.hasMoreElements()) {
							// Oh dear - more than one match
							// Ambiguous user, can't continue

						} else {
							log.debug(LogManager.getHeader(context, "got DN", resultDN));
							return resultDN;
						}
					}
				}
				catch (NamingException e)
				{
					// if the lookup fails go ahead and create a new record for them because the authentication
					// succeeded
					log.warn(LogManager.getHeader(context,
								"ldap_attribute_lookup", "type=failed_search "
										+ e));
				}
			}
			catch (NamingException e)
			{
				log.warn(LogManager.getHeader(context,
							"ldap_authentication", "type=failed_auth " + e));
			}
			finally
			{
				// Close the context when we're done
				try
				{
					if (ctx != null)
						ctx.close();
				}
				catch (NamingException e)
				{
				}
			}

			// No DN match found
			return null;
		}

		/**
         * contact the ldap server and attempt to authenticate
         */
		protected boolean ldapAuthenticate(String netid, String password,
						Context context) {
			if (!password.equals("")) {
				// Set up environment for creating initial context
				Hashtable<String, String> env = new Hashtable<String, String>();
				env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
						"com.sun.jndi.ldap.LdapCtxFactory");
				env.put(javax.naming.Context.PROVIDER_URL, ldap_provider_url);

				// Authenticate
				env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "Simple");
				env.put(javax.naming.Context.SECURITY_PRINCIPAL, netid);
				env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);
				env.put(javax.naming.Context.AUTHORITATIVE, "true");
				env.put(javax.naming.Context.REFERRAL, "follow");

				DirContext ctx = null;
				try {
					// Try to bind
					ctx = new InitialDirContext(env);
				} catch (NamingException e) {
					log.warn(LogManager.getHeader(context,
							"ldap_authentication", "type=failed_auth " + e));
					return false;
				} finally {
					// Close the context when we're done
					try {
						if (ctx != null)
							ctx.close();
					} catch (NamingException e) {
					}
				}
			} else {
				return false;
			}

			return true;
		}		
	}

    /*
     * Returns URL to which to redirect to obtain credentials (either password
     * prompt or e.g. HTTPS port for client cert.); null means no redirect.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
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
                                          "/ldap-login");
    }

    /**
     * Returns message key for title of the "login" page, to use
     * in a menu showing the choice of multiple login methods.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @return Message key to look up in i18n message catalog.
     */
    public String loginPageTitle(Context context)
    {
        return "org.dspace.eperson.LDAPAuthentication.title";
    }
}