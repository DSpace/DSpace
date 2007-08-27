/*
 * DAVServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.dav;

import java.io.IOException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.StringTokenizer;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;


/**
 * Servlet implementing WebDAV server for DSpace.
 * <P>
 * 
 * @author Larry Stone
 * @version $Revision$
 */
public class DAVServlet extends HttpServlet

{
    
    /** log4j category. */
    private static Logger log = Logger.getLogger(DAVServlet.class);

    /** Names of DAV HTTP extension methods. */
    private static final String METHOD_PROPFIND = "PROPFIND";

    /** The Constant METHOD_PROPPATCH. */
    private static final String METHOD_PROPPATCH = "PROPPATCH";

    /** The Constant METHOD_MKCOL. */
    private static final String METHOD_MKCOL = "MKCOL";

    /** The Constant METHOD_COPY. */
    private static final String METHOD_COPY = "COPY";

    /** The Constant METHOD_MOVE. */
    private static final String METHOD_MOVE = "MOVE";

    /** The Constant METHOD_LOCK. */
    private static final String METHOD_LOCK = "LOCK";

    /** The Constant METHOD_UNLOCK. */
    private static final String METHOD_UNLOCK = "UNLOCK";

    /** The Constant METHOD_DELETE. */
    private static final String METHOD_DELETE = "DELETE";

    /** Method names of standard HTTP methods; we have to override HttpServlet fields, because they are private (ugh). */
    private static final String METHOD_GET = "GET";

    /** The Constant METHOD_PUT. */
    private static final String METHOD_PUT = "PUT";

    /** The Constant METHOD_OPTIONS. */
    private static final String METHOD_OPTIONS = "OPTIONS";

    /** Switch to allow anonymous (unauthenticated) access to DAV resources. If true, client doesn't have to authenticate, false they do. */
    private static boolean allowAnonymousAccess = ConfigurationManager
            .getBooleanProperty("dav.access.anonymous");

    /** Guess at longest status text the servlet container will tolerate; Tomcat 5.0 handles this, but dies on longer messages. */
    private final static int HTTP_STATUS_MESSAGE_MAX = 1000;

    /** A random secret to embed in cookies, generated fresh at every startup:. */
    private static final String cookieSecret = Utils.generateHexKey();

    // name of our HTTP cookie.
    /** The Constant COOKIE_NAME. */
    private static final String COOKIE_NAME = "DSpaceDavAuth";

    // sell-by time (shelf life) for cookies, in milliseconds: 1/2 hour
    /** The Constant COOKIE_SELL_BY. */
    private static final long COOKIE_SELL_BY = 30 * 60 * 1000;

    // 'C' is for cookie..
    /**
     * Gimme cookie.
     * 
     * @param request the request
     * 
     * @return the cookie
     */
    private static Cookie gimmeCookie(HttpServletRequest request)
    {
        Cookie cookies[] = request.getCookies();
        if (cookies != null)
            for (Cookie element : cookies)
            {
                if (element.getName().equals(COOKIE_NAME))
                {
                    return element;
                }
            }
        return null;
    }

