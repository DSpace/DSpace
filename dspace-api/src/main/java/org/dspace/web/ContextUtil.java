/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.web;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;

/**
 * Miscellaneous UI utility methods methods for managing DSpace context.
 *
 * This class was "adapted" from the class of the same name in old XMLUI.
 *
 * @author Tim Donohue
 */
public class ContextUtil {
    /**
     * The log4j logger
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ContextUtil.class);

    /**
     * Where the context is stored on an HTTP Request object
     */
    public static final String DSPACE_CONTEXT = "dspace.context";

    /**
     * Default constructor
     */
    private ContextUtil() { }

    /**
     * Inspection method to check if a DSpace context has been created for this request.
     *
     * @param request the servlet request object
     * @return True if a context has previously been created, false otherwise.
     */
    public static boolean isContextAvailable(ServletRequest request) {
        Object object = request.getAttribute(DSPACE_CONTEXT);

        if (object instanceof Context) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Obtain a new context object. If a context object has already been created
     * for this HTTP request, it is re-used, otherwise it is created.
     *
     * @param request the servlet request object
     * @return a context object
     */
    public static Context obtainContext(HttpServletRequest request) {
        Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

        if (context == null) {
            try {
                context = ContextUtil.initializeContext();
            } catch (SQLException e) {
                log.error("Unable to initialize context", e);
                return null;
            }

            // Store the context in the request
            request.setAttribute(DSPACE_CONTEXT, context);
        }
        // this need to be verified each time that the context is extracted from the request
        // as some call happen before that the login process is completed and user settings can
        // change the locale
        Locale currentLocale = getLocale(context, request);
        context.setCurrentLocale(currentLocale);
        return context;
    }

    /**
     * Shortcut for {@link #obtainContext(Request)} using the {@link RequestService}
     * to retrieve the current thread request
     *
     * @return the DSpace Context associated with the current thread-bound request
     */
    public static Context obtainCurrentRequestContext() {
        Context context = null;
        RequestService requestService = new DSpace().getRequestService();
        Request currentRequest = requestService.getCurrentRequest();
        if (currentRequest != null) {
            context = ContextUtil.obtainContext(currentRequest.getHttpServletRequest());
        }
        return context;
    }

    private static Locale getLocale(Context context, HttpServletRequest request) {
        Locale userLocale = null;
        Locale supportedLocale = null;

        // Locales requested from client
        String locale = request.getHeader("Accept-Language");
        if (StringUtils.isNotBlank(locale)) {
            Enumeration<Locale> locales = request.getLocales();
            if (locales != null) {
                while (locales.hasMoreElements()) {
                    Locale current = locales.nextElement();
                    if (I18nUtil.isSupportedLocale(current)) {
                        userLocale = current;
                        break;
                    }
                }
            }
        }
        if (userLocale == null && context.getCurrentUser() != null) {
            String userLanguage = context.getCurrentUser().getLanguage();
            if (userLanguage != null) {
                userLocale = new Locale(userLanguage);
            }
        }
        if (userLocale == null) {
            return I18nUtil.getDefaultLocale();
        }
        supportedLocale = I18nUtil.getSupportedLocale(userLocale);
        return supportedLocale;
    }

    /**
     * Initialize a new Context object
     *
     * @return a DSpace Context Object
     * @throws SQLException
     */
    private static Context initializeContext() throws SQLException {
        // Create a new Context
        Context context = new Context();
        // Set the session ID
        /**context.setExtraLogInfo("session_id="
         + request.getSession().getId());

         AuthenticationUtil.resumeLogin(context, request);

         // Set any special groups - invoke the authentication mgr.
         int[] groupIDs = AuthenticationManager.getSpecialGroups(context, request);

         for (int i = 0; i < groupIDs.length; i++)
         {
         context.setSpecialGroup(groupIDs[i]);
         log.debug("Adding Special Group id="+String.valueOf(groupIDs[i]));
         }

         // Set the session ID and IP address
         String ip = request.getRemoteAddr();
         if (useProxies == null) {
         useProxies = ConfigurationManager.getBooleanProperty("useProxies", false);
         }
         if(useProxies && request.getHeader("X-Forwarded-For") != null)
         {
         // This header is a comma delimited list
         for(String xfip : request.getHeader("X-Forwarded-For").split(","))
         {
         if(!request.getHeader("X-Forwarded-For").contains(ip))
         {
         ip = xfip.trim();
         }
         }
         }
         context.setExtraLogInfo("session_id=" + request.getSession().getId() + ":ip_addr=" + ip);
         */

        return context;
    }

    /**
     * Check if a context exists for this request, if so complete the context.
     *
     * @param request The request object
     */
    public static void completeContext(ServletRequest request) throws ServletException {
        Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

        if (context != null && context.isValid()) {
            try {
                context.complete();
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
    }

    public static void abortContext(ServletRequest request) {
        Context context = (Context) request.getAttribute(DSPACE_CONTEXT);

        if (context != null && context.isValid()) {
            context.abort();
        }
    }
}
