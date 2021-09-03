/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 * Methods for authenticating the user. This is DSpace platform code, as opposed
 * to the site-specific authentication code, that resides in implementations of
 * the <code>org.dspace.eperson.AuthenticationMethod</code> interface.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Authenticate
{
    /** log4j category */
    private static Logger log = Logger.getLogger(Authenticate.class);
    
    private static boolean initialized = false;
    
    private static AuthenticationService authenticationService;
    
    private static AuthorizeService authorizeService;
    
    private static EPersonService personService;

    private static synchronized void initialize() {
    	if (initialized) {
    		return;
    	}
    	authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    	authorizeService =  AuthorizeServiceFactory.getInstance().getAuthorizeService();
    	personService = EPersonServiceFactory.getInstance().getEPersonService();
    }
    
    /**
     * Return the request that the system should be dealing with, given the
     * request that the browse just sent. If the incoming request is from a
     * redirect resulting from successful authentication, a request object
     * corresponding to the original request that prompted authentication is
     * returned. Otherwise, the request passed in is returned.
     * 
     * @param request
     *            the incoming HTTP request
     * @return the HTTP request the DSpace system should deal with
     */
    public static HttpServletRequest getRealRequest(HttpServletRequest request)
    {
    	initialize();
    	
        HttpSession session = request.getSession();

        if (session.getAttribute("resuming.request") != null)
        {
            // Get info about the interrupted request
            RequestInfo requestInfo = (RequestInfo) session
                    .getAttribute("interrupted.request.info");

            HttpServletRequest actualRequest;

            if (requestInfo == null)
            {
                // Can't find the wrapped request information.
                // FIXME: Proceed with current request - correct?
                actualRequest = request;
            }
            else
            {
                /*
                 * Wrap the current request to make it look like the interruped
                 * one
                 */
                actualRequest = requestInfo.wrapRequest(request);
            }

            // Remove the info from the session so it isn't resumed twice
            session.removeAttribute("resuming.request");
            session.removeAttribute("interrupted.request.info");
            session.removeAttribute("interrupted.request.url");

            // Return the wrapped request
            return actualRequest;
        }
        else
        {
            return request;
        }
    }

    /**
     * Resume a previously interrupted request. This is invoked when a user has
     * been successfully authenticated. The request which led to authentication
     * will be resumed.
     * 
     * @param request
     *            <em>current</em> HTTP request
     * @param response
     *            HTTP response
     */
    public static void resumeInterruptedRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException
    {
    	initialize();
    	
        HttpSession session = request.getSession();
        String originalURL = (String) session
                .getAttribute("interrupted.request.url");

        if (originalURL == null)
        {
            // If for some reason we don't have the original URL, redirect
            // to My DSpace
            originalURL = request.getContextPath() + "/mydspace";
        }
        else
        {
            // Set the flag in the session, so that when the redirect is
            // followed, we'll know to resume the interrupted request
            session.setAttribute("resuming.request", Boolean.TRUE);
        }

        // Send the redirect
        response.sendRedirect(response.encodeRedirectURL(originalURL));
    }

    /**
     * Start the authentication process. This packages up the request that led
     * to authentication being required, and then invokes the site-specific
     * authentication method.
     * 
     * If it returns true, the user was authenticated without any
     * redirection (e.g. by an X.509 certificate or other implicit method) so
     * the process that called this can continue and send its own response.
     * A "false" result means this method has sent its own redirect.
     *
     * @param context
     *            current DSpace context
     * @param request
     *            current HTTP request - the one that prompted authentication
     * @param response
     *            current HTTP response
     *
     * @return true if authentication is already finished (implicit method)
     */
    public static boolean startAuthentication(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
    	initialize();
    	
        HttpSession session = request.getSession();

        /*
         * Authenticate:
         * 1. try implicit methods first, since that may work without
         *    a redirect. return true if no redirect needed.
         * 2. if those fail, redirect to enter credentials.
         *    return false.
         */
        if (authenticationService.authenticateImplicit(context, null, null,
                null, request) == AuthenticationMethod.SUCCESS)
        {
            loggedIn(context, request, context.getCurrentUser());
            log.info(LogManager.getHeader(context, "login", "type=implicit"));
            if(context.getCurrentUser() != null){
                //We have a new user
                Authenticate.resumeInterruptedRequest(request, response);
                return false;
            }else{
                //Couldn't log & authentication finished
                return true;
            }

        }
        else
        {
        // Since we may be doing a redirect, make sure the redirect is not
        // cached
        response.addDateHeader("expires", 1);
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-control", "no-store");

        // Store the data from the request that led to authentication
        RequestInfo info = new RequestInfo(request);
        session.setAttribute("interrupted.request.info", info);

        // Store the URL of the request that led to authentication
        session.setAttribute("interrupted.request.url", UIUtil
                .getOriginalURL(request));

            /*
             * Grovel over authentication methods, counting the
             * ones with a "redirect" login page -- if there's only one,
             * go directly there.  If there is a choice, go to JSP chooser.
             */
            Iterator ai = authenticationService.authenticationMethodIterator();
            AuthenticationMethod am;
            int count = 0;
            String url = null;
            while (ai.hasNext())
            {
                String s;
                am = (AuthenticationMethod)ai.next();
                s = am.loginPageURL(context, request, response);
                if (s != null)
                {
                    url = s;
                    ++count;
                }
            }
            if (count == 1)
            {
                response.sendRedirect(url);
            }
            else
            {
                JSPManager.showJSP(request, response, "/login/chooser.jsp");
            }
        }
        return false;
    }

    /**
     * Store information about the current user in the request and context
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param eperson
     *            the eperson logged in
     */
    public static void loggedIn(Context context,
                                HttpServletRequest request,
                                EPerson eperson)
    {
    	initialize();
    	
        HttpSession session = request.getSession();

        // For security reasons after login, give the user a new session
        if ((!session.isNew()) && (session.getAttribute("dspace.current.user.id") == null))
        {
            // Keep the user's locale setting if set
            Locale sessionLocale = UIUtil.getSessionLocale(request);

            // Get info about the interrupted request, if set
            RequestInfo requestInfo = (RequestInfo) session.getAttribute("interrupted.request.info");

            // Get the original URL of interrupted request, if set
            String requestUrl = (String) session.getAttribute("interrupted.request.url");
            
            // Shibboleth stores information about special groups in the session. Preserve these information.
	    Boolean shibbolethAuthenticated = (Boolean) session.getAttribute("shib.authenticated");
            List<UUID> shibbolethSpecialGroups = (List<UUID>) session.getAttribute("shib.specialgroup");
           
            // Invalidate session unless dspace.cfg says not to
            if(ConfigurationManager.getBooleanProperty("webui.session.invalidate", true))
            {
               session.invalidate();
            }

            // Give the user a new session
            session = request.getSession();

            // Restore the session locale
            if (sessionLocale != null)
            {
                Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
            }

            // Restore interrupted request information and url to new session
            if (requestInfo != null && requestUrl != null) {
                session.setAttribute("interrupted.request.info", requestInfo);
                session.setAttribute("interrupted.request.url", requestUrl);
            }
            
            // Restore shibboleth special groups
	    if (shibbolethAuthenticated != null) {
		    session.setAttribute("shib.authenticated", shibbolethAuthenticated.booleanValue());
	    }
            if (shibbolethSpecialGroups != null) {
                session.setAttribute("shib.specialgroup", shibbolethSpecialGroups);
            }
        }

        context.setCurrentUser(eperson);
        
        boolean isAdmin = false;
        boolean isCommunityAdmin = false;
        boolean isCollectionAdmin = false;
        
        try
        {
            isAdmin = authorizeService.isAdmin(context);
            isCommunityAdmin = authorizeService.isCommunityAdmin(context);
            isCollectionAdmin = authorizeService.isCollectionAdmin(context);
        }
        catch (SQLException se)
        {
            log.warn("Unable to use AuthorizeManager " + se);
        }
        finally 
        {
            request.setAttribute("is.admin", Boolean.valueOf(isAdmin));
            request.setAttribute("is.communityAdmin", Boolean.valueOf(isCommunityAdmin));
            request.setAttribute("is.collectionAdmin", Boolean.valueOf(isCollectionAdmin));
        }

        // We store the current user in the request as an EPerson object...
        request.setAttribute("dspace.current.user", eperson);

        // and in the session as an ID
        session.setAttribute("dspace.current.user.id", eperson.getID());

        // and the remote IP address to compare against later requests
        // so we can detect session hijacking.
        session.setAttribute("dspace.current.remote.addr",
                             request.getRemoteAddr());

    }

    /**
     * Log the user out
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @throws SQLException 
     */
    public static void loggedOut(Context context, HttpServletRequest request) throws SQLException
    {
    	initialize();
    	
        HttpSession session = request.getSession();

        context.setCurrentUser(null);
        request.removeAttribute("is.admin");
        request.removeAttribute("dspace.current.user");
        session.removeAttribute("dspace.current.user.id");

        UUID previousUserID = (UUID) session.getAttribute("dspace.previous.user.id");
        
        // Keep the user's locale setting if set
        Locale sessionLocale = UIUtil.getSessionLocale(request);

        // Invalidate session unless dspace.cfg says not to (or it is a loggedOut from a loginAs)
        if(ConfigurationManager.getBooleanProperty("webui.session.invalidate", true) 
                && previousUserID != null)
        {
            session.invalidate();
        }

        // Restore the session locale
        if (sessionLocale != null)
        {
            Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
        }
        
        if (previousUserID != null)
        {
            session.removeAttribute("dspace.previous.user.id");
            EPerson ePerson = personService.find(context, previousUserID);
            loggedIn(context, request, ePerson);
        }
    }
}
