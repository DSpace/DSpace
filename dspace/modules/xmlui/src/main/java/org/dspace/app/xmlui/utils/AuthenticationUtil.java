/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.SystemwideAlerts;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;

/**
 * Methods for authenticating the user. This is DSpace platform code, as opposed
 * to the site-specific authentication code, that resides in implementations of
 * the org.dspace.eperson.AuthenticationMethod interface.
 * 
 * @author Scott Phillips
 * @author Robert Tansley
 */

public class AuthenticationUtil
{
    private static final Logger log = Logger.getLogger(AuthenticationUtil.class);

    /**
     * Session attribute name for storing the return URL where the user should
     * be redirected too once successfully authenticated.
     */
    public static final String REQUEST_INTERRUPTED = "dspace.request.interrupted";
    public static final String REQUEST_RESUME = "dspace.request.resume";
    
    /**
     * These store a message giving a reason for why the request is being interrupted.
     */
    public static final String REQUEST_INTERRUPTED_HEADER = "dspace.request.interrupted.header";
    public static final String REQUEST_INTERRUPTED_MESSAGE = "dspace.request.interrupted.message";
    public static final String REQUEST_INTERRUPTED_CHARACTERS = "dspace.request.interrupted.characters";

    
    /**
     * The IP address this user first logged in from, do not allow this session for
     * other IP addresses.
     */
    private static final String CURRENT_IP_ADDRESS = "dspace.user.ip";
    
    /**
     * The effective user id, typically this will never change. However, if an administrator 
     * has assumed login as this user then they will differ.
     */
    private static final String EFFECTIVE_USER_ID = "dspace.user.effective";
    private static final String AUTHENTICATED_USER_ID = "dspace.user.authenticated";

    protected static final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected static final AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    protected static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected static final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    /**
     * Authenticate the current DSpace content based upon given authentication
     * credentials. The AuthenticationManager will consult the configured
     * authentication stack to determine the best method.
     * 
     * @param objectModel
     *            Cocoon's object model.
     * @param email
     *            The email credentials provided by the user.
     * @param password
     *            The password credentials provided by the user.
     * @param realm
     *            The realm credentials provided by the user.
     * @return Return a current context with either the eperson attached if the
     *         authentication was successful or or no eperson attached if the
     *         attempt failed.
     * @throws java.sql.SQLException passed through.
     */
    public static Context authenticate(Map objectModel, String email, String password, String realm)
    throws SQLException
    {
        // Get the real HttpRequest
        HttpServletRequest request = (HttpServletRequest) objectModel
                .get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        int implicitStatus = authenticationService.authenticateImplicit(
                context, null, null, null, request);

        if (implicitStatus == AuthenticationMethod.SUCCESS)
        {
            log.info(LogManager.getHeader(context, "login", "type=implicit"));
            AuthenticationUtil.logIn(context, request, context.getCurrentUser());
        }
        else
        {
            // If implicit authentication failed, fall over to explicit.

            int explicitStatus = authenticationService.authenticate(context,
                    email, password, realm, request);

            if (explicitStatus == AuthenticationMethod.SUCCESS)
            {
                // Logged in OK.
                log.info(LogManager
                        .getHeader(context, "login", "type=explicit"));
                AuthenticationUtil.logIn(context, request, context
                        .getCurrentUser());
            }
            else
            {
                log.info(LogManager.getHeader(context, "failed_login", "email="
                        + email + ", realm=" + realm + ", result="
                        + explicitStatus));
            }
        }

        return context;
    }

    /**
     * Perform implicit authentication. The authenticationManager will consult
     * the authentication stack for any methods that can implicitly authenticate
     * this session. If the attempt was successful then the returned context
     * will have an eperson attached other wise the context will not have an
     * eperson attached.
     * 
     * @param objectModel
     *            Cocoon's object model.
     * @return This requests DSpace context.
     * @throws java.sql.SQLException passed through.
     */
    public static Context authenticateImplicit(Map objectModel)
            throws SQLException
    {
        // Get the real HttpRequest
        final HttpServletRequest request = (HttpServletRequest) objectModel
                .get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        int implicitStatus = authenticationService.authenticateImplicit(
                context, null, null, null, request);

        if (implicitStatus == AuthenticationMethod.SUCCESS)
        {
            log.info(LogManager.getHeader(context, "login", "type=implicit"));
            AuthenticationUtil.logIn(context, request, context.getCurrentUser());
        }

        return context;
    }

