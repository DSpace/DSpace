/*
 * AuthorizeManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.authorize;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * AuthorizeManager handles all authorization checks for DSpace. For better
 * security, DSpace assumes that you do not have the right to do something
 * unless that permission is spelled out somewhere. That "somewhere" is the
 * ResourcePolicy table. The AuthorizeManager is given a user, an object, and an
 * action, and it then does a lookup in the ResourcePolicy table to see if there
 * are any policies giving the user permission to do that action.
 * <p>
 * ResourcePolicies now apply to single objects (such as submit (ADD) permission
 * to a collection.)
 * <p>
 * Note: If an eperson is a member of the administrator group (id 1), then they
 * are automatically given permission for all requests another special group is
 * group 0, which is anonymous - all EPeople are members of group 0.
 */
public class AuthorizeManager
{
    /**
     * Utility method, checks that the current user of the given context can
     * perform all of the specified actions on the given object. An
     * <code>AuthorizeException</code> if all the authorizations fail.
     * 
     * @param c
     *            context with the current user
     * @param o
     *            DSpace object user is attempting to perform action on
     * @param actions
     *            array of action IDs from
     *            <code>org.dspace.core.Constants</code>
     * @throws AuthorizeException
     *             if any one of the specified actions cannot be performed by
     *             the current user on the given object.
     * @throws SQLException
     *             if there's a database problem
     */
    public static void authorizeAnyOf(Context c, DSpaceObject o, int[] actions)
            throws AuthorizeException, SQLException
    {
        AuthorizeException ex = null;

        for (int i = 0; i < actions.length; i++)
        {
            try
            {
                authorizeAction(c, o, actions[i]);

                return;
            }
            catch (AuthorizeException e)
            {
                if (ex == null)
                {
                    ex = e;
                }
            }
        }

        throw ex;
    }

    /**
     * Checks that the context's current user can perform the given action on
     * the given object. Throws an exception if the user is not authorized,
     * otherwise the method call does nothing.
     * 
     * @param c
     *            context
     * @param o
     *            a DSpaceObject
     * @param action
     *            action to perform from <code>org.dspace.core.Constants</code>
     * 
     * @throws AuthorizeException
     *             if the user is denied
     */
    public static void authorizeAction(Context c, DSpaceObject o, int action)
            throws AuthorizeException, SQLException
    {
        if (o == null)
        {
            // action can be -1 due to a null entry
            String actionText;

            if (action == -1)
            {
                actionText = "null";
            }
            else
            {
                actionText = Constants.actionText[action];
            }

            EPerson e = c.getCurrentUser();
            int userid;

            if (e == null)
            {
                userid = 0;
            }
            else
            {
                userid = e.getID();
            }

            throw new AuthorizeException(
                    "Authorization attempted on null DSpace object "
                            + actionText + " by user " + userid);
        }

        if (!authorize(c, o, action, c.getCurrentUser()))
        {
            // denied, assemble and throw exception
            int otype = o.getType();
            int oid = o.getID();
            int userid;
            EPerson e = c.getCurrentUser();

            if (e == null)
            {
                userid = 0;
            }
            else
            {
                userid = e.getID();
            }

            //            AuthorizeException j = new AuthorizeException("Denied");
            //            j.printStackTrace();
            // action can be -1 due to a null entry
            String actionText;

            if (action == -1)
            {
                actionText = "null";
            }
            else
            {
                actionText = Constants.actionText[action];
            }

            throw new AuthorizeException("Authorization denied for action "
                    + actionText + " on " + Constants.typeText[otype] + ":"
                    + oid + " by user " + userid, o, action);
        }
    }

    /**
     * same authorize, returns boolean for those who don't want to deal with
     * catching exceptions.
     * 
     * @param c
     *            DSpace context, containing current user
     * @param o
     *            DSpaceObject
     * @param a
     *            action being attempted, from
     *            <code>org.dspace.core.Constants</code>
     * 
     * @return <code>true</code> if the current user in the context is
     *         authorized to perform the given action on the given object
     */
    public static boolean authorizeActionBoolean(Context c, DSpaceObject o,
            int a) throws SQLException
    {
        boolean isAuthorized = true;

        if (o == null)
        {
            return false;
        }

        try
        {
            authorizeAction(c, o, a);
        }
        catch (AuthorizeException e)
        {
            isAuthorized = false;
        }

        return isAuthorized;
    }

