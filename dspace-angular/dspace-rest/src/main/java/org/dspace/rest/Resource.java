/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Superclass of all resource classes in REST API. It has methods for creating
 * context, write statistics, processsing exceptions, splitting a key of
 * metadata, string representation of action and method for getting the logged
 * in user from the token in request header.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 */
public class Resource {

    @javax.ws.rs.core.Context
    public ServletContext servletContext;

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(Resource.class);

    private static final boolean writeStatistics;

    static {
        writeStatistics = DSpaceServicesFactory.getInstance().getConfigurationService()
                                               .getBooleanProperty("rest.stats", false);
    }

    /**
     * Create context to work with DSpace database. It can create context
     * with or without a logged in user (retrieved from SecurityContextHolder). Throws
     * WebApplicationException caused by: SQLException if there was a problem
     * with reading from database. Throws AuthorizeException if there was
     * a problem with authorization to read from the database. Throws Exception
     * if there was a problem creating context.
     *
     * @return Newly created context with the logged in user unless the specified user was null.
     * If user is null, create the context without a logged in user.
     * @throws ContextException Thrown in case of a problem creating context. Can be caused by
     *                          SQLException error in creating context or finding the user to
     *                          log in. Can be caused by AuthorizeException if there was a
     *                          problem authorizing the found user.
     * @throws SQLException     An exception that provides information on a database access error or other errors.
     */
    protected static org.dspace.core.Context createContext() throws ContextException, SQLException {
        org.dspace.core.Context context = new org.dspace.core.Context();
        //context.getDBConnection().setAutoCommit(false); // Disable autocommit.

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Collection<SimpleGrantedAuthority> specialGroups = (Collection<SimpleGrantedAuthority>) authentication
                .getAuthorities();
            for (SimpleGrantedAuthority grantedAuthority : specialGroups) {
                context.setSpecialGroup(EPersonServiceFactory.getInstance().getGroupService()
                                                             .findByName(context, grantedAuthority.getAuthority())
                                                             .getID());
            }
            context.setCurrentUser(
                EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, authentication.getName()));
        }

        return context;
    }

    /**
     * Records a statistics event about an object used via REST API.
     *
     * @param dspaceObject  DSpace object on which a request was performed.
     * @param action        Action that was performed.
     * @param user_ip       User's IP address.
     * @param user_agent    User agent string (specifies browser used and its version).
     * @param xforwardedfor When accessed via a reverse proxy, the application sees the proxy's IP as the
     *                      source of the request. The proxy may be configured to add the
     *                      "X-Forwarded-For" HTTP header containing the original IP of the client
     *                      so that the reverse-proxied application can get the client's IP.
     * @param headers       If you want to access the item as the user logged into the
     *                      context. The header "rest-dspace-token" with the token passed
     *                      from the login method must be set.
     * @param request       Servlet's HTTP request object.
     * @param context       Context which must be aborted.
     */
    protected void writeStats(DSpaceObject dspaceObject, UsageEvent.Action action,
                              String user_ip, String user_agent, String xforwardedfor, HttpHeaders headers,
                              HttpServletRequest request, Context context) {
        if (!writeStatistics) {
            return;
        }

        if ((user_ip == null) || (user_ip.length() == 0)) {
            DSpaceServicesFactory.getInstance().getEventService()
                                 .fireEvent(new UsageEvent(action, request, context, dspaceObject));
        } else {
            DSpaceServicesFactory.getInstance().getEventService().fireEvent(
                new UsageEvent(action, user_ip, user_agent, xforwardedfor, context, dspaceObject));
        }

        log.debug("fired event");
    }

    /**
     * Process exception, print message to logger error stream and abort DSpace
     * context.
     *
     * @param message Message, which will be printed to error stream.
     * @param context Context which must be aborted.
     * @throws WebApplicationException This exception is throw for user of REST api.
     */
    protected static void processException(String message, org.dspace.core.Context context)
        throws WebApplicationException {
        if ((context != null) && (context.isValid())) {
            context.abort();
        }
        log.error(message);
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Process finally statement. It will print message to logger error stream
     * and abort DSpace context, if was not properly ended.
     *
     * @param context Context which must be aborted.
     * @throws WebApplicationException This exception is thrown for user of REST API.
     */
    protected void processFinally(org.dspace.core.Context context) throws WebApplicationException {
        if ((context != null) && (context.isValid())) {
            context.abort();
            log.error("Something get wrong. Aborting context in finally statement.");
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Split string with regex ".".
     *
     * @param key String which will be splitted.
     * @return String array filed with separated string.
     */
    protected String[] mySplit(String key) {
        ArrayList<String> list = new ArrayList<String>();
        int prev = 0;
        for (int i = 0; i < key.length(); i++) {
            if (key.charAt(i) == '.') {
                list.add(key.substring(prev, i));
                prev = i + 1;
            } else if (i + 1 == key.length()) {
                list.add(key.substring(prev, i + 1));
            }
        }

        if (list.size() == 2) {
            list.add(null);
        }

        return list.toArray(new String[0]);
    }

    /**
     * Return string representation of values
     * org.dspace.core.Constants.{READ,WRITE,DELETE}.
     *
     * @param action Constant from org.dspace.core.Constants.*
     * @return String representation. read or write or delete.
     */
    protected String getActionString(int action) {
        String actionStr;
        switch (action) {
            case org.dspace.core.Constants.READ:
                actionStr = "read";
                break;
            case org.dspace.core.Constants.WRITE:
                actionStr = "write";
                break;
            case org.dspace.core.Constants.DELETE:
                actionStr = "delete";
                break;
            case org.dspace.core.Constants.REMOVE:
                actionStr = "remove";
                break;
            case org.dspace.core.Constants.ADD:
                actionStr = "add";
                break;
            default:
                actionStr = "(?action?)";
                break;
        }
        return actionStr;
    }

}