    /**
     * Log the given user in as a real authenticated user. This should only be used after 
     * a user has presented credentials and they have been validated. 
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param eperson
     *            the eperson logged in
     */
    private static void logIn(Context context, HttpServletRequest request,
            EPerson eperson) throws SQLException
    {
        if (eperson == null)
        {
            return;
        }
        
        HttpSession session = request.getSession();

        context.setCurrentUser(eperson);

        // Check to see if systemwide alerts is restricting sessions
        if (!authorizeService.isAdmin(context) && !SystemwideAlerts.canUserStartSession())
        {
        	// Do not allow this user to login because sessions are being restricted by a systemwide alert.
        	context.setCurrentUser(null);
        	return;
        }
        
        // Set any special groups - invoke the authentication manager.
        List<Group> groups = authenticationService.getSpecialGroups(context,
                request);
        for (Group group : groups)
        {
            context.setSpecialGroup(group.getID());
        }

        // and the remote IP address to compare against later requests
        // so we can detect session hijacking.
        session.setAttribute(CURRENT_IP_ADDRESS, request.getRemoteAddr());
        
        // Set both the effective and authenticated user to the same.
        session.setAttribute(EFFECTIVE_USER_ID, eperson.getID());
        session.setAttribute(AUTHENTICATED_USER_ID,eperson.getID());
        
    }
    
