/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.authenticate;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.dspace.authenticate.shibboleth.ShibEPerson;
import cz.cuni.mff.ufal.dspace.authenticate.shibboleth.ShibGroup;
import cz.cuni.mff.ufal.dspace.authenticate.shibboleth.ShibHeaders;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

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
 * based on class by:
 * <a href="mailto:bliong@melcoe.mq.edu.au">Bruc Liong, MELCOE</a>
 * <a href="mailto:kli@melcoe.mq.edu.au">Xiang Kevin Li, MELCOE</a>
 * <a href="http://www.scottphillips.com">Scott Phillips</a>
 * modified for LINDAT/CLARIN
 * @version $Revision$
 */
public class ShibAuthentication implements AuthenticationMethod
{
    /** log4j category */
    private static Logger log = cz.cuni.mff.ufal.Logger.getLogger(ShibAuthentication.class);

    /** Additional metadata mappings **/
    private static Map<String,String> metadataHeaderMap = null;

    /** Maximum length for eperson metadata fields **/
    private static final int NAME_MAX_SIZE = 64;
    private static final int PHONE_MAX_SIZE = 32;


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
     * your shibboleth federation or identity provider. There are three ways to
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
     * Simply enable shibboleth to pass the NetID attribute and set the netid-header
     * below to the correct value. When a user attempts to log in to DSpace first
     * DSpace will look for an EPerson with the passed NetID, however when this
     * fails DSpace will fall back to email based authentication. Then DSpace will
     * update the user's EPerson account record to set their netted so all future
     * authentications for this user will be based upon netted. One thing to note
     * is that DSpace will prevent an account from switching NetIDs. If an account
     * all ready has a NetID set and then they try and authenticate with a
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
    @Override
    public int authenticate(Context context, String username, String password,
            String realm, HttpServletRequest request) throws SQLException {

        // Check if sword compatability is allowed, and if so see if we can
        // authenticate based upon a username and password. This is really helpfull
        // if your repo uses shibboleth but you want some accounts to be able use
        // sword. This allows this compatability without installing the password-based
        // authentication method which has side effects such as allowing users to login
        // with a username and password from the webui.
        boolean swordCompatability = ConfigurationManager.getBooleanProperty("authentication-shibboleth","sword.compatability", true);
        if ( swordCompatability &&
                username != null && username.length() > 0 &&
                password != null && password.length() > 0 ) {
            return swordCompatability(context, username, password, request);
        }

        if (request == null) {
            log.warn("Unable to authenticate using Shibboleth because the request object is null.");
            return BAD_ARGS;
        }


        // Log all headers received if debugging is turned on. This is enormously
        // helpful when debugging shibboleth related problems.
        if (log.isDebugEnabled()) {
            log.debug("Starting Shibboleth Authentication");
        }
        ShibHeaders shibheaders = new ShibHeaders(request);
        	shibheaders.log_headers();

        String organization = shibheaders.get_idp();
        if (organization == null) {
            log.info("Exiting shibboleth authenticate because no idp set");
            return BAD_ARGS;
        }

        // Initialize the additional EPerson metadata.
        initialize();

        // Should we auto register new users.
        boolean autoRegister = ConfigurationManager.getBooleanProperty(
                        "authentication-shibboleth","autoregister", true);

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

            // if not welcomed, store info in session
            if ( null == eperson.getWelcome() &&
                    ConfigurationManager.getBooleanProperty("lr", "lr.login.welcome.message", false) ) {
                request.getSession().setAttribute("shib.welcome", shibheaders.toString());
            }

            String auth = eperson.getEmail() != null ? eperson.getEmail() : eperson.getNetid();
            log.info(auth +" has been authenticated via shibboleth.");
            return AuthenticationMethod.SUCCESS;

        } catch (Throwable t) {
            // Log the error, and undo the authentication before returning a failure.
            log.error("Unable to successfully authenticate using shibboleth for user because of an exception.",t);
            String errorMessage = DSpaceApi.getFunctionalityManager().getErrorMessage();
            if(errorMessage==null || errorMessage.equals("")) {
            	DSpaceApi.getFunctionalityManager().setErrorMessage("We couldn't finish the authentication because of an exception\n.");
            }
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
     * Depending upon the shibboleth attributed use in the role-header it may be
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
    @Override
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
        try {
            // User has not successfully authenticated via shibboleth.
            if ( request == null ||
                    context.getCurrentUser() == null ||
                    request.getSession().getAttribute("shib.authenticated") == null ) {
                return new int[0];
            }

            // If we have all ready calculated the special groups then return them.
            if (request.getSession().getAttribute("shib.specialgroup") != null)
            {
                log.debug("Returning cached special groups.");
                return (int[])
                request.getSession().getAttribute("shib.specialgroup");
            }

            int[] groupIds = new ShibGroup(new ShibHeaders(request), context).get();
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
            shibURL.trim();

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
            returnURL +=  contextPath + "/shibboleth-login";

            //try {
                //shibURL += "?target="+URLEncoder.encode(returnURL, "UTF-8");
            	shibURL += "?target=" + returnURL;
            //} catch (UnsupportedEncodingException uee) {
            //  log.error("Unable to generate lazysession authentication",uee);
            //}

            //<UFAL>
            // The following debug message is misleading, only URL is generated
            // no redirection happens
            //log.debug("Redirecting user to Shibboleth initiator: "+shibURL);
            //</UFAL>

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
    @Override
    public String loginPageTitle(Context context)
    {
        return "org.dspace.authenticate.ShibAuthentication.title";
    }


    /**
     * Identify an existing EPerson based upon the shibboleth attributes provided on
     * the request object. There are three case underwhich this can occure each in
     * a fall back position to the previous method.
     *
     * 1) NetID from Shibboleth Header (best)
     *    The NetID-based method is superior because users may change their email
     *    address with the identity provider. When this happens DSpace will not be
     *    able to associate their new address with their old account.
     *
     * 2) Email address from Shibboleth Header (okay)
     *    In the case where a NetID header is not available or not found DSpace
     *    will fall back to identifying a user based-upon their email address.
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
    private EPerson findEPerson(Context context, HttpServletRequest request) throws SQLException, AuthorizeException
    {
        EPerson eperson = null;
        ShibHeaders shib_headers = new ShibHeaders(request);

        // Getting the Shibboleth header mappings based on organization
        //
        String org = shib_headers.get_idp();
	        if ( org == null ) {
	        	return null;
	        }
    	ShibEPerson shib_person = new ShibEPerson(shib_headers, org);


        // 1) First, look for a netid header.
    	//
    	eperson = shib_person.get_by_netid(context);

        // 2) Second, look for an email header.
        //
        if (eperson == null) {
        	eperson = shib_person.get_by_email(context);
        }

        // 3) Last, check to see if tomcat is passing a user.
        if (eperson == null) {
        	eperson = shib_person.get_by_tomcat(context, request);
        }

        if ( null == eperson ) {
            log.warn("Shibboleth authentication was not able to find a " +
                    "netid (eppn, ...), email, or Tomcat Remote user for which to indentify a user from." );
        }


        return eperson;
    }

    private void set_extra_log_info(Context context, HttpServletRequest request, String organisation)
    {
    	String extraLogInfo = context.getExtraLogInfo();
        if ( extraLogInfo == null )
            extraLogInfo = "";
        //
        if (organisation == null) {
            organisation = "not available";
        }
        String info = String.format("idp=%s, ip=%s", organisation, request.getRemoteAddr());
        extraLogInfo = info + ", " + extraLogInfo;
        context.setExtraLogInfo(extraLogInfo);
    }


    /**
     * Register a new eperson object. This method is called when no existing user was
     * found for the NetID or Email and autoregister is enabled. When these conditions
     * are met this method will create a new eperson object.
     *
     * In order to create a new eperson object there is a minimal set of metadata
     * required: Email, First Name, and Last Name. If we don't have access to these
     * three peices of information then we will be unable to create a new eperson
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
    private EPerson registerNewEPerson(Context context, HttpServletRequest request) throws SQLException, AuthorizeException
    {
        ShibHeaders shib_headers = new ShibHeaders(request);

        // Getting the Shibboleth header mappings based on organization
        //
        String org = shib_headers.get_idp();
	        if ( org == null ) {
	        	return null;
	        }
    	ShibEPerson shib_person = new ShibEPerson(shib_headers, org);
    	////

        // set extra info
        set_extra_log_info(context, request, org);

        // name
        String fname = truncate_names("fname", shib_person.get_first_name(""), NAME_MAX_SIZE);
        String lname = truncate_names("lname", shib_person.get_last_name("", true), NAME_MAX_SIZE);

        IFunctionalities functionalityManger = DSpaceApi.getFunctionalityManager();

        // netid and email
        String netid = shib_person.get_first_netid();
        String email = shib_person.get_email();
        email = functionalityManger.getEmailAcceptedOrNull(email);


        if ( email == null && netid == null)
        {
            // We require that there be an email or netid. If we
            // don't have these pieces of information then we fail.
            String message = String.format(
            		"Unable to register a new eperson because we can " +
            		"find neither an email address nor a netid for the user.\n" +
            		ShibEPerson.info_to_log(),
            		netid, email, fname, lname);
            logAndSetMessage(context, request, org, message);
            //DSpaceApi.getFunctionalityManager().setErrorMessage("You could not be automatically registered. " + message);
            return null; // TODO should this throw an exception?
        }


        // Turn off authorizations to create a new user
        context.turnOffAuthorisationSystem();
        EPerson eperson = EPerson.create(context, "New Shibboleth user.");


        /* <UFAL>
         * Name encoding conversions
         */

        log.info("Got to the name conversion!");
        fname = functionalityManger.convert ( fname ) ;
        lname = functionalityManger.convert ( lname ) ;
        log.info("Name conversion passed successfully to " + fname + " " + lname + "!");

        /* </UFAL> */

        // Set the minimum attributes for the new eperson

	    init_eperson( eperson, true, netid, email, fname, lname );

        eperson.setCanLogIn(true);

        // set default language if present
        String lang = ConfigurationManager.getProperty("lr", "lr.default.eperson.language");
        if ( null != lang && 0 < lang.trim().length() ) {
            eperson.setLanguage( lang );
        }

        // Commit the new eperson
        AuthenticationManager.initEPerson(context, request, eperson);
        eperson.update();
        context.commit();


        /* <UFAL>
         *
         * Register User in the UFAL license database
         *
         */
        //if no email the registration is postponed after entering and confirming mail        
        if(email != null){
        	functionalityManger.openSession();
            try{            	
            	functionalityManger.registerUser(
                        eperson.getID(), eperson.getEmail(), org, true);
                eperson.setCanLogIn(false);
                eperson.update();
            }catch(Exception e){
                throw new AuthorizeException("User has not been added among registred users!") ;
            }
            functionalityManger.closeSession();
        }                

        /* </UFAL> */

        // Turn authorizations back on.
        context.restoreAuthSystemState();

        if (log.isInfoEnabled()) {
            String message = String.format(
	            "Auto registered new EPerson using Shibboleth-based attributes:  " +
	            ShibEPerson.info_to_log(),
	            netid, email, fname, lname);
            log.info(message);
        }

        return eperson;
    }





    /**
     * After sucessfully authenticate a user this method will update the users attributes. The
     * user's email, name, or other attribute may have been changed since the last time they
     * logged into DSpace. This method will update the database with their most recient information.
     *
     * This method handles the basic DSpace metadata (email, first name, last name) along with
     * additional metadata set using the setMetadata() methods on the eperson object. The
     * additional metadata are defined by a mapping created in the dspace.cfg.
     *
     * @param context The current DSpace database context
     * @param request The current HTTP Request
     * @param eperson The eperson object to update.
     */
    private void updateEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException, AuthorizeException
    {

        ShibHeaders shib_headers = new ShibHeaders(request);

        // Getting the Shibboleth header mappings based on organization
        //
        String org = shib_headers.get_idp();
    	ShibEPerson shib_person = new ShibEPerson(shib_headers, org);
    	////

    	IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();
        // name
        String fname = functionalityManager.convert(
        		truncate_names("fname", shib_person.get_first_name(""), NAME_MAX_SIZE));
        String lname = functionalityManager.convert(
        		truncate_names("lname", shib_person.get_last_name("", true), NAME_MAX_SIZE));

        // netid and email
        String netid = shib_person.get_first_netid();
        String email = shib_person.get_email();
        email = functionalityManager.getEmailAcceptedOrNull(email);


        context.turnOffAuthorisationSystem();
	        if (log.isDebugEnabled())
	        {
	            String message = String.format(
	            		"Updated the eperson's minimal metadata: \n" +
	    	            ShibEPerson.info_to_log(),
	    	            netid, email, fname, lname);
	            log.debug(message);
	        }

	    // 1) Update eperson minimal metadata
	    init_eperson( eperson, false, netid, email, fname, lname );
        // 2) Update additional eperson metadata
	    update_eperson(eperson, shib_headers);
        eperson.setLoggedIn();

        eperson.update();
        context.commit();
        context.restoreAuthSystemState();
    }

    //
    //

    private static boolean canUpdateField(String field, String newValue, String oldValue)
    {
        boolean res = false;
        if(newValue != null && (oldValue == null || oldValue.isEmpty()))
        {
            res = true;
        }
        return res;
    }

    private static boolean valuesDiffer(String newValue, String oldValue, boolean ignoreCase)
    {
        boolean res;

        if(ignoreCase) {
            res = (newValue == null ? oldValue == null : newValue.equalsIgnoreCase(oldValue));
        }
        else {
            res = (newValue == null ? oldValue == null : newValue.equals(oldValue));
        }

        return res;
    }

    private static void logIgnoredUpdate(String field, String newValue, String oldValue)
    {
        boolean ignoreCase = false;

        if(field.equals("email"))
        {
            ignoreCase = true;
        }

        if(valuesDiffer(newValue, oldValue, ignoreCase)) {
            log.info(String.format("EPerson's %s field update ignored (new value: '%s', old value: '%s')", field, newValue, oldValue));
        }
    }

    private static void init_eperson(EPerson eperson, boolean first_time,
    		String netid, String email, String fname, String lname)
    {
        if (netid != null && ( first_time || eperson.getNetid() == null) ) {
            // Only update the netid if none has been previously set. This can occur when a repo switches
            // to netid based authentication. The current users do not have netids and fall back to email based
            // identification but once they login we update their record and lock the account to a particular netid.
            eperson.setNetid(netid);
        }
        if (canUpdateField("email", email, eperson.getEmail())) {
            // The email could have changed if using netid based lookup.
            eperson.setEmail(email.toLowerCase());
        }
        else {
            logIgnoredUpdate("email", email, eperson.getEmail());
        }
        if (canUpdateField("first_name", fname, eperson.getFirstName())) {
            eperson.setFirstName(fname);
        }
        else {
            logIgnoredUpdate("first_name", fname, eperson.getFirstName());
        }
        if (canUpdateField("last_name", lname, eperson.getLastName())) {
            eperson.setLastName(lname);
        }
        else {
            logIgnoredUpdate("last_name",lname, eperson.getLastName());
        }
    }

    private static void update_eperson(EPerson eperson, ShibHeaders shib_headers)
    {
        for (String header : metadataHeaderMap.keySet())
        {

            String field = metadataHeaderMap.get(header);
            String value = shib_headers.get_single(header);

            // Truncate values
            if (value == null) {
                log.warn("Unable to update the eperson's '"+field+"' metadata because " +
                		"the header '"+header+"' does not exist.");
                continue;
            } else if ("phone".equals(field) && value.length() > PHONE_MAX_SIZE) {
                value = truncate_names("phone", value, PHONE_MAX_SIZE);
            } else if (value.length() > EPerson.METADATA_MAX_SIZE) {
                value = truncate_names(field, value, EPerson.METADATA_MAX_SIZE);
            }

            // update only if it is not filled out
            String existing_meta = eperson.getMetadata(field);
            if (canUpdateField(field, value, existing_meta)) {
            	eperson.setMetadata(field, value);
            	log.debug("Updated the eperson's '"+field+"' metadata using header: '"+header+"' = '"+value+"'.");
            }else {
                logIgnoredUpdate(field, value, existing_meta);
            }
        }
    }


    /**
     * Provide password-based authentication to enable sword compatability.
     *
     * Sword compatability will allow this authentication method to work when using
     * sword. Sort relies on username and password based authentication and is
     * entirely incapable of supporting shibboleth. This option allows you to
     * authenticate username and passwords for sword sessions with out adding
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
    protected int swordCompatability(Context context, String username, String password, HttpServletRequest request) throws SQLException {

        EPerson eperson = null;

        log.debug("Shibboleth Sword compatability activated.");
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
            log.info(eperson.getEmail()+" has been authenticated via shibboleth using password-based sword compatability mode.");
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
     * if it suports the metadata field. If the field is not supported and autocreate is turned on then
     * the field will be automatically created.
     *
     * It is safe to call this methods multiple times.
     */
    private synchronized static void initialize() throws SQLException {

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
                log.error("Unable to parse metadata mapping string: '"+metadataString+"'");
                continue;
            }

            String header = metadataParts[0].trim();
            String name = metadataParts[1].trim().toLowerCase();

            boolean valid = EPerson.checkIfMetadataFieldExists(name);

            if ( ! valid && autoCreate) {
                valid = EPerson.autoCreateMetadataField(name);
            }

            if (valid) {
                // The eperson field is fine, we can use it.
                log.debug("Loading additional eperson metadata mapping for: '"+header+"' = '"+name+"'");
                map.put(header, name);
            } else {
                // The field dosn't exist, and we can't use it.
                log.error("Skipping the additional eperson metadata mapping for: '"+header+"' = '"+name+"' because the field is not supported by the current configuration.");
            }
        } // foreach metadataStringList


        metadataHeaderMap = map;
        return;
    }


    /*
     * <UFAL>
     */
    private void logAndSetMessage(
          Context context,
          HttpServletRequest request,
          String organization,
          String detailed_message)
    {
        cz.cuni.mff.ufal.Headers headers = new cz.cuni.mff.ufal.Headers(request, "," );
        log.error( String.format(
                "Could not identify a user from [%s] - we have not received enough information (email, netid, eppn, ...)." +
                "\n\nDetails:\n%s\n\nHeaders received:\n%s",
                organization, detailed_message, headers.toString()) );
        context.cache(headers,1);
    }
    /* </UFAL> */

    private static String truncate_names(String field_name, String input, int max_len)
    {
        if (input != null && input.length() > max_len) {
            log.warn("Truncating eperson's " + field_name + " because it is longer " +
            		"than " + max_len + ": '" + input + "'");
            input = input.substring(0, max_len);
        }
		return input;
    }

}