    /**
     * Check to see if the given user can perform the given action on the given
     * object. Always returns true if the ignore authorization flat is set in
     * the current context.
     * 
     * @param c
     *            current context. User is irrelevant; "ignore authorization"
     *            flag is relevant
     * @param o
     *            object action is being attempted on
     * @param action
     *            ID of action being attempted, from
     *            <code>org.dspace.core.Constants</code>
     * @param e
     *            user attempting action
     * @return <code>true</code> if user is authorized to perform the given
     *         action, <code>false</code> otherwise
     * @throws SQLException
     */
    private static boolean authorize(Context c, DSpaceObject o, int action,
            EPerson e) throws SQLException
    {
        int userid;

        // return FALSE if there is no DSpaceObject
        if (o == null)
        {
            return false;
        }

        // is authorization disabled for this context?
        if (c.ignoreAuthorization())
        {
            return true;
        }

        // is eperson set? if not, userid = 0 (anonymous)
        if (e == null)
        {
            userid = 0;
        }
        else
        {
            userid = e.getID();

            // perform isadmin check since user
            // is user part of admin group?
            if (isAdmin(c))
            {
                return true;
            }
        }

        for (ResourcePolicy rp : getPoliciesActionFilter(c, o, action))
        {
            // check policies for date validity
            if (rp.isDateValid())
            {
                if ((rp.getEPersonID() != -1) && (rp.getEPersonID() == userid))
                {
                    return true; // match
                }

                if ((rp.getGroupID() != -1)
                        && (Group.isMember(c, rp.getGroupID())))
                {
                    // group was set, and eperson is a member
                    // of that group
                    return true;
                }
            }
        }

        // default authorization is denial
        return false;
    }

    ///////////////////////////////////////////////
    // admin check methods
    ///////////////////////////////////////////////

    /**
     * Check to see if the current user is an admin. Always return
     * <code>true</code> if c.ignoreAuthorization is set. Anonymous users
     * can't be Admins (EPerson set to NULL)
     * 
     * @param c
     *            current context
     * 
     * @return <code>true</code> if user is an admin or ignore authorization
     *         flag set
     */
    public static boolean isAdmin(Context c) throws SQLException
    {
        // if we're ignoring authorization, user is member of admin
        if (c.ignoreAuthorization())
        {
            return true;
        }

        EPerson e = c.getCurrentUser();

        if (e == null)
        {
            return false; // anonymous users can't be admins....
        }
        else
        {
            return Group.isMember(c, 1);
        }
    }

    ///////////////////////////////////////////////
    // policy manipulation methods
    ///////////////////////////////////////////////

    /**
     * Add a policy for an individual eperson
     * 
     * @param c
     *            context. Current user irrelevant
     * @param o
     *            DSpaceObject to add policy to
     * @param actionID
     *            ID of action from <code>org.dspace.core.Constants</code>
     * @param e
     *            eperson who can perform the action
     * 
     * @throws AuthorizeException
     *             if current user in context is not authorized to add policies
     */
    public static void addPolicy(Context c, DSpaceObject o, int actionID,
            EPerson e) throws SQLException, AuthorizeException
    {
        ResourcePolicy rp = ResourcePolicy.create(c);

        rp.setResource(o);
        rp.setAction(actionID);
        rp.setEPerson(e);

        rp.update();
    }

    /**
     * Add a policy for a group
     * 
     * @param c
     *            current context
     * @param o
     *            object to add policy for
     * @param actionID
     *            ID of action from <code>org.dspace.core.Constants</code>
     * @param g
     *            group to add policy for
     * @throws SQLException
     *             if there's a database problem
     * @throws AuthorizeException
     *             if the current user is not authorized to add this policy
     */
    public static void addPolicy(Context c, DSpaceObject o, int actionID,
            Group g) throws SQLException, AuthorizeException
    {
        ResourcePolicy rp = ResourcePolicy.create(c);

        rp.setResource(o);
        rp.setAction(actionID);
        rp.setGroup(g);

        rp.update();
    }

