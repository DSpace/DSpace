/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * Shibboleth authentication for DSpace
 * 
 * Shibboleth is a distributed authentication system for securely authenticating
 * users and passing attributes about the user from one or more identity
 * providers. In the Shibboleth terminology DSpace is a Service Provider which
 * receives authentication information and then based upon that provides a 
 * service to the user. With Shibboleth DSpace will require that you use 
 * Apache installed with the mod_shib module acting as a proxy for all HTTP
 * requests for your servlet container (typically Tomcat). DSpace will receive
 * authentication information from the mod_shib module through HTTP headers. 
 *
 * See for more information on installing and configuring a Shibboleth 
 * Service Provider:
 * https://wiki.shibboleth.net/confluence/display/SHIB2/Installation
 * 
 * See the DSpace.cfg or DSpace manual for information on how to configure
 * this authentication module.
 * 
 * @author <a href="mailto:bliong@melcoe.mq.edu.au">Bruc Liong, MELCOE</a>
 * @author <a href="mailto:kli@melcoe.mq.edu.au">Xiang Kevin Li, MELCOE</a>
 * @author <a href="http://www.scottphillips.com">Scott Phillips</a>
 * @version $Revision$
 */
public class ShibAuthentication implements AuthenticationMethod
{
	/** log4j category */
	private static Logger log = Logger.getLogger(ShibAuthentication.class);

	/** Additional metadata mappings **/
	private static Map<String,String> metadataHeaderMap = null;

	/** Maximum length for eperson metadata fields **/
	private static final int NAME_MAX_SIZE = 64;
	private static final int PHONE_MAX_SIZE = 32;

	/** Maximum length for eperson additional metadata fields **/
	private static final int METADATA_MAX_SIZE = 1024;


	/**
	 * Authenticate the given or implicit credentials. This is the heart of the
	 * authentication method: test the credentials for authenticity, and if
	 * accepted, attempt to match (or optionally, create) an
	 * <code>EPerson</code>. If an <code>EPerson</code> is found it is set in
	 * the <code>Context</code> that was passed.
	 * 
	 * DSpace supports authentication using NetID, or email address. A user's NetID
	 * is a unique identifier from the IdP that identifies a particular user. The
	 * NetID can be of almost any form such as a unique integer, string, or with
	 * Shibboleth 2.0 you can use "targeted ids". You will need to coordinate with
	 * your Shibboleth federation or identity provider. There are three ways to
	 * supply identity information to DSpace:
	 * 
	 * 1) NetID from Shibboleth Header (best)
	 * 
	 *    The NetID-based method is superior because users may change their email
	 *    address with the identity provider. When this happens DSpace will not be 
	 *    able to associate their new address with their old account.
	 *    
	 * 2) Email address from Shibboleth Header (okay)
	 * 
	 *    In the case where a NetID header is not available or not found DSpace
	 *    will fall back to identifying a user based-upon their email address. 
	 *    
	 * 3) Tomcat's Remote User (worst)
	 * 
	 *    In the event that neither Shibboleth headers are found then as a last
	 *    resort DSpace will look at Tomcat's remote user field. This is the least
	 *    attractive option because Tomcat has no way to supply additional 
	 *    attributes about a user. Because of this the autoregister option is not
	 *    supported if this method is used.
	 *    
	 * Identity Scheme Migration Strategies:
	 * 
	 * If you are currently using Email based authentication (either 1 or 2) and
	 * want to upgrade to NetID based authentication then there is an easy path.
	 * Simply enable Shibboleth to pass the NetID attribute and set the netid-header
	 * below to the correct value. When a user attempts to log in to DSpace first
	 * DSpace will look for an EPerson with the passed NetID, however when this
	 * fails DSpace will fall back to email based authentication. Then DSpace will
	 * update the user's EPerson account record to set their netid so all future
	 * authentications for this user will be based upon netid. One thing to note
	 * is that DSpace will prevent an account from switching NetIDs. If an account
	 * already has a NetID set and then they try and authenticate with a
	 * different NetID the authentication will fail. 
	 * 
	 * @param context
	 *            DSpace context, will be modified (ePerson set) upon success.
	 * 
	 * @param username
	 *            Username (or email address) when method is explicit. Use null
	 *            for implicit method.
	 * 
	 * @param password
	 *            Password for explicit auth, or null for implicit method.
	 * 
	 * @param realm
	 *            Not used by Shibboleth-based authentication
	 * 
	 * @param request
	 *            The HTTP request that started this operation, or null if not
	 *            applicable.
	 * 
	 * @return One of: SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER,
	 *         BAD_ARGS
	 *         <p>
	 *         Meaning: <br>
	 *         SUCCESS - authenticated OK. <br>
	 *         BAD_CREDENTIALS - user exists, but credentials (e.g. passwd)
	 *         don't match <br>
	 *         CERT_REQUIRED - not allowed to login this way without X.509 cert.
	 *         <br>
	 *         NO_SUCH_USER - user not found using this method. <br>
	 *         BAD_ARGS - user/pw not appropriate for this method
	 */
	public int authenticate(Context context, String username, String password,
			String realm, HttpServletRequest request) throws SQLException {

		// Check if sword compatibility is allowed, and if so see if we can
		// authenticate based upon a username and password. This is really helpful
		// if your repo uses Shibboleth but you want some accounts to be able use 
		// sword. This allows this compatibility without installing the password-based
		// authentication method which has side effects such as allowing users to login
		// with a username and password from the webui.
		boolean swordCompatibility = ConfigurationManager.getBooleanProperty("authentication-shibboleth","sword.compatibility", true);
		if ( swordCompatibility &&
				username != null && username.length() > 0 &&
				password != null && password.length() > 0 ) {
			return swordCompatibility(context, username, password, request);
		}
		
		if (request == null) {
			log.warn("Unable to authenticate using Shibboleth because the request object is null.");
			return BAD_ARGS;
		}

		// Initialize the additional EPerson metadata.
        initialize(context);

		// Log all headers received if debugging is turned on. This is enormously
		// helpful when debugging shibboleth related problems.
		if (log.isDebugEnabled()) {
			log.debug("Starting Shibboleth Authentication");

			String message = "Received the following headers:\n";
			@SuppressWarnings("unchecked")
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				@SuppressWarnings("unchecked")
				Enumeration<String> headerValues = request.getHeaders(headerName);
				while (headerValues.hasMoreElements()) {
					String headerValue = headerValues.nextElement();
					message += ""+headerName+"='"+headerValue+"'\n";
				}
			}
			log.debug(message);
		}

