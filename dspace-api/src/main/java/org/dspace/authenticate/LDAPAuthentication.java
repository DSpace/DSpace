/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Authentication module to authenticate against a flat LDAP tree where
 * all users are in the same unit.
 *
 * @author Larry Stone, Stuart Lewis
 * @version $Revision: 5844 $
 */
public class LDAPAuthentication
    implements AuthenticationMethod {

    /** log4j category */
    private static Logger log = Logger.getLogger(LDAPAuthentication.class);

    /**
     * Let a real auth method return true if it wants.
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
        throws SQLException
    {
        // XXX might also want to check that username exists in LDAP.

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
     * MIT policy on certs and groups, so always short-circuit.
     *
     * @return One of:
     *   SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
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
        {
            return BAD_ARGS;
        }

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

        // if they entered a netid that matches an eperson
        if (eperson != null)
        {
            // e-mail address corresponds to active account
            if (eperson.getRequireCertificate())
            {
                return CERT_REQUIRED;
            }
            else if (!eperson.canLogIn())
            {
                return BAD_ARGS;
            }

            if (ldap.ldapAuthenticate(netid, password, context))
            {
                eperson = EPerson.findByNetid(context, netid.toLowerCase());
                context.setCurrentUser(eperson);
                log.info(LogManager.getHeader(context, "authenticate", "type=ldap"));
                return SUCCESS;
            }
            else
            {
                return BAD_CREDENTIALS;
            }
        }

        // the user does not already exist so try and authenticate them
        // with ldap and create an eperson for them
        else
        {
            if (ldap.ldapAuthenticate(netid, password, context))
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
	                                if ((ldap.ldapEmail!=null)&&(!ldap.ldapEmail.equals("")))
                                    {
                                        eperson.setEmail(ldap.ldapEmail);
                                    }
	                                else
                                    {
                                        eperson.setEmail(netid);
                                    }
	                                if ((ldap.ldapGivenName!=null)&&(!ldap.ldapGivenName.equals("")))
                                    {
                                        eperson.setFirstName(ldap.ldapGivenName);
                                    }
	                                if ((ldap.ldapSurname!=null)&&(!ldap.ldapSurname.equals("")))
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
    private static class SpeakerToLDAP {

        private Logger log = null;

        /** ldap email result */
        protected String ldapEmail = null;

        /** ldap name result */
        protected String ldapGivenName = null;
        protected String ldapSurname = null;
        protected String ldapPhone = null;

        SpeakerToLDAP(Logger thelog)
        {
            log = thelog;
        }

        /**
         * contact the ldap server and attempt to authenticate
         */
        protected boolean ldapAuthenticate(String netid, String password, Context context)
        {
            if (!password.equals(""))
            {
                String ldap_provider_url = ConfigurationManager.getProperty("ldap.provider_url");
                String ldap_id_field = ConfigurationManager.getProperty("ldap.id_field");
                String ldap_search_context = ConfigurationManager.getProperty("ldap.search_context");
                String ldap_object_context = ConfigurationManager.getProperty("ldap.object_context");

                // Set up environment for creating initial context
                Hashtable env = new Hashtable(11);
                env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                env.put(javax.naming.Context.PROVIDER_URL, ldap_provider_url);

                // Authenticate
                env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
                env.put(javax.naming.Context.SECURITY_PRINCIPAL, ldap_id_field+"="+netid+","+ldap_object_context);
                env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);

                DirContext ctx = null;
                try
                {
                    // Create initial context
                    ctx = new InitialDirContext(env);

                    String ldap_email_field = ConfigurationManager.getProperty("ldap.email_field");
                    String ldap_givenname_field = ConfigurationManager.getProperty("ldap.givenname_field");
                    String ldap_surname_field = ConfigurationManager.getProperty("ldap.surname_field");
                    String ldap_phone_field = ConfigurationManager.getProperty("ldap.phone_field");

                    Attributes matchAttrs = new BasicAttributes(true);
                    matchAttrs.put(new BasicAttribute(ldap_id_field, netid));

                    String attlist[] = {ldap_email_field, ldap_givenname_field, ldap_surname_field, ldap_phone_field};

                    // look up attributes
                    try
                    {
                        NamingEnumeration answer = ctx.search(ldap_search_context, matchAttrs, attlist);
                        while(answer.hasMore()) {
                            SearchResult sr = (SearchResult)answer.next();
                            Attributes atts = sr.getAttributes();
                            Attribute att;

                            if (attlist[0]!=null)
                            {
                                    att = atts.get(attlist[0]);
                                    if (att != null)
                                    {
                                        ldapEmail = (String) att.get();
                                    }
                            }

                            if (attlist[1]!=null)
                            {
                                    att = atts.get(attlist[1]);
                                    if (att != null)
                                    {
                                        ldapGivenName = (String) att.get();
                                    }
                            }

                            if (attlist[2]!=null)
                            {
                                    att = atts.get(attlist[2]);
                                    if (att != null)
                                    {
                                        ldapSurname = (String) att.get();
                                    }
                            }

                            if (attlist[3]!=null)
                            {
                                    att = atts.get(attlist[3]);
                                    if (att != null)
                                    {
                                        ldapPhone = (String) att.get();
                                    }
                            }
                        }
                    }
                    catch (NamingException e)
                    {
                        // if the lookup fails go ahead and create a new record for them because the authentication
                        // succeeded
                        log.warn(LogManager.getHeader(context,
                                        "ldap_attribute_lookup", "type=failed_search "+e));
                        return true;
                    }
                }
                catch (NamingException e)
                {
                    log.warn(LogManager.getHeader(context,
                                        "ldap_authentication", "type=failed_auth "+e));
                    return false;
                }
                finally
                {
                    // Close the context when we're done
                    try
                    {
                        if (ctx != null)
                        {
                            ctx.close();
                        }
                    }
                    catch (NamingException e)
                    {
                    }
                }
            }
            else
            {
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