    /**
     * Return a List of the policies for an object
     * 
     * @param c  current context
     * @param o  object to retrieve policies for
     * 
     * @return List of <code>ResourcePolicy</code> objects
     */
    public static List<ResourcePolicy> getPolicies(Context c, DSpaceObject o)
            throws SQLException
    {
    	TableRowIterator tri = DatabaseManager.queryTable(c, "resourcepolicy",
                "SELECT * FROM resourcepolicy WHERE resource_type_id= ? AND resource_id= ? ",
                o.getType(),o.getID());

        List<ResourcePolicy> policies = new ArrayList();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // first check the cache (FIXME: is this right?)
            ResourcePolicy cachepolicy = (ResourcePolicy) c.fromCache(
                    ResourcePolicy.class, row.getIntColumn("policy_id"));

            if (cachepolicy != null)
            {
                policies.add(cachepolicy);
            }
            else
            {
                policies.add(new ResourcePolicy(c, row));
            }
        }
        tri.close();

        return policies;
    }

    /**
     * Return a List of the policies for a group
     *
     * @param c  current context
     * @param g  group to retrieve policies for
     *
     * @return List of <code>ResourcePolicy</code> objects
     */
    public static List<ResourcePolicy> getPoliciesForGroup(Context c, Group g)
            throws SQLException
    {
    	TableRowIterator tri = DatabaseManager.queryTable(c, "resourcepolicy",
                "SELECT * FROM resourcepolicy WHERE epersongroup_id= ? ",
                g.getID());

        List<ResourcePolicy> policies = new ArrayList<ResourcePolicy>();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // first check the cache (FIXME: is this right?)
            ResourcePolicy cachepolicy = (ResourcePolicy) c.fromCache(
                    ResourcePolicy.class, row.getIntColumn("policy_id"));

            if (cachepolicy != null)
            {
                policies.add(cachepolicy);
            }
            else
            {
                policies.add(new ResourcePolicy(c, row));
            }
        }
        tri.close();

        return policies;
    }
    
    /**
     * Return a list of policies for an object that match the action
     * 
     * @param c
     *            context
     * @param o
     *            DSpaceObject policies relate to
     * @param actionID
     *            action (defined in class Constants)
     * @throws SQLException
     *             if there's a database problem
     */
    public static List<ResourcePolicy> getPoliciesActionFilter(Context c, DSpaceObject o,
            int actionID) throws SQLException
    {
    	TableRowIterator tri = DatabaseManager.queryTable(c, "resourcepolicy",
                "SELECT * FROM resourcepolicy WHERE resource_type_id= ? "+
                "AND resource_id= ? AND action_id= ? ", 
                o.getType(), o.getID(),actionID);

        List<ResourcePolicy> policies = new ArrayList<ResourcePolicy>();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // first check the cache (FIXME: is this right?)
            ResourcePolicy cachepolicy = (ResourcePolicy) c.fromCache(
                    ResourcePolicy.class, row.getIntColumn("policy_id"));

            if (cachepolicy != null)
            {
                policies.add(cachepolicy);
            }
            else
            {
                policies.add(new ResourcePolicy(c, row));
            }
        }
        tri.close();

        return policies;
    }

    /**
     * Add policies to an object to match those from a previous object
     * 
     * @param c  context
     * @param src
     *            source of policies
     * @param dest
     *            destination of inherited policies
     * @throws SQLException
     *             if there's a database problem
     * @throws AuthorizeException
     *             if the current user is not authorized to add these policies
     */
    public static void inheritPolicies(Context c, DSpaceObject src,
            DSpaceObject dest) throws SQLException, AuthorizeException
    {
        // find all policies for the source object
        List<ResourcePolicy> policies = getPolicies(c, src);

        addPolicies(c, policies, dest);
    }

    /**
     * Copies policies from a list of resource policies to a given DSpaceObject
     * 
     * @param c
     *            DSpace context
     * @param policies
     *            List of ResourcePolicy objects
     * @param dest
     *            object to have policies added
     * @throws SQLException
     *             if there's a database problem
     * @throws AuthorizeException
     *             if the current user is not authorized to add these policies
     */
    public static void addPolicies(Context c, List<ResourcePolicy> policies, DSpaceObject dest)
            throws SQLException, AuthorizeException
    {
        // now add them to the destination object
        for (ResourcePolicy srp : policies)
        {
            ResourcePolicy drp = ResourcePolicy.create(c);

            // copy over values
            drp.setResource(dest);
            drp.setAction(srp.getAction());
            drp.setEPerson(srp.getEPerson());
            drp.setGroup(srp.getGroup());
            drp.setStartDate(srp.getStartDate());
            drp.setEndDate(srp.getEndDate());

            // and write out new policy
            drp.update();
        }
    }

    /**
     * removes ALL policies for an object.  FIXME doesn't check authorization
     * 
     * @param c
     *            DSpace context
     * @param o
     *            object to remove policies for
     * @throws SQLException
     *             if there's a database problem
     */
    public static void removeAllPolicies(Context c, DSpaceObject o)
            throws SQLException
    {
        // FIXME: authorization check?
    	 DatabaseManager.updateQuery(c, "DELETE FROM resourcepolicy WHERE "
                 + "resource_type_id= ? AND resource_id= ? ",
                 o.getType(), o.getID());
    }

    /**
     * Remove all policies from an object that match a given action. FIXME
     * doesn't check authorization
     * 
     * @param context
     *            current context
     * @param dso
     *            object to remove policies from
     * @param actionID
     *            ID of action to match from
     *            <code>org.dspace.core.Constants</code>, or -1=all
     * @throws SQLException
     *             if there's a database problem
     */
    public static void removePoliciesActionFilter(Context context,
            DSpaceObject dso, int actionID) throws SQLException
    {
        if (actionID == -1)
        {
            // remove all policies from object
            removeAllPolicies(context, dso);
        }
        else
        {
        	DatabaseManager.updateQuery(context,
                    "DELETE FROM resourcepolicy WHERE resource_type_id= ? AND "+
                    "resource_id= ? AND action_id= ? ",
                    dso.getType(), dso.getID(), actionID);
        }
    }

    /**
     * Removes all policies relating to a particular group. FIXME doesn't check
     * authorization
     * 
     * @param c
     *            current context
     * @param groupID
     *            ID of the group
     * @throws SQLException
     *             if there's a database problem
     */
    public static void removeGroupPolicies(Context c, int groupID)
            throws SQLException
    {
        DatabaseManager.updateQuery(c, "DELETE FROM resourcepolicy WHERE "
                + "epersongroup_id= ? ", groupID);
    }

    /**
     * Removes all policies from a group for a particular object that belong to
     * a Group. FIXME doesn't check authorization
     * 
     * @param c
     *            current context
     * @param o
     *            the object
     * @param g
     *            the group
     * @throws SQLException
     *             if there's a database problem
     */
    public static void removeGroupPolicies(Context c, DSpaceObject o, Group g)
            throws SQLException
    {
        DatabaseManager.updateQuery(c, "DELETE FROM resourcepolicy WHERE "
                + "resource_type_id= ? AND resource_id= ? AND epersongroup_id= ? ",
                o.getType(), o.getID(), g.getID());
    }

    /**
     * Returns all groups authorized to perform an action on an object. Returns
     * empty array if no matches.
     * 
     * @param c
     *            current context
     * @param o
     *            object
     * @param actionID
     *            ID of action frm <code>org.dspace.core.Constants</code>
     * @return array of <code>Group</code>s that can perform the specified
     *         action on the specified object
     * @throws java.sql.SQLException
     *             if there's a database problem
     */
    public static Group[] getAuthorizedGroups(Context c, DSpaceObject o,
            int actionID) throws java.sql.SQLException
    {
        // do query matching groups, actions, and objects
        TableRowIterator tri = DatabaseManager.queryTable(c, "resourcepolicy",
                "SELECT * FROM resourcepolicy WHERE resource_type_id= ? "+
                "AND resource_id= ? AND action_id= ? ",o.getType(),o.getID(),actionID);
    	
        List<Group> groups = new ArrayList<Group>();

        while (tri.hasNext())
        {
            TableRow row = tri.next();

            // first check the cache (FIXME: is this right?)
            ResourcePolicy cachepolicy = (ResourcePolicy) c.fromCache(
                    ResourcePolicy.class, row.getIntColumn("policy_id"));

            ResourcePolicy myPolicy = null;

            if (cachepolicy != null)
            {
                myPolicy = cachepolicy;
            }
            else
            {
                myPolicy = new ResourcePolicy(c, row);
            }

            // now do we have a group?
            Group myGroup = myPolicy.getGroup();

            if (myGroup != null)
            {
                groups.add(myGroup);
            }
        }
        tri.close();

        Group[] groupArray = new Group[groups.size()];
        groupArray = groups.toArray(groupArray);

        return groupArray;
    }
}
