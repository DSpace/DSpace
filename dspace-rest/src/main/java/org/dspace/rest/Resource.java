package org.dspace.rest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

/**
 * Superclass of all resource class in REST api. It has methods for creating
 * context, write statistics, process exception, splitting key of metadata,
 * string representation of action and method for getting user from header.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
public class Resource
{

    private static Logger log = Logger.getLogger(Resource.class);

    private static final boolean writeStatistics;
    static
    {
        writeStatistics = ConfigurationManager.getBooleanProperty("rest", "stats", false);
    }

    /**
     * Create context to work with DSpace database. It can create context
     * without logged user (parameter user is null) or with. It can throws
     * WebApplicationException caused by: SQLException, if there was problem
     * with reading from database. AuthorizeException, if there was problem with
     * authorization to read form database. And Exception, if there was some
     * problem with creating context.
     * 
     * @param user
     *            User which will be logged in context.
     * @return New created context with logged user if user was not null.
     *         Otherwise, without logged user.
     * @throws ContextException
     *             Throw if was problem to create context. It can be caused by
     *             SQLException, error in creating context or find user to log
     *             in. Or can be caused by AuthorizeException if was problem to
     *             authorize to find user.
     */
    protected org.dspace.core.Context createContext(EPerson person) throws ContextException
    {

        org.dspace.core.Context context = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection().setAutoCommit(false); // Disable autocommit.

            if (person != null)
            {
                context.setCurrentUser(person);
            }

            return context;

        }
        catch (SQLException e)
        {
            context.abort();
            throw new ContextException("Could not create context, SQLException. Message: " + e, e);
        }
    }

    /**
     * It write statistic about using REST api.
     * 
     * @param user
     *            User, which is used by actual context.
     * @param typeOfObject
     *            Type of which object is performed. From class
     *            org.dspace.core.Constants. For example: bitstream, item and so
     *            on.
     * @param dspaceObject
     *            Object of DSpace which is performed.
     * @param action
     *            What action is performed with object.
     * @param user_ip
     * @param user_agent
     * @param xforwarderfor
     * @param headers
     * @param request
     */
    protected void writeStats(int typeOfObject, org.dspace.content.DSpaceObject dspaceObject, UsageEvent.Action action,
            String user_ip, String user_agent, String xforwarderfor, HttpHeaders headers, HttpServletRequest request)
    {

        if (!writeStatistics)
        {
            return;
        }

        org.dspace.core.Context context = null;

        try
        {
            context = createContext(getUser(headers));

            if ((user_ip == null) || (user_ip.length() == 0))
            {
                new DSpace().getEventService().fireEvent(new UsageEvent(action, request, context, dspaceObject));
            }
            else
            {
                new DSpace().getEventService().fireEvent(
                        new UsageEvent(action, user_ip, user_agent, xforwarderfor, context, dspaceObject));
            }

            log.debug("fired event");
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not write usageEvent, SQLException. Message: " + e, context);

        }
        catch (ContextException e)
        {
            processException("Could not write usageEvent, ContextException. Message: " + e.getMessage(), context);
        }

    }

    /**
     * Process exception, print message to logger error stream and abort DSpace
     * context.
     * 
     * @param message
     *            Message, which will be printed to error stream.
     * @param context
     *            Context which must be aborted.
     * @throws WebApplicationException
     *             This exception is throw for user of REST api.
     */
    protected void processException(String message, org.dspace.core.Context context) throws WebApplicationException
    {
        if ((context != null) && (context.isValid()))
        {
            context.abort();
        }
        log.error(message);
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Split string with regex ".".
     * 
     * @param key
     *            String which will be splitted.
     * @return String array filed with separated string.
     */
    protected String[] mySplit(String key)
    {
        ArrayList<String> list = new ArrayList<String>();
        int prev = 0;
        for (int i = 0; i < key.length(); i++)
        {
            if (key.charAt(i) == '.')
            {
                list.add(key.substring(prev, i));
                prev = i + 1;
            }
            else if (i + 1 == key.length())
            {
                list.add(key.substring(prev, i + 1));
            }
        }

        if (list.size() == 2)
        {
            list.add(null);
        }

        return list.toArray(new String[0]);
    }

    /**
     * Return string representation of values
     * org.dspace.core.Constants.{READ,WRITE,DELETE}.
     * 
     * @param action
     *            Constant from org.dspace.core.Constants.*
     * @return String representation. read or write or delete.
     */
    protected String getActionString(int action)
    {
        String actionStr;
        switch (action)
        {
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

    /**
     * Return EPerson based on stored token in headers under
     * "rest-dspace-token".
     * 
     * @param headers
     *            Only must have "rest-api-token" for successfull return of
     *            user.
     * @return Return EPerson logged under token in headers. If token was wrong
     *         or header rest-dspace-token was missing, returns null.
     */
    protected EPerson getUser(HttpHeaders headers)
    {
        List<String> list = headers.getRequestHeader(TokenHolder.TOKEN_HEADER);
        String token = null;
        if ((list != null) && (list.size() > 0))
        {
            token = list.get(0);
            return TokenHolder.getEPerson(token);
        }
        return null;
    }
}