		// Should we auto register new users.
		boolean autoRegister = ConfigurationManager.getBooleanProperty("authentication-shibboleth","autoregister", true);

		// Four steps to authenticate a user
		try {
			// Step 1: Identify User
			EPerson eperson = findEPerson(context, request);

			// Step 2: Register New User, if necessary
			if (eperson == null && autoRegister)
				eperson = registerNewEPerson(context, request);

			if (eperson == null) 
				return AuthenticationMethod.NO_SUCH_USER;

			// Step 3: Update User's Metadata
			updateEPerson(context, request, eperson);

			// Step 4: Log the user in.
			context.setCurrentUser(eperson);
			request.getSession().setAttribute("shib.authenticated", true);
			AuthenticationManager.initEPerson(context, request, eperson);

			log.info(eperson.getEmail()+" has been authenticated via shibboleth.");
			return AuthenticationMethod.SUCCESS;

		} catch (Throwable t) {
			// Log the error, and undo the authentication before returning a failure.
			log.error("Unable to successfully authenticate using shibboleth for user because of an exception.",t);
			context.setCurrentUser(null);
			return AuthenticationMethod.NO_SUCH_USER;
		}
	}

    /**
     * Get list of extra groups that user implicitly belongs to. Note that this
     * method will be invoked regardless of the authentication status of the
     * user (logged-in or not) e.g. a group that depends on the client
     * network-address.
     * 
     * DSpace is able to place users into pre-defined groups based upon values
     * received from Shibboleth. Using this option you can place all faculty members
     * into a DSpace group when the correct affiliation's attribute is provided.
     * When DSpace does this they are considered 'special groups', these are really
     * groups but the user's membership within these groups is not recorded in the
     * database. Each time a user authenticates they are automatically placed within
     * the pre-defined DSpace group, so if the user loses their affiliation then the
     * next time they login they will no longer be in the group.
     * 
     * Depending upon the shibboleth attributed use in the role-header, it may be
     * scoped. Scoped is shibboleth terminology for identifying where an attribute
     * originated from. For example a students affiliation may be encoded as
     * "student@tamu.edu". The part after the @ sign is the scope, and the preceding
     * value is the value. You may use the whole value or only the value or scope.
     * Using this you could generate a role for students and one institution
     * different than students at another institution. Or if you turn on
     * ignore-scope you could ignore the institution and place all students into
     * one group.
     * 
     * The values extracted (a user may have multiple roles) will be used to look
     * up which groups to place the user into. The groups are defined as
     * "authentication.shib.role.<role-name>" which is a comma separated list of
     * DSpace groups. 
     * 
     * @param context
     *            A valid DSpace context.
     * 
     * @param request
     *            The request that started this operation, or null if not
     *            applicable.
     * 
     * @return array of EPerson-group IDs, possibly 0-length, but never
     *         <code>null</code>.
     */
	public int[] getSpecialGroups(Context context, HttpServletRequest request)
	{
		try {
			// If we have already calculated the special groups then return them.
			if (request.getSession().getAttribute("shib.specialgroup") != null)
			{
				log.debug("Returning cached special groups.");
				return (int[]) 
				request.getSession().getAttribute("shib.specialgroup");
			}

			log.debug("Starting to determine special groups");
			String defaultRoles = ConfigurationManager.getProperty("authentication-shibboleth","default-roles");
			String roleHeader = ConfigurationManager.getProperty("authentication-shibboleth","role-header");
			boolean ignoreScope = ConfigurationManager.getBooleanProperty("authentication-shibboleth","role-header.ignore-scope", true);
			boolean ignoreValue = ConfigurationManager.getBooleanProperty("authentication-shibboleth","role-header.ignore-value", false);

			if (ignoreScope && ignoreValue) {
				throw new IllegalStateException("Both config parameters for ignoring an roll attributes scope and value are turned on, this is not a permissable configuration. (Note: ignore-scope defaults to true) The configuration parameters are: 'authentication.shib.role-header.ignore-scope' and 'authentication.shib.role-header.ignore-value'");
			}

			// Get the Shib supplied affiliation or use the default affiliation
                        List<String> affiliations = new ArrayList<String>();
                        if (context.getCurrentUser().getMetadata(roleHeader) != null) {
				affiliations.add(context.getCurrentUser().getMetadata(roleHeader));
                        }
			if (affiliations == null) {
				if (defaultRoles != null)
					affiliations = Arrays.asList(defaultRoles.split(","));
				log.debug("Failed to find Shibboleth role header, '"+roleHeader+"', falling back to the default roles: '"+defaultRoles+"'");
			} else {
				log.debug("Found Shibboleth role header: '"+roleHeader+"' = '"+affiliations+"'");
			}

			// Loop through each affiliation
			Set<Integer> groups = new HashSet<Integer>();
			if (affiliations != null) {
				for ( String affiliation : affiliations) {
					// If we ignore the affiliation's scope then strip the scope if it exists.
					if (ignoreScope) {
						int index = affiliation.indexOf('@');
						if (index != -1) {
							affiliation = affiliation.substring(0, index);
						}
					} 
					// If we ignore the value, then strip it out so only the scope remains.
					if (ignoreValue) {
						int index = affiliation.indexOf('@');
						if (index != -1) {
							affiliation = affiliation.substring(index+1, affiliation.length());
						}
					} 
                                        // Replace spaces with underscores
					affiliation = affiliation.replace(" ", "_");

					// Get the group names
					String groupNames = ConfigurationManager.getProperty("authentication-shibboleth","role." + affiliation);
					if (groupNames == null || groupNames.trim().length() == 0) {
						groupNames = ConfigurationManager.getProperty("authentication-shibboleth","role." + affiliation.toLowerCase());
					}

					if (groupNames == null) {
						log.debug("Unable to find role mapping for the value, '"+affiliation+"', there should be a mapping in config/modules/authentication-shibboleth.cfg:  role."+affiliation+" = <some group name>");
						continue;
					} else {
						log.debug("Mapping role affiliation to DSpace group: '"+groupNames+"'");
					}

					// Add each group to the list.
					String[] names = groupNames.split(",");
					for (int i = 0; i < names.length; i++) {
						try {
							Group group = Group.findByName(context,names[i].trim());
							if (group != null)
								groups.add(group.getID());
							else 
								log.debug("Unable to find group: '"+names[i].trim()+"'");
						} catch (SQLException sqle) {
							log.error("Exception thrown while trying to lookup affiliation role for group name: '"+names[i].trim()+"'",sqle);
						}
					} // for each groupNames
				} // foreach affiliations
			} // if affiliations


			log.info("Added current EPerson to special groups: "+groups);

			// Convert from a Java Set to primitive int array
			int groupIds[] = new int[groups.size()];
			Iterator<Integer> it = groups.iterator();
			for (int i = 0; it.hasNext(); i++) {
				groupIds[i] = it.next();
			}

			// Cache the special groups, so we don't have to recalculate them again
			// for this session.
			request.getSession().setAttribute("shib.specialgroup", groupIds);

			return groupIds;
		} catch (Throwable t) {
			log.error("Unable to validate any sepcial groups this user may belong too because of an exception.",t);
			return new int[0];
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
			HttpServletRequest request, String email) throws SQLException {
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
		return false;
	}

	/**
	 * Indicate whether or not a particular user can self-register, based on
	 * e-mail address.
	 * 
	 * @param context
	 *            DSpace context
	 * @param request
	 *            HTTP request, in case anything in that is used to decide
	 * @param username
	 *            e-mail address of user attempting to register
	 * 
	 */
	public boolean canSelfRegister(Context context, HttpServletRequest request,
			String username) throws SQLException {

		// Shibboleth will auto create accounts if configured to do so, but that is not
		// the same as self register. Self register means that the user can sign up for
		// an account from the web. This is not supported with shibboleth.
		return false;
	}

	/**
	 * Initialize a new e-person record for a self-registered new user.
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
			EPerson eperson) throws SQLException {
		// We don't do anything because all our work is done authenticate and special groups.
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
		// If this server is configured for lazy sessions then use this to
		// login, otherwise default to the protected shibboleth url.

		boolean lazySession = ConfigurationManager.getBooleanProperty("authentication-shibboleth","lazysession", false);

		if ( lazySession ) {
			String shibURL = ConfigurationManager.getProperty("authentication-shibboleth","lazysession.loginurl");
			boolean forceHTTPS = ConfigurationManager.getBooleanProperty("authentication-shibboleth","lazysession.secure",true);

			// Shibboleth authentication initiator 
			if (shibURL == null || shibURL.length() == 0)
				shibURL = "/Shibboleth.sso/Login";
			shibURL = shibURL.trim();

			// Determine the return URL, where shib will send the user after authenticating. We need it to go back
			// to DSpace's shibboleth-login url so the we will extract the user's information and locally
			// authenticate them.
			String host = request.getServerName();
			int port = request.getServerPort();
			String contextPath = request.getContextPath();

			String returnURL;
			if (request.isSecure() || forceHTTPS)
				returnURL = "https://";
			else 
				returnURL = "http://";

			returnURL += host;
			if (!(port == 443 || port == 80))
				returnURL += ":" + port;
			returnURL += "/" + contextPath + "/shibboleth-login";

			try {
				shibURL += "?target="+URLEncoder.encode(returnURL, "UTF-8");
			} catch (UnsupportedEncodingException uee) {
				log.error("Unable to generate lazysession authentication",uee);
			}

			log.debug("Redirecting user to Shibboleth initiator: "+shibURL);

			return response.encodeRedirectURL(shibURL);
		} else {
			// If we are not using lazy sessions rely on the protected URL.
			return response.encodeRedirectURL(request.getContextPath()
					+ "/shibboleth-login");
		}
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


	/**
	 * Identify an existing EPerson based upon the shibboleth attributes provided on
	 * the request object. There are three cases where this can occurr, each as
	 * a fallback for the previous method.
	 * 
	 * 1) NetID from Shibboleth Header (best)
	 *    The NetID-based method is superior because users may change their email
	 *    address with the identity provider. When this happens DSpace will not be 
	 *    able to associate their new address with their old account.
	 * 
	 * 2) Email address from Shibboleth Header (okay)
	 *    In the case where a NetID header is not available or not found DSpace
	 *    will fall back to identifying a user based upon their email address. 
	 *    
	 * 3) Tomcat's Remote User (worst)
	 *    In the event that neither Shibboleth headers are found then as a last
	 *    resort DSpace will look at Tomcat's remote user field. This is the least
	 *    attractive option because Tomcat has no way to supply additional 
	 *    attributes about a user. Because of this the autoregister option is not
	 *    supported if this method is used.
	 *    
	 * If successful then the identified EPerson will be returned, otherwise null.
	 * 
	 * @param context The DSpace database context
	 * @param request The current HTTP Request
	 * @return The EPerson identified or null.
	 */
	private EPerson findEPerson(Context context, HttpServletRequest request) throws SQLException, AuthorizeException {

		boolean isUsingTomcatUser = ConfigurationManager.getBooleanProperty("authentication-shibboleth","email-use-tomcat-remote-user");
		String netidHeader = ConfigurationManager.getProperty("authentication-shibboleth","netid-header");
		String emailHeader = ConfigurationManager.getProperty("authentication-shibboleth","email-header");

		EPerson eperson = null;
		boolean foundNetID = false;
		boolean foundEmail = false;
		boolean foundRemoteUser = false;


		// 1) First, look for a netid header.
		if (netidHeader != null) {
			String netid = findSingleAttribute(request,netidHeader);

			if (netid != null) {
				foundNetID = true;
				eperson = EPerson.findByNetid(context, netid);

				if (eperson == null)
					log.info("Unable to identify EPerson based upon Shibboleth netid header: '"+netidHeader+"'='"+netid+"'.");
				else
					log.debug("Identified EPerson based upon Shibboleth netid header: '"+netidHeader+"'='"+netid+"'.");
			}
		}

		// 2) Second, look for an email header.
		if (eperson == null && emailHeader != null) {
			String email = findSingleAttribute(request,emailHeader);

			if (email != null) {
				foundEmail = true;
				email = email.toLowerCase();
				eperson = EPerson.findByEmail(context, email);

				if (eperson == null)
					log.info("Unable to identify EPerson based upon Shibboleth email header: '"+emailHeader+"'='"+email+"'.");
				else
					log.info("Identified EPerson based upon Shibboleth email header: '"+emailHeader+"'='"+email+"'.");

				if (eperson != null && eperson.getNetid() != null) {
					// If the user has a netID it has been locked to that netid, don't let anyone else try and steal the account.
					log.error("The identified EPerson based upon Shibboleth email header, '"+emailHeader+"'='"+email+"', is locked to another netid: '"+eperson.getNetid()+"'. This might be a possible hacking attempt to steal another users credentials. If the user's netid has changed you will need to manually change it to the correct value or unset it in the database.");
					eperson = null;
				}
			}
		}

		// 3) Last, check to see if tomcat is passing a user.
		if (eperson == null && isUsingTomcatUser) {
			String email = request.getRemoteUser();

			if (email != null) {
				foundRemoteUser = true;
				email = email.toLowerCase();
				eperson = EPerson.findByEmail(context, email);

				if (eperson == null)
					log.info("Unable to identify EPerson based upon Tomcat's remote user: '"+email+"'.");
				else
					log.info("Identified EPerson based upon Tomcat's remote user: '"+email+"'.");

				if (eperson != null && eperson.getNetid() != null) {
					// If the user has a netID it has been locked to that netid, don't let anyone else try and steal the account.
					log.error("The identified EPerson based upon Tomcat's remote user, '"+email+"', is locked to another netid: '"+eperson.getNetid()+"'. This might be a possible hacking attempt to steal another users credentials. If the user's netid has changed you will need to manually change it to the correct value or unset it in the database.");
					eperson = null;
				}
			}
		}

		if (!foundNetID && !foundEmail && !foundRemoteUser) {
			log.error("Shibboleth authentication was not able to find a NetId, Email, or Tomcat Remote user for which to indentify a user from.");
		}


		return eperson;
	}


	/**
	 * Register a new eperson object. This method is called when no existing user was
	 * found for the NetID or Email and autoregister is enabled. When these conditions
	 * are met this method will create a new eperson object. 
	 * 
	 * In order to create a new eperson object there is a minimal set of metadata
	 * required: Email, First Name, and Last Name. If we don't have access to these
	 * three pieces of information then we will be unable to create a new eperson
	 * object, such as the case when Tomcat's Remote User field is used to identify
	 * a particular user.
	 * 
	 * Note, that this method only adds the minimal metadata. Any additional metadata
	 * will need to be added by the updateEPerson method.
	 *
	 * @param context The current DSpace database context
	 * @param request The current HTTP Request
	 * @return A new eperson object or null if unable to create a new eperson.
	 */
	private EPerson registerNewEPerson(Context context, HttpServletRequest request) throws SQLException, AuthorizeException {

		// Header names
		String netidHeader = ConfigurationManager.getProperty("authentication-shibboleth","netid-header");
		String emailHeader = ConfigurationManager.getProperty("authentication-shibboleth","email-header");
		String fnameHeader = ConfigurationManager.getProperty("authentication-shibboleth","firstname-header");
		String lnameHeader = ConfigurationManager.getProperty("authentication-shibboleth","lastname-header");

		// Header values
		String netid = findSingleAttribute(request,netidHeader);
		String email = findSingleAttribute(request,emailHeader);
		String fname = findSingleAttribute(request,fnameHeader);
		String lname = findSingleAttribute(request,lnameHeader);

		if ( email == null || (fnameHeader != null && fname == null) || (lnameHeader != null && lname == null)) {
			// We require that there be an email, first name, and last name. If we
			// don't have at least these three pieces of information then we fail.
			String message = "Unable to register new eperson because we are unable to find an email address along with first and last name for the user.\n";
			message += "  NetId Header: '"+netidHeader+"'='"+netid+"' (Optional) \n";
			message += "  Email Header: '"+emailHeader+"'='"+email+"' \n";
			message += "  First Name Header: '"+fnameHeader+"'='"+fname+"' \n";
			message += "  Last Name Header: '"+lnameHeader+"'='"+lname+"'";
			log.error(message);

			return null; // TODO should this throw an exception?
		}

		// Truncate values of parameters that are too big.
		if (fname != null && fname.length() > NAME_MAX_SIZE) {
			log.warn("Truncating eperson's first name because it is longer than "+NAME_MAX_SIZE+": '"+fname+"'");
			fname = fname.substring(0,NAME_MAX_SIZE);
		}
		if (lname != null && lname.length() > NAME_MAX_SIZE) {
			log.warn("Truncating eperson's last name because it is longer than "+NAME_MAX_SIZE+": '"+lname+"'");
			lname = lname.substring(0,NAME_MAX_SIZE);
		}

		// Turn off authorizations to create a new user
		context.turnOffAuthorisationSystem();
		EPerson eperson = EPerson.create(context);

		// Set the minimum attributes for the new eperson
		if (netid != null)
			eperson.setNetid(netid);
		eperson.setEmail(email.toLowerCase());
		if ( fname != null )
			eperson.setFirstName(fname);
		if ( lname != null )
			eperson.setLastName(lname);
		eperson.setCanLogIn(true);

		// Commit the new eperson
		AuthenticationManager.initEPerson(context, request, eperson);
		eperson.update();
		context.commit();

		// Turn authorizations back on.
		context.restoreAuthSystemState();

		if (log.isInfoEnabled()) {
			String message = "Auto registered new eperson using Shibboleth-based attributes:";
			if (netid != null)
				message += "  NetId: '"+netid+"'\n";
			message += "  Email: '"+email+"' \n";
			message += "  First Name: '"+fname+"' \n";
			message += "  Last Name: '"+lname+"'";
			log.info(message);
		}

		return eperson;
	}





	/**
	 * After we successfully authenticated a user, this method will update the user's attributes. The
	 * user's email, name, or other attribute may have been changed since the last time they
	 * logged into DSpace. This method will update the database with their most recent information.
	 * 
	 * This method handles the basic DSpace metadata (email, first name, last name) along with
	 * additional metadata set using the setMetadata() methods on the eperson object. The
	 * additional metadata are defined by a mapping created in the dspace.cfg.
	 * 
	 * @param context The current DSpace database context
	 * @param request The current HTTP Request
	 * @param eperson The eperson object to update.
	 */
	private void updateEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException, AuthorizeException {

		// Header names & values
		String netidHeader = ConfigurationManager.getProperty("authentication-shibboleth","netid-header");
		String emailHeader = ConfigurationManager.getProperty("authentication-shibboleth","email-header");
		String fnameHeader = ConfigurationManager.getProperty("authentication-shibboleth","firstname-header");
		String lnameHeader = ConfigurationManager.getProperty("authentication-shibboleth","lastname-header");

		String netid = findSingleAttribute(request,netidHeader);
		String email = findSingleAttribute(request,emailHeader);
		String fname = findSingleAttribute(request,fnameHeader);
		String lname = findSingleAttribute(request,lnameHeader);

		// Truncate values of parameters that are too big.
		if (fname != null && fname.length() > NAME_MAX_SIZE) {
			log.warn("Truncating eperson's first name because it is longer than "+NAME_MAX_SIZE+": '"+fname+"'");
			fname = fname.substring(0,NAME_MAX_SIZE);
		}
		if (lname != null && lname.length() > NAME_MAX_SIZE) {
			log.warn("Truncating eperson's last name because it is longer than "+NAME_MAX_SIZE+": '"+lname+"'");
			lname = lname.substring(0,NAME_MAX_SIZE);
		}

		context.turnOffAuthorisationSystem();

		// 1) Update the minimum metadata
		if (netid != null && eperson.getNetid() == null)
			// Only update the netid if none has been previously set. This can occur when a repo switches 
			// to netid based authentication. The current users do not have netids and fall back to email-based
			// identification but once they login we update their record and lock the account to a particular netid.
			eperson.setNetid(netid);
		if (email != null)
			// The email could have changed if using netid based lookup.
			eperson.setEmail(email.toLowerCase());
		if (fname != null)
			eperson.setFirstName(fname);
		if (lname != null)
			eperson.setLastName(lname);

		if (log.isDebugEnabled()) {
			String message = "Updated the eperson's minimal metadata: \n";
			message += " Email Header: '"+emailHeader+"' = '"+email+"' \n";
			message += " First Name Header: '"+fnameHeader+"' = '"+fname+"' \n";
			message += " Last Name Header: '"+fnameHeader+"' = '"+lname+"'";
			log.debug(message);
		}

		// 2) Update additional eperson metadata
		for (String header : metadataHeaderMap.keySet()) {

			String field = metadataHeaderMap.get(header);
			String value = findSingleAttribute(request, header);

			// Truncate values
			if (value == null) {
				log.warn("Unable to update the eperson's '"+field+"' metadata because the header '"+header+"' does not exist.");
				continue;
			} else if ("phone".equals(field) && value.length() > PHONE_MAX_SIZE) {
				log.warn("Truncating eperson phone metadata because it is longer than "+PHONE_MAX_SIZE+": '"+value+"'");
				value = value.substring(0,PHONE_MAX_SIZE);
			} else if (value.length() > METADATA_MAX_SIZE) {
				log.warn("Truncating eperson "+field+" metadata because it is longer than "+METADATA_MAX_SIZE+": '"+value+"'");
				value = value.substring(0,METADATA_MAX_SIZE);
			}

			eperson.setMetadata(field, value);
			log.debug("Updated the eperson's '"+field+"' metadata using header: '"+header+"' = '"+value+"'.");
		}
		eperson.update();
		context.commit();
		context.restoreAuthSystemState();
	}

	/**
	 * Provide password-based authentication to enable sword compatibility. 
	 * 
	 * Sword compatibility will allow this authentication method to work when using
	 * sword. Sword relies on username and password based authentication and is
	 * entirely incapable of supporting shibboleth. This option allows you to
	 * authenticate username and passwords for sword sessions without adding
	 * another authentication method onto the stack. You will need to ensure that
	 * a user has a password. One way to do that is to create the user via the
	 * create-administrator command line command and then edit their permissions.
	 * 
	 * @param context The DSpace database context
	 * @param username The username
	 * @param password The password
	 * @param request The HTTP Request
	 * @return A valid DSpace Authentication Method status code.
	 */
	protected int swordCompatibility(Context context, String username, String password, HttpServletRequest request) throws SQLException {

		EPerson eperson = null;

		log.debug("Shibboleth Sword compatibility activated.");
		try {
			eperson = EPerson.findByEmail(context, username.toLowerCase());
		} catch (AuthorizeException ae) {
			// ignore exception, treat it as lookup failure.
		}

		if (eperson == null) {
			// lookup failed.
			log.error("Shibboleth-based password authentication failed for user "+username+" because no such user exists.");
			return NO_SUCH_USER;
		} else if (!eperson.canLogIn()) {
			// cannot login this way
			log.error("Shibboleth-based password authentication failed for user "+username+" because the eperson object is not allowed to login.");
			return BAD_ARGS;
		} else if (eperson.getRequireCertificate()) {
			// this user can only login with x.509 certificate
			log.error("Shibboleth-based password authentication failed for user "+username+" because the eperson object requires a certificate to authenticate..");
			return CERT_REQUIRED;
		}

		else if (eperson.checkPassword(password)) {
			// Password matched
			AuthenticationManager.initEPerson(context, request, eperson);
			context.setCurrentUser(eperson);
			log.info(eperson.getEmail()+" has been authenticated via shibboleth using password-based sword compatibility mode.");
			return SUCCESS;
		} else {
			// Passsword failure
			log.error("Shibboleth-based password authentication failed for user "+username+" because a bad password was supplied.");
			return BAD_CREDENTIALS;
		}

	}



	/**
	 * Initialize Shibboleth Authentication. 
	 * 
	 * During initalization the mapping of additional eperson metadata will be loaded from the DSpace.cfg
	 * and cached. While loading the metadata mapping this method will check the EPerson object to see
	 * if it supports the metadata field. If the field is not supported and autocreate is turned on then
	 * the field will be automatically created.
	 * 
	 * It is safe to call this methods multiple times.
     * @param context
	 */
    private synchronized static void initialize(Context context) throws SQLException {

		if (metadataHeaderMap != null)
			return;


		HashMap<String, String> map = new HashMap<String,String>();

		String mappingString = ConfigurationManager.getProperty("authentication-shibboleth","eperson.metadata");
		boolean autoCreate = ConfigurationManager.getBooleanProperty("authentication-shibboleth","eperson.metadata.autocreate", true);

		// Bail out if not set, returning an empty map.
		if (mappingString == null || mappingString.trim().length() == 0) {
			log.debug("No additional eperson metadata mapping found: authentication.shib.eperson.metadata");

			metadataHeaderMap = map;
			return;
		}

		log.debug("Loading additional eperson metadata from: 'authentication.shib.eperson.metadata' = '"+mappingString+"'");


		String[] metadataStringList =  mappingString.split(",");
		for (String metadataString : metadataStringList) {
			metadataString = metadataString.trim();

			String[] metadataParts = metadataString.split("=>");

			if (metadataParts.length != 2) {
				log.error("Unable to parse metadat mapping string: '"+metadataString+"'");
				continue;
			}

			String header = metadataParts[0].trim();
			String name = metadataParts[1].trim().toLowerCase();

            boolean valid = checkIfEpersonMetadataFieldExists(name,context);

			if ( ! valid && autoCreate) {
                valid = autoCreateEpersonMetadataField(name,context);
			}

			if (valid) {
				// The eperson field is fine, we can use it.
				log.debug("Loading additional eperson metadata mapping for: '"+header+"' = '"+name+"'");
				map.put(header, name);
			} else {
				// The field doesn't exist, and we can't use it.
				log.error("Skipping the additional eperson metadata mapping for: '"+header+"' = '"+name+"' because the field is not supported by the current configuration.");
			}
		} // foreach metadataStringList


		metadataHeaderMap = map;
		return;
	}

	/**
     * Check if a MetadataField for an eperson is available.
	 * 
	 * @param metadataName The name of the metadata field.
     * @param context
	 * @return True if a valid metadata field, otherwise false.
	 */
    private static synchronized boolean checkIfEpersonMetadataFieldExists(String metadataName, Context context) throws SQLException {

		if (metadataName == null)
			return false;

		if ("phone".equals(metadataName))
			// The phone is a predefined field
			return true;

        MetadataSchema schemaObj = MetadataSchema.find(context, "eperson");

        if (schemaObj == null)
        {
            log.error("Schema eperson is not registered and does not exist.");
        } else
        {
            MetadataField eperson = MetadataField.findByElement(context, schemaObj.getSchemaID(), metadataName, null);

            if ( eperson!=null) {
                return true;
				}
            else {
                log.error("Unable to find eperson named: '" + metadataName + "'");
            }
        }

		return false;
	}

	/** Validate Postgres Column Names */
	private static final String COLUMN_NAME_REGEX = "^[_A-Za-z0-9]+$";
	
	/**
     * Automattically create a new metadataField for an eperson
	 * 
	 * @param metadataName The name of the new metadata field.
	 * @return True if successful, otherwise false.
	 */
    private static synchronized boolean autoCreateEpersonMetadataField(String metadataName, Context context) throws SQLException {

		if (metadataName == null)
			return false;

		if ("phone".equals(metadataName))
			// The phone is a predefined field
			return true;

		if ( ! metadataName.matches(COLUMN_NAME_REGEX))
			return false;


        MetadataSchema schemaObj = MetadataSchema.find(context, "eperson");

        if (schemaObj == null)
        {
            log.error("Schema eperson is not registered and does not exist.");
        } else {
            MetadataField metadataField = new MetadataField(schemaObj, metadataName, null, null);
		try {
                metadataField.create(context);
            } catch (IOException e) {
                log.error("Unable to auto-create the eperson MetadataField with MetadataName " + metadataName ,e);
                return false;
            } catch (AuthorizeException e) {
                log.error("Unauthorized to auto-create the eperson MetadataField with MetadataName " + metadataName + " MetadataField", e);
                return false;
            } catch (NonUniqueMetadataException e) {
                log.error("The eperson MetadataField with MetadataName " + metadataName +" already exists",e);
                return false;
            }


            log.info("Auto created the eperson metadataField: '" + metadataName + "'");
			return true;

        }
			return false;
	}


	/**
	 * Find a particular Shibboleth header value and return the all values.
	 * The header name uses a bit of fuzzy logic, so it will first try case
	 * sensitive, then it will try lowercase, and finally it will try uppercase.
	 * 
	 * This method will not interpret the header value in any way.
	 * 
	 * 
	 * @param request The HTTP request to look for values in.
	 * @param name The name of the attribute or header
	 * @return The value of the attribute or header requested, or null if none found.
	 */
	private String findAttribute(HttpServletRequest request, String name) {
		if ( name == null ) {
			return null;
		}
		// First try to get the value from the attribute
		String value = (String) request.getAttribute(name);
		if (StringUtils.isEmpty(value))
			value = (String) request.getAttribute(name.toLowerCase());
		if (StringUtils.isEmpty(value))
			value = (String) request.getAttribute(name.toUpperCase());

		// Second try to get the value from the header
		if (StringUtils.isEmpty(value))
		    value = request.getHeader(name);
		if (StringUtils.isEmpty(value))
			value = request.getHeader(name.toLowerCase());
		if (StringUtils.isEmpty(value))
			value = request.getHeader(name.toUpperCase());
                
                boolean reconvertAttributes = 
                        ConfigurationManager.getBooleanProperty(
                            "authentication-shibboleth",
                            "reconvert.attributes",
                            false);
                
                if (!StringUtils.isEmpty(value) && reconvertAttributes)
                {
                    try {
                        value = new String(value.getBytes("ISO-8859-1"), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        log.warn("Failed to reconvert shibboleth attribute ("
                                + name + ").", ex);
                    }
                }
		
		return value;
	}


	/**
	 * Find a particular Shibboleth header value and return the first value.
	 * The header name uses a bit of fuzzy logic, so it will first try case
	 * sensitive, then it will try lowercase, and finally it will try uppercase.
	 * 
	 * Shibboleth attributes may contain multiple values separated by a
	 * semicolon. This method will return the first value in the attribute. If
	 * you need multiple values use findMultipleAttributes instead.
	 * 
	 * If no attribute is found then null is returned.
	 * 
	 * @param request The HTTP request to look for headers values on.
	 * @param name The name of the header
	 * @return The value of the header requested, or null if none found.
	 */
	private String findSingleAttribute(HttpServletRequest request, String name) {
		if ( name == null) {
			return null;
		}

		String value = findAttribute(request, name);


		if (value != null) {
			// If there are multiple values encoded in the shibboleth attribute
			// they are separated by a semicolon, and any semicolons in the
			// attribute are escaped with a backslash. For this case we are just 
			// looking for the first attribute so we scan the value until we find 
			// the first unescaped semicolon and chop off everything else.
			int idx = 0;
			do {
				idx = value.indexOf(';',idx);
				if ( idx != -1 && value.charAt(idx-1) != '\\') {
					value = value.substring(0,idx);
					break;
				}
			} while (idx >= 0);

			// Unescape the semicolon after splitting
			value = value.replaceAll("\\;", ";");
		}

		return value;
	}

	/**
	 * Find a particular Shibboleth hattributeeader value and return the values.
	 * The attribute name uses a bit of fuzzy logic, so it will first try case
	 * sensitive, then it will try lowercase, and finally it will try uppercase.
	 * 
	 * Shibboleth attributes may contain multiple values separated by a
	 * semicolon and semicolons are escaped with a backslash. This method will
	 * split all the attributes into a list and unescape semicolons.
	 * 
	 * If no attributes are found then null is returned.
	 * 
	 * @param request The HTTP request to look for headers values on.
	 * @param name The name of the attribute
	 * @return The list of values found, or null if none found.
	 */
	private List<String> findMultipleAttributes(HttpServletRequest request, String name) {
		String values = findAttribute(request, name);

		if (values == null)
			return null;

		// Shibboleth attributes are separated by semicolons (and semicolons are 
		// escaped with a backslash). So here we will scan through the string and 
		// split on any unescaped semicolons.
		List<String> valueList = new ArrayList<String>();
		int idx = 0;
		do {
			idx = values.indexOf(';',idx);

			if ( idx == 0 ) { 
				// if the string starts with a semicolon just remove it. This will
				// prevent an endless loop in an error condition.
				values = values.substring(1,values.length());

			} else if (idx > 0 && values.charAt(idx-1) == '\\' ) {
				// The attribute starts with an escaped semicolon
				idx++;
			} else if ( idx > 0) {
				// First extract the value and store it on the list.
				String value = values.substring(0,idx);	
				value = value.replaceAll("\\\\;", ";");
				valueList.add(value);

				// Next, remove the value from the string and continue to scan.
				values = values.substring(idx+1,values.length());
				idx = 0;
			}
		} while (idx >= 0);

		// The last attribute will still be left on the values string, put it 
		// into the list.
		if (values.length() > 0) {
			values = values.replaceAll("\\\\;", ";");
			valueList.add(values);
		}

		return valueList;
	}


}

