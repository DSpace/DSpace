/*
 * IPAuthentication.java
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
 * For supported IP ranges see {@link org.dspace.authenticate.IPMatcher}.
 * 
 * @version $Revision$
 * @author Robert Tansley
 */
public class IPAuthentication implements AuthenticationMethod
{
    /** Our logger */
    private static Logger log = Logger.getLogger(IPAuthentication.class);

    /** All the IP matchers */
    private List<IPMatcher> ipMatchers;

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
        ipMatcherGroupIDs = new HashMap<IPMatcher, Integer>();
        ipMatcherGroupNames = new HashMap<IPMatcher, String>();

        Enumeration e = ConfigurationManager.propertyNames();

        while (e.hasMoreElements())
        {
            String propName = (String) e.nextElement();
            if (propName.startsWith("authentication.ip."))
            {
                String[] nameParts = propName.split("\\.");

                if (nameParts.length == 3)
                {
                    addMatchers(nameParts[2], ConfigurationManager
                            .getProperty(propName));
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

        for (int i = 0; i < ranges.length; i++)
        {
            try
            {
                IPMatcher ipm = new IPMatcher(ranges[i]);
                ipMatchers.add(ipm);
                ipMatcherGroupNames.put(ipm, groupName);

                if (log.isDebugEnabled())
                {
                    log.debug("Configured " + ranges[i] + " for special group "
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
        List<Integer> groupIDs = new ArrayList<Integer>();

        String addr = request.getRemoteAddr();

        for (int i = 0; i < ipMatchers.size(); i++)
        {
            IPMatcher ipm = ipMatchers.get(i);

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
                                ipMatcherGroupIDs.put(ipm, new Integer(group
                                        .getID()));
                                ipMatcherGroupNames.remove(ipm);

                                groupIDs.add(new Integer(group.getID()));
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
                    gsb.append(",");
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
