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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;

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
    protected static Boolean useProxies;

    /** All the IP matchers */
    protected List<IPMatcher> ipMatchers;

    /** All the negative IP matchers */
    protected List<IPMatcher> ipNegativeMatchers;

    protected GroupService groupService;

    
    /**
     * Maps IPMatchers to group names when we don't know group DB ID yet. When
     * the DB ID is known, the IPMatcher is moved to ipMatcherGroupIDs and then
     * points to the DB ID.
     */
    protected Map<IPMatcher, String> ipMatcherGroupNames;

    /** Maps IPMatchers to group IDs (Integers) where we know the group DB ID */
    protected Map<IPMatcher, UUID> ipMatcherGroupIDs;

    /**
     * Initialize an IP authenticator, reading in the configuration. Note this
     * will never fail if the configuration is bad -- a warning will be logged.
     */
    public IPAuthentication()
    {
        ipMatchers = new ArrayList<IPMatcher>();
        ipNegativeMatchers = new ArrayList<IPMatcher>();
        ipMatcherGroupIDs = new HashMap<>();
        ipMatcherGroupNames = new HashMap<>();
        groupService = EPersonServiceFactory.getInstance().getGroupService();

        List<String> propNames = DSpaceServicesFactory.getInstance().getConfigurationService().getPropertyKeys("authentication-ip");

        for(String propName : propNames)
        {
            String[] nameParts = propName.split("\\.");

            if (nameParts.length == 2)
            {
                addMatchers(nameParts[1], DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty(propName));
            }
            else
            {
                log.warn("Malformed configuration property name: "
                        + propName);
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
    protected void addMatchers(String groupName, String[] ipRanges)
    {
        for (String entry : ipRanges)
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

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request,
            String username) throws SQLException
    {
        return false;
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson) throws SQLException
    {
    }

    @Override
    public boolean allowSetPassword(Context context,
            HttpServletRequest request, String username) throws SQLException
    {
        return false;
    }

    @Override
    public boolean isImplicit()
    {
        return true;
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request)
            throws SQLException
    {
        if (request == null)
        {
            return ListUtils.EMPTY_LIST;
        }
        List<Group> groups = new ArrayList<Group>();

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
                    UUID g = ipMatcherGroupIDs.get(ipm);
                    if (g != null)
                    {
                        groups.add(groupService.find(context, g));
                    }
                    else
                    {
                        // See if we have a group name
                        String groupName = ipMatcherGroupNames.get(ipm);

                        if (groupName != null)
                        {
                            Group group = groupService.findByName(context, groupName);
                            if (group != null)
                            {
                                // Add ID so we won't have to do lookup again
                                ipMatcherGroupIDs.put(ipm, (group.getID()));
                                ipMatcherGroupNames.remove(ipm);

                                groups.add(group);
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
                    UUID g = ipMatcherGroupIDs.get(ipm);
                    if (g != null)
                    {
                        groups.remove(groupService.find(context, g));
                    }
                    else
                    {
                        // See if we have a group name
                        String groupName = ipMatcherGroupNames.get(ipm);

                        if (groupName != null)
                        {
                            Group group = groupService.findByName(context, groupName);
                            if (group != null)
                            {
                                // Add ID so we won't have to do lookup again
                                ipMatcherGroupIDs.put(ipm, group.getID());
                                ipMatcherGroupNames.remove(ipm);

                                groups.remove(group);
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


        if (log.isDebugEnabled())
        {
            StringBuilder gsb = new StringBuilder();
            for (Group group : groups) {
                gsb.append(group.getID()).append(", ");
            }

            log.debug(LogManager.getHeader(context, "authenticated",
                    "special_groups=" + gsb.toString()));
        }

        return groups;
    }

    @Override
    public int authenticate(Context context, String username, String password,
            String realm, HttpServletRequest request) throws SQLException
    {
        return BAD_ARGS;
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request,
            HttpServletResponse response)
    {
        return null;
    }

    @Override
    public String loginPageTitle(Context context)
    {
        return null;
    }
}