    /**
     * Get Session Cookie.
     * <p>
     * DAVServlet rolls its own session cookie because the Servlet container's
     * session <em>cannot</em> be constrained to use ONLY cookies and NOT
     * URL-rewriting, and the latter would break the DAV protocol so we cannot
     * use it. Since we really only need to cache the authenticated EPerson (an
     * integer ID) anyway, it's easy enough so simply stuff that into a cookie.
     * <p>
     * Cookie format is: <br>
     * {timestamp}!{epersonID}!{client-IP}!{MAC} <br>
     * where timestamp and eperson are integers; client IP is dotted IP
     * notation, and MAC is the hex MD5 of the preceding fields plus the
     * "cookieSecret" string. The MAC ensures that the cookie was issued by this
     * servlet.
     * <p>
     * Look for authentication cookie and try to get a previously-authenticated
     * EPerson from it if found. Also check the timestamp to be sure the cookie
     * isn't "stale".
     * <p>
     * NOTE This is also used by the SOAP servlet.
     * <p>
     * 
     * @param context -
     * set user in this context
     * @param request -
     * HTTP request.
     * 
     * @return true when a fresh cookie yields a valid eperson.
     * 
     * @throws SQLException the SQL exception
     */
    protected static boolean getAuthFromCookie(Context context,
            HttpServletRequest request) throws SQLException
    {
        Cookie cookie = gimmeCookie(request);
        if (cookie == null)
        {
            return false;
        }
        String crumb[] = cookie.getValue().split("\\!");
        if (crumb.length != 4)
        {
            log
                    .warn("Got invalid cookie value = \"" + cookie.getValue()
                            + "\"");
            return false;
        }
        long timestamp = 0;
        int epersonID = 0;
        try
        {
            timestamp = Long.parseLong(crumb[0]);
            epersonID = Integer.parseInt(crumb[1]);
        }
        catch (NumberFormatException e)
        {
            log.warn("Error groveling cookie, " + e.toString());
            return false;
        }

        // check freshness
        long now = new Date().getTime();
        if (timestamp > now || (now - timestamp) > COOKIE_SELL_BY)
        {
            log.warn("Cookie is stale or has weird time, value = \""
                    + cookie.getValue() + "\"");
            return false;
        }

        // check IP address
        if (!crumb[2].equals(request.getRemoteAddr()))
        {
            log.warn("Cookie fails IP Addr test, value = \""
                    + cookie.getValue() + "\"");
            return false;
        }

        // check MAC
        String mac = Utils.getMD5(crumb[0] + "!" + crumb[1] + "!" + crumb[2]
                + "!" + cookieSecret);
        if (!mac.equals(crumb[3]))
        {
            log.warn("Cookie fails MAC test, value = \"" + cookie.getValue()
                    + "\"");
            return false;
        }

        // looks like the browser reguritated a good one:
        EPerson cuser = EPerson.find(context, epersonID);
        if (cuser != null)
        {
            context.setCurrentUser(cuser);
            log.debug("Got authenticated user from cookie, id=" + crumb[1]);
            return true;
        }
        return false;
    }

    /**
     * Set a new cookie -- only bother if there is no existing cookie or it's at
     * least halfway stale, so you're not churning it.. When force is true,
     * always set a fresh cookie. (e.g. after mac failure upon server restart,
     * etc)
     * <p>
     * 
     * @param context -
     * get user from context
     * @param request the request
     * @param response the response
     * @param force the force
     */
    protected static void putAuthCookie(Context context,
            HttpServletRequest request, HttpServletResponse response,
            boolean force)
    {
        Cookie cookie = gimmeCookie(request);
        long now = new Date().getTime();
        if (!force && cookie != null)
        {
            String crumb[] = cookie.getValue().split("\\!");
            if (crumb.length == 4)
            {
                long timestamp = -1;
                try
                {
                    timestamp = Long.parseLong(crumb[0]);
                }
                catch (NumberFormatException e)
                {
                }

                // check freshness - skip setting cookie if old one isn't stale
                if (timestamp > 0 && (now - timestamp) < (COOKIE_SELL_BY / 2))
                {
                    return;
                }
            }
        }
        EPerson user = context.getCurrentUser();
        if (user == null)
        {
            return;
        }
        String value = String.valueOf(now) + "!" + String.valueOf(user.getID())
                + "!" + request.getRemoteAddr() + "!";
        String mac = Utils.getMD5(value + cookieSecret);
        cookie = new Cookie(COOKIE_NAME, value + mac);
        cookie.setPath(request.getContextPath());
        response.addCookie(cookie);

        log.debug("Setting new cookie, value = \"" + value + mac + "\"");
    }

