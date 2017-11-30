/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Adds users to special groups based on IP address. Configuration parameter
 * form is:
 * <P>
 * {@code authentication.ip.<GROUPNAME> = <IPRANGE>[, <IPRANGE> ...]}
 * <P>
 * e.g. {@code authentication.ip.MIT = 18., 192.25.0.0/255.255.0.0}
 * <P>
 * Negative matches can be included by prepending the range with a '-'. For example if you want
 * to include all of a class B network except for users of a contained class c network, you could use:
 * <P>
 * 111.222,-111.222.333.
 * <p>
 * For supported IP ranges see {@link org.dspace.authenticate.IPMatcher}.
 * 
 * @version $Revision$
 * @author Robert Tansley
 */
public class IPAuthentication implements AuthenticationMethod
{
    /** Our logger */
    private static Logger log = Logger.getLogger(IPAuthentication.class);

    /** Whether to look for x-forwarded headers for logging IP addresses */
    private static Boolean useProxies;

    /** All the IP matchers */
    private List<IPMatcher> ipMatchers;

    /** All the negative IP matchers */
    private List<IPMatcher> ipNegativeMatchers;

    
    /**
     * Maps IPMatchers to group names when we don't know group DB ID yet. When
     * the DB ID is known, the IPMatcher is moved to ipMatcherGroupIDs and then
     * points to the DB ID.
     */
    private Map<IPMatcher, String> ipMatcherGroupNames;

    /** Maps IPMatchers to group IDs (Integers) where we know the group DB ID */
    private Map<IPMatcher, Integer> ipMatcherGroupIDs;

    /**
     * Initialize an IP authenticator, reading in the configuration. Note this
     * will never fail if the configuration is bad -- a warning will be logged.
     */
    public IPAuthentication()
    {
        ipMatchers = new ArrayList<IPMatcher>();
        ipNegativeMatchers = new ArrayList<IPMatcher>();
        ipMatcherGroupIDs = new HashMap<IPMatcher, Integer>();
        ipMatcherGroupNames = new HashMap<IPMatcher, String>();

        Enumeration e = ConfigurationManager.propertyNames("authentication-ip");

        while (e.hasMoreElements())
        {
            String propName = (String) e.nextElement();
            if (propName.startsWith("ip."))
            {
                String[] nameParts = propName.split("\\.");

                if (nameParts.length == 2)
                {
                    addMatchers(nameParts[1], ConfigurationManager.getProperty("authentication-ip", propName));
                }
                else
                {
                    log.warn("Malformed configuration property name: "
                            + propName);
                }
            }
        }
    }

    /**
     * Add matchers for the given comma-delimited IP ranges and group.
     * 
     * @param groupName
     *            name of group
     * @param ipRanges
     *            IP ranges
     */
    private void addMatchers(String groupName, String ipRanges)
    {
        String[] ranges = ipRanges.split("\\s*,\\s*");

        for (String entry : ranges)
        {
            try
            {
                IPMatcher ipm;
                if (entry.startsWith("-"))
                {
                    ipm = new IPMatcher(entry.substring(1));
                    ipNegativeMatchers.add(ipm);
                }
                else
                {
                    ipm = new IPMatcher(entry);
                    ipMatchers.add(ipm);
                }
                ipMatcherGroupNames.put(ipm, groupName);

                if (log.isDebugEnabled())
                {
                    log.debug("Configured " + entry + " for special group "
                            + groupName);
                }
            }
            catch (IPMatcherException ipme)
            {
                log.warn("Malformed IP range specified for group " + groupName,
                        ipme);
            }
        }
    }