    /**
     * Log the given user in as a real authenticated user. This should only be used after 
     * a user has presented credentials and they have been validated. This method 
     * signature is provided to be easier to call from flow scripts.
     * 
     * @param objectModel 
     * 			  The cocoon object model.
     * @param eperson
     *            the eperson logged in
     * @throws java.sql.SQLException passed through.
     */
    public static void logIn(Map objectModel, EPerson eperson) throws SQLException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);
        
        logIn(context,request,eperson);
    }

    /**
     * Check to see if there are any session attributes indicating a currently authenticated 
     * user. If there is then log this user in.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP Request
     * @throws java.sql.SQLException passed through.
     */
    public static void resumeLogin(Context context, HttpServletRequest request)
            throws SQLException
    {
        HttpSession session = request.getSession(false);

        if (session != null)
        {
            UUID id = (UUID) session.getAttribute(EFFECTIVE_USER_ID);
            UUID realid = (UUID) session.getAttribute(AUTHENTICATED_USER_ID);
            
            if (id != null)
            {
                // Should we check for an ip match from the start of the request to now?
                boolean ipcheck = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.session.ipcheck", true);

                String address = (String)session.getAttribute(CURRENT_IP_ADDRESS);
                if (!ipcheck || (address != null && address.equals(request.getRemoteAddr())))
                {
                    EPerson eperson = ePersonService.find(context, id);
                    context.setCurrentUser(eperson);
                    
                    // Check to see if systemwide alerts is restricting sessions
                    if (!authorizeService.isAdmin(context) && !SystemwideAlerts.canUserMaintainSession())
                    {
                    	// Normal users can not maintain their sessions, check to see if this is really an
                    	// administrator logging in as someone else.
                    	
                    	EPerson realEPerson = ePersonService.find(context, realid);
                    	Group administrators = groupService.findByName(context, Group.ADMIN);
                 	    if (!groupService.isDirectMember(administrators, realEPerson))
                 	    {
                 	    	// Log this user out because sessions are being restricted by a systemwide alert.
                 	    	context.setCurrentUser(null);
                 	    	return;
                 	    }
                    }
                    

                    // Set any special groups - invoke the authentication mgr.
                    List<Group> groups = authenticationService.getSpecialGroups(context, request);
                    for (Group group : groups)
                    {
                        context.setSpecialGroup(group.getID());
                    }
                }
                else
                {
                    // Possible hack attempt or maybe your setup is not providing a consistent end-user IP address.
                    log.warn(LogManager.getHeader(context, "ip_mismatch", "id=" + id + ", request ip=" +
                        request.getRemoteAddr() + ", session ip=" + address));
                }
            } // if id
        } // if session
    }

    /**
     * Assume the login as another user. Only site administrators may perform the action.
     * 
     * @param context
     * 		The current DSpace context logged in as a site administrator
     * @param request
     * 		The real HTTP request.
     * @param loginAs
     * 		Whom to login as.
     * @throws SQLException passed through.
     * @throws AuthorizeException using an I18nTransformer key as the message
     */
    public static void loginAs(Context context, HttpServletRequest request, EPerson loginAs ) 
    throws SQLException, AuthorizeException
    {
    	// Only allow loginAs if the administrator has allowed it.
    	if (!DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.user.assumelogin", false))
        {
            return;
        }
    	
    	// Only super administrators can login as someone else.
    	if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException("xmlui.utils.AuthenticationUtil.onlyAdmins");
        }
    		
    	// Just to be double be sure, make sure the administrator
    	// is the one who actually authenticated himself.
	    HttpSession session = request.getSession(false);
        UUID authenticatedID = (UUID) session.getAttribute(AUTHENTICATED_USER_ID);
	    if (!context.getCurrentUser().getID().equals(authenticatedID))
        {
            throw new AuthorizeException("xmlui.utils.AuthenticationUtil.onlyAuthenticatedAdmins");
        }
	    
	    // You may not assume the login of another super administrator
	    if (loginAs == null)
        {
            return;
        }
	    Group administrators = groupService.findByName(context, Group.ADMIN);
	    if (groupService.isDirectMember(administrators, loginAs))
        {
            throw new AuthorizeException("xmlui.utils.AuthenticationUtil.notAnotherAdmin");
        }
	    
	    // Success, allow the user to login as another user.
	    context.setCurrentUser(loginAs);
	
        // Set any special groups - invoke the authentication mgr.
        List<Group> groups = authenticationService.getSpecialGroups(context, request);
        for (Group group : groups)
        {
            context.setSpecialGroup(group.getID());
        }
	    	        
        // Set both the effective and authenticated user to the same.
        session.setAttribute(EFFECTIVE_USER_ID, loginAs.getID());
    }
    
    
    /**
     * Log the user out.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @throws java.sql.SQLException passed through.
     */
    public static void logOut(Context context, HttpServletRequest request) throws SQLException
    {
        HttpSession session = request.getSession();

        if (session.getAttribute(EFFECTIVE_USER_ID) != null &&
        	session.getAttribute(AUTHENTICATED_USER_ID) != null)
        {
    	    UUID effectiveID = (UUID) session.getAttribute(EFFECTIVE_USER_ID);
    	    UUID authenticatedID = (UUID) session.getAttribute(AUTHENTICATED_USER_ID);
    	    
    	    if (!effectiveID.equals(authenticatedID))
    	    {
    	    	// The user has login in as another user, instead of logging them out, 
    	    	// revert back to their previous login name.
    	    	
    	    	EPerson authenticatedUser = ePersonService.find(context, authenticatedID);
    	    	context.setCurrentUser(authenticatedUser);
    	    	session.setAttribute(EFFECTIVE_USER_ID, authenticatedID);
    	    	return;
    	    }
        }
        
        // Otherwise, just log the person out as normal.
        context.setCurrentUser(null);
        session.removeAttribute(EFFECTIVE_USER_ID);
        session.removeAttribute(AUTHENTICATED_USER_ID);
        session.removeAttribute(CURRENT_IP_ADDRESS);
    }

    /**
     * Determine if the email can register itself or needs to be
     * created by a site administrator first.
     * 
     * @param objectModel
     *          The Cocoon object model
     * @param email
     *          The email of the person to be registered.
     * @return true if the email can register, otherwise false.
     * @throws java.sql.SQLException passed through.
     */
    public static boolean canSelfRegister(Map objectModel, String email) throws SQLException 
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);
        
        if (SystemwideAlerts.canUserStartSession())
        {
            return authenticationService.canSelfRegister(context, request, email);
        }
        else
        {
            // System wide alerts is preventing new sessions.
            return false;
        }
    }
    
    /**
     * Determine if the EPerson (to be created or already created) has the
     * ability to set their own password.
     * 
     * @param objectModel
     *              The Cocoon object model
     * @param email
     *              The email address of the EPerson.
     * @return true if allowed.
     * @throws java.sql.SQLException passed through.
     */
    public static boolean allowSetPassword(Map objectModel, String email)
	throws SQLException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);
        
        return authenticationService.allowSetPassword(context, request, email);
    }
    
    /**
     * Construct a new, mostly blank, eperson for the given email address.
     * This should only be called once the email address has been verified.
     * 
     * @param objectModel 
     *              The Cocoon object model.
     * @param email
     *              The email address of the new eperson.
     * @return A newly created EPerson object.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    public static EPerson createNewEperson(Map objectModel, String email) throws 
        SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);        
        Context context = ContextUtil.obtainContext(objectModel);
        
        // Need to create new eperson
        // FIXME: TEMPORARILY need to turn off authentication, as usually
        // only site admins can create e-people
        context.turnOffAuthorisationSystem();
        EPerson eperson = ePersonService.create(context);
        eperson.setEmail(email);
        eperson.setCanLogIn(true);
        eperson.setSelfRegistered(true);
        ePersonService.update(context, eperson);
        context.restoreAuthSystemState();
        
        // Give site auth a chance to set/override appropriate fields
        authenticationService.initEPerson(context, request, eperson);
        
        return eperson;   
    }

    /**
     * Is there a currently interrupted request?
     * 
     * @param objectModel The Cocoon object Model
     * @return true if there is an interrupted or un-resumed request.
     */
    public static boolean isInterupptedRequest(Map objectModel) 
    {
    	final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);        

    	HttpSession session = request.getSession();
    	
    	Object interruptedObject = session.getAttribute(REQUEST_INTERRUPTED);
    	
    	if (interruptedObject instanceof RequestInfo)
    	{
    		// There is currently either an interrupted or yet-to-be resumed request.
    		return true;
    	}
    		
    	// There are not interrupted requests.
    	return false;
    }

    /**
     * Interrupt the current request and store if for later resumption. This
     * request will send an HTTP redirect telling the client to authenticate
     * first. Once that has been finished then the request can be resumed.
     * 
     * @param objectModel The Cocoon object Model
     * @param header A message header (i18n tag)
     * @param message A message for why the request was interrupted (i18n tag)
     * @param characters An untranslated message, perhaps an error message?
     */
    public static void interruptRequest(Map objectModel, String header, String message, String characters)
    {
    	final HttpServletRequest request = 
    		(HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);        

    	HttpSession session = request.getSession();
        
        // Store this interrupted request until after the user successfully authenticates.
        RequestInfo interruptedRequest = new RequestInfo(request);
        
        // Set the request as interrupted
        session.setAttribute(REQUEST_INTERRUPTED,interruptedRequest);
        session.setAttribute(REQUEST_RESUME, null); // just to be clear.
        
        // Set the interrupt message
        session.setAttribute(REQUEST_INTERRUPTED_HEADER, header);
        session.setAttribute(REQUEST_INTERRUPTED_MESSAGE, message);
        session.setAttribute(REQUEST_INTERRUPTED_CHARACTERS, characters);
        
        
    }
    
    
    /**
     * Set the interrupted request to a resumable state. The
     * next request that the server receives (for this session) that
     * has the same servletPath will be replaced with the previously
     * interrupted request.
     * 
     * @param objectModel The Cocoon object Model
     * @return null.
     */
    public static String resumeInterruptedRequest(Map objectModel)
    {
    	final HttpServletRequest request = 
    		(HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);        

    	HttpSession session = request.getSession();
        
    	// Clear the interrupt message
	    session.setAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER, null);
	    session.setAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE, null);
	    session.setAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS, null);
    	
        // Set the request as interrupted
        Object interruptedObject = session.getAttribute(REQUEST_INTERRUPTED);
        if (interruptedObject instanceof RequestInfo)
        {
        	RequestInfo interruptedRequest = (RequestInfo) interruptedObject;
        	
        	session.setAttribute(REQUEST_INTERRUPTED, null);
        	session.setAttribute(REQUEST_RESUME, interruptedRequest); 
        	
        	// Return the path for which this request belongs too. Only URLs
        	// for this path may be resumed.
        	if (interruptedRequest.getServletPath() == null || interruptedRequest.getServletPath().length() == 0) {
                return interruptedRequest.getActualPath();
            } else {
        	    return interruptedRequest.getServletPath();
            }
        }
        
        // No request was interrupted.
        return null;
    }
    
    
    
    /**
     * Check to see if this request should be resumed.
     * 
     * @param realHttpRequest The current real request
     * @return Either the current real request or a stored request that was previously interrupted.
     */
    public static HttpServletRequest resumeRequest(HttpServletRequest realHttpRequest) 
    {
    	// First check to see if there is a resumed request.
    	HttpSession session = realHttpRequest.getSession();
    	//session.setMaxInactiveInterval(60);
        Object object = session.getAttribute(REQUEST_RESUME);
    	
        // Next check to make sure it's the right type of object, 
        // there should be no condition where it is not - but always 
        // safe to check.
        if (object instanceof RequestInfo)
        {
        	RequestInfo interruptedRequest = (RequestInfo) object;
        
        	// Next, check to make sure this real request if for the same URL
        	// path, if so then resume the previous request.
        	String interruptedServletPath = interruptedRequest.getServletPath();
        	String realServletPath = realHttpRequest.getServletPath();
        	
        	if (realServletPath != null && 
        		realServletPath.equals(interruptedServletPath))
        	{
        		// Clear the resumed request and send the request back to be resumed.
        		session.setAttribute(REQUEST_INTERRUPTED, null);
        		session.setAttribute(REQUEST_RESUME, null);
 
        		return interruptedRequest.wrapRequest(realHttpRequest);
        	}
        }
    	// Otherwise return the real request.
    	return realHttpRequest;
    }

    /**
     * Has this user authenticated?
     * @param request the user's Request.
     * @return true if request is in a session having a user ID.
     */
    public static boolean isLoggedIn(HttpServletRequest request)
    {
        return (null != request.getSession().getAttribute(EFFECTIVE_USER_ID));
    }
}