    /**
     * Get authenticated user for this service. Returns null upon failure, with
     * the implication that an error repsonse has already been "sent", so caller
     * should not set anything else in servlet response.
     * 
     * @param request the request
     * @param response the response
     * @param username the username
     * @param password the password
     * 
     * @return the context
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    private static Context authenticate(HttpServletRequest request,
            HttpServletResponse response, String username, String password)
            throws IOException, SQLException
    {
        Context context = new Context();

        if (getAuthFromCookie(context, request))
        {
            putAuthCookie(context, request, response, false);
            return context;
        }

        // get username/password from Basic auth header if avail:
        String cred = request.getHeader("Authorization");
        if (cred != null && username == null && password == null)
        {
            log.info(LogManager.getHeader(context, "got creds", "Authorize: "
                    + cred));
            StringTokenizer ct = new StringTokenizer(cred);

            // format: Basic {username:password in base64}
            if (ct.nextToken().equalsIgnoreCase("Basic"))
            {
                String crud = ct.nextToken();
                String dcrud = new String(Base64.decodeBase64(crud.getBytes()));
                int colon = dcrud.indexOf(":");
                if (colon > 0)
                {
                    username = URLDecode(dcrud.substring(0, colon));
                    password = URLDecode(dcrud.substring(colon + 1));
                    log
                            .info(LogManager.getHeader(context, "auth",
                                    "Got username=\"" + username
                                            + "\", password=\"" + password
                                            + "\" out of \"" + crud + "\"."));
                }
            }
        }
        if (AuthenticationManager.authenticate(context, username, password,
                null, request) == AuthenticationMethod.SUCCESS)
        {
            log.info(LogManager.getHeader(context, "auth",
                    "Authentication returned SUCCESS, eperson="
                            + context.getCurrentUser().getEmail()));
        }
        else
        {
            if (username == null)
            {
                log.info(LogManager.getHeader(context, "auth",
                        "No credentials, so sending WWW-Authenticate header."));
            }
            else
            {
                log.warn(LogManager.getHeader(context, "auth",
                        "Authentication FAILED, cred=" + cred));
            }

            // ...EXCEPT if dav.access.anonymous is true in config:
            if (!allowAnonymousAccess)
            {
                if (response != null)
                {
                    response.setHeader("WWW-Authenticate",
                            "Basic realm=\"dspace\"");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
                return null;
            }
        }

        // Set any special groups - invoke the authentication mgr.
        int[] groupIDs = AuthenticationManager.getSpecialGroups(context,
                request);
        for (int element : groupIDs)
        {
            context.setSpecialGroup(element);
            log.debug("Adding Special Group id=" + String.valueOf(element));
        }
        putAuthCookie(context, request, response, true);
        return context;
    }

    /**
     * Return portion of URI path relevant to the DAV resource. We go through
     * the extra pain of chopping up getRequestURI() because it is NOT
     * URL-decoded by the Servlet container, while unfortunately getPathInfo()
     * IS pre-decoded, leaving a redudndant "/" (and who knows what else) in the
     * handle. Since the "handle" may not even be a CNRI Handle, we don't want
     * to assume it even has a "/" (escaped or not).
     * <p>
     * Finally, search for doubled-up '/' separators and coalesce them.
     * 
     * @param request the request
     * 
     * @return String of undecoded path NOT starting with '/'.
     */
    private static String getDavResourcePath(HttpServletRequest request)
    {
        String path = request.getRequestURI();
        String ppath = path.substring(request.getContextPath().length());
        String scriptName = request.getServletPath();
        if (ppath.startsWith(scriptName))
        {
            ppath = ppath.substring(scriptName.length());
        }
        // log.debug("Got DAV URI: BEFORE // FIXUP: PATH_INFO=\"" + ppath+"\"");
        // turn all double '/' ("//") in URI into single '/'
        StringBuffer sb = new StringBuffer(ppath);
        int i = ppath.length() - 2;
        if (i > 0)
        {
            while ((i = ppath.lastIndexOf("//", i)) > -1)
            {
                sb.deleteCharAt(i + 1);
                --i;
            }
        }
        // remove leading '/'
        if (sb.length() > 0 && sb.charAt(0) == '/')
        {
            sb.deleteCharAt(0);
        }
        ppath = sb.toString();
        log.debug("Got DAV URI: PATH_INFO=\"" + ppath + "\"");
        return ppath;
    }

    /**
     * override service() to add DAV methods.
     * 
     * @param request the request
     * @param response the response
     * 
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        String method = request.getMethod();

        // no authentication needed for OPTIONS
        if (method.equals(METHOD_OPTIONS))
        {
            doOptions(request, response);
        }
        else if (!serviceInternal(method, request, response))
        {
            super.service(request, response);
        }
    }

    /**
     * truncate string to max length for HTTP status message.
     * 
     * @param msg the msg
     * 
     * @return the string
     */
    private static String truncateForStatus(String msg)
    {
        return (msg.length() > HTTP_STATUS_MESSAGE_MAX) ? msg.substring(0,
                HTTP_STATUS_MESSAGE_MAX)
                + "... [Message truncated, see logs for details.]" : msg;
    }