    public boolean canSelfRegister(Context context, HttpServletRequest request,
            String username) throws SQLException
    {
        return false;
    }

    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson) throws SQLException
    {
    }

    public boolean allowSetPassword(Context context,
            HttpServletRequest request, String username) throws SQLException
    {
        return false;
    }

    public boolean isImplicit()
    {
        return true;
    }

    public int[] getSpecialGroups(Context context, HttpServletRequest request)
            throws SQLException
    {
        if (request == null)
        {
            return new int[0];
        }
        List<Integer> groupIDs = new ArrayList<Integer>();

        // Get the user's IP address
        String addr = request.getRemoteAddr();
        if (useProxies == null) {
            useProxies = ConfigurationManager.getBooleanProperty("useProxies", false);
        }
        if (useProxies && request.getHeader("X-Forwarded-For") != null)
        {
            /* This header is a comma delimited list */
            for(String xfip : request.getHeader("X-Forwarded-For").split(","))
            {
                if(!request.getHeader("X-Forwarded-For").contains(addr))
                {
                    addr = xfip.trim();
                }
            }
        }

        for (IPMatcher ipm : ipMatchers)
        {
            try
            {
                if (ipm.match(addr))
                {
                    // Do we know group ID?
                    Integer g = ipMatcherGroupIDs.get(ipm);
                    if (g != null)
                    {
                        groupIDs.add(g);
                    }
                    else
                    {
                        // See if we have a group name
                        String groupName = ipMatcherGroupNames.get(ipm);

                        if (groupName != null)
                        {
                            Group group = Group.findByName(context, groupName);
                            if (group != null)
                            {
                                // Add ID so we won't have to do lookup again
                                ipMatcherGroupIDs.put(ipm, Integer.valueOf(group.getID()));
                                ipMatcherGroupNames.remove(ipm);

                                groupIDs.add(Integer.valueOf(group.getID()));
                            }
                            else
                            {
                                log.warn(LogManager.getHeader(context,
                                        "configuration_error", "unknown_group="
                                                + groupName));
                            }
                        }
                    }
                }
            }
            catch (IPMatcherException ipme)
            {
                log.warn(LogManager.getHeader(context, "configuration_error",
                        "bad_ip=" + addr), ipme);
            }
        }

        // Now remove any negative matches
        for (IPMatcher ipm : ipNegativeMatchers)
        {
            try
            {
                if (ipm.match(addr))
                {
                    // Do we know group ID?
                    Integer g = ipMatcherGroupIDs.get(ipm);
                    if (g != null)
                    {
                        groupIDs.remove(g);
                    }
                    else
                    {
                        // See if we have a group name
                        String groupName = ipMatcherGroupNames.get(ipm);

                        if (groupName != null)
                        {
                            Group group = Group.findByName(context, groupName);
                            if (group != null)
                            {
                                // Add ID so we won't have to do lookup again
                                ipMatcherGroupIDs.put(ipm, Integer.valueOf(group.getID()));
                                ipMatcherGroupNames.remove(ipm);

                                groupIDs.remove(Integer.valueOf(group.getID()));
                            }
                            else
                            {
                                log.warn(LogManager.getHeader(context,
                                        "configuration_error", "unknown_group="
                                                + groupName));
                            }
                        }
                    }
                }
            }
            catch (IPMatcherException ipme)
            {
                log.warn(LogManager.getHeader(context, "configuration_error",
                        "bad_ip=" + addr), ipme);
            }
        }


        int[] results = new int[groupIDs.size()];
        for (int i = 0; i < groupIDs.size(); i++)
        {
            results[i] = (groupIDs.get(i)).intValue();
        }

        if (log.isDebugEnabled())
        {
            StringBuffer gsb = new StringBuffer();

            for (int i = 0; i < results.length; i++)
            {
                if (i > 0)
                {
                    gsb.append(",");
                }
                gsb.append(results[i]);
            }

            log.debug(LogManager.getHeader(context, "authenticated",
                    "special_groups=" + gsb.toString()));
        }

        return results;
    }

    public int authenticate(Context context, String username, String password,
            String realm, HttpServletRequest request) throws SQLException
    {
        return BAD_ARGS;
    }

    public String loginPageURL(Context context, HttpServletRequest request,
            HttpServletResponse response)
    {
        return null;
    }

    public String loginPageTitle(Context context)
    {
        return null;
    }
}