    /**
     * Pass this request along to the appropriate resource and method. Includes
     * authentication, where needed. Return true if we handle this request,
     * false otherwise. True means response has been "sent", false not.
     * 
     * @param method the method
     * @param request the request
     * @param response the response
     * 
     * @return true, if service internal
     * 
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected static boolean serviceInternal(String method,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        // Fake new DAV methods not understood by the Apache Servlet base class
        // (returns HTTP/500 when it sees unrecognised method)
        // The way it is faked is by submitting "delete=true" in the PUT URL's
        // query parameters (for a delete)
        // The way it is faked is by submitting "mkcol=true" in the PUT URL's
        // query parameters (for a mk-collection)
        if (method.equals(METHOD_PUT)
                && request.getQueryString().indexOf("delete=true") >= 0)
        {
            method = METHOD_DELETE;
        }
        if (method.equals(METHOD_PUT)
                && request.getQueryString().indexOf("mkcol=true") >= 0)
        {
            method = METHOD_MKCOL;
        }

        // if not a DAV method (i.e. POST), defer to superclass.
        if (!(method.equals(METHOD_PROPFIND) || method.equals(METHOD_PROPPATCH)
                || method.equals(METHOD_MKCOL) || method.equals(METHOD_COPY)
                || method.equals(METHOD_MOVE) || method.equals(METHOD_DELETE)
                || method.equals(METHOD_GET) || method.equals(METHOD_PUT)))
        {
            return false;
        }

        // set all incoming encoding to UTF-8
        request.setCharacterEncoding("UTF-8");
        String pathElt[] = getDavResourcePath(request).split("/");

        Context context = null;
        try
        {
            // this sends a response on failure, unless it throws.
            context = authenticate(request, response, null, null);
            if (context == null)
            {
                return true;
            }

            // Note: findResource sends error response if it fails.
            DAVResource resource = DAVResource.findResource(context, request,
                    response, pathElt);
            if (resource != null)
            {
                if (method.equals(METHOD_PROPFIND))
                {
                    resource.propfind();
                }
                else if (method.equals(METHOD_PROPPATCH))
                {
                    resource.proppatch();
                }
                else if (method.equals(METHOD_COPY))
                {
                    resource.copy();
                }
                else if (method.equals(METHOD_DELETE))
                {
                    resource.delete();
                }
                else if (method.equals(METHOD_MKCOL))
                {
                    resource.mkcol();
                }
                else if (method.equals(METHOD_GET))
                {
                    resource.get();
                }
                else if (method.equals(METHOD_PUT))
                {
                    resource.put();
                }
                else
                {
                    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
                }
                context.complete();
                context = null;
            }
        }
        catch (SQLException e)
        {
            log.info(e.toString());
            response
                    .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            truncateForStatus("Database access error: "
                                    + e.toString()));
        }
        catch (AuthorizeException e)
        {
            log.info(e.toString());
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    truncateForStatus("Access denied: " + e.toString()));
        }
        catch (DAVStatusException se)
        {
            response.sendError(se.getStatus(), truncateForStatus(se
                    .getMessage()));
        }
        finally
        {
            // Abort the context if it's still valid
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        return true;
    }

    /**
     * Handler for HTTP OPTIONS method. Same for all resources under the WeDAV
     * root. Add DAV methods so client knows we handle DAV.
     * 
     * @param request the request
     * @param response the response
     * 
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    protected void doOptions(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        // we only support minimal DAV
        response.addHeader("DAV", "1");

        response.addHeader("Allow",
                "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, "
                        + "PROPFIND, PROPPATCH, MKCOL, COPY, MOVE");
    }

    /**
     * Sugar-coating for URLDecoder.decode, used all over.
     * 
     * @param in the in
     * 
     * @return the string
     */
    protected static String URLDecode(String in)
    {
        try
        {
            return URLDecoder.decode(in, "UTF-8");
        }
        catch (java.io.UnsupportedEncodingException e)
        {
            return "";
        }
    }

    // last servlet instance when put into service, set by init()
    /** The servlet instance. */
    private static GenericServlet servletInstance = null;

    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig sc) throws ServletException
    {
        super.init(sc);
        servletInstance = this;
    }

    /**
     * Gets the servlet instance.
     * 
     * @return the servlet instance
     */
    public static GenericServlet getServletInstance()
    {
        return servletInstance;
    }

}
