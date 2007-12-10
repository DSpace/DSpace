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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.authorize.dao.ResourcePolicyDAOFactory;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;

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
    private static Logger log = Logger.getLogger(AuthorizeManager.class);

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
     */
    public static void authorizeAnyOf(Context c, DSpaceObject o, int[] actions)
            throws AuthorizeException
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
            throws AuthorizeException
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
                    + oid + " by user " + userid + " in context " + c, o, action);
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
            int a)
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
     */
    private static boolean authorize(Context c, DSpaceObject o, int action,
            EPerson e)
    {
        GroupDAO groupDAO = GroupDAOFactory.getInstance(c);

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

                if ((rp.getGroupID() != -1) &&
                        groupDAO.currentUserInGroup(rp.getGroupID()))
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
    public static boolean isAdmin(Context c)
    {
        GroupDAO groupDAO = GroupDAOFactory.getInstance(c);

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
            // FIXME: horrible assumption about the admin group having ID '1'.
            return groupDAO.currentUserInGroup(1);
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
            EPerson e) throws AuthorizeException
    {
        ResourcePolicyDAO dao = ResourcePolicyDAOFactory.getInstance(c);
        ResourcePolicy rp = dao.create();

        rp.setResource(o);
        rp.setAction(actionID);
        rp.setEPerson(e);

        dao.update(rp);
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
     * @throws AuthorizeException
     *             if the current user is not authorized to add this policy
     */
    public static void addPolicy(Context c, DSpaceObject o, int actionID,
            Group g) throws AuthorizeException
    {
        ResourcePolicyDAO dao = ResourcePolicyDAOFactory.getInstance(c);
        ResourcePolicy rp = dao.create();

        rp.setResource(o);
        rp.setAction(actionID);
        rp.setGroup(g);

        dao.update(rp);
    }

    /**
     * Return a List of the policies for an object
     * 
     * @param c  current context
     * @param o  object to retrieve policies for
     * 
     * @return List of <code>ResourcePolicy</code> objects
     */
    @Deprecated
    public static List<ResourcePolicy> getPolicies(Context c, DSpaceObject o)
    {
        return ResourcePolicyDAOFactory.getInstance(c).getPolicies(o);
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
    {
        return ResourcePolicyDAOFactory.getInstance(c).getPolicies(g);
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
     */
    public static List<ResourcePolicy> getPoliciesActionFilter(Context c, DSpaceObject o,
            int actionID)
    {
        return ResourcePolicyDAOFactory.getInstance(c).getPolicies(o, actionID);
    }

    /**
     * Add policies to an object to match those from a previous object
     * 
     * @param c  context
     * @param src
     *            source of policies
     * @param dest
     *            destination of inherited policies
     * @throws AuthorizeException
     *             if the current user is not authorized to add these policies
     */
    public static void inheritPolicies(Context c, DSpaceObject src,
            DSpaceObject dest) throws AuthorizeException
    {
        ResourcePolicyDAO dao = ResourcePolicyDAOFactory.getInstance(c);

        // find all policies for the source object
        List<ResourcePolicy> policies = dao.getPolicies(src);

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
     * @throws AuthorizeException
     *             if the current user is not authorized to add these policies
     */
    public static void addPolicies(Context c, List<ResourcePolicy> policies, DSpaceObject dest)
            throws AuthorizeException
    {
        ResourcePolicyDAO dao = ResourcePolicyDAOFactory.getInstance(c);

        // now add them to the destination object
        for (ResourcePolicy srp : policies)
        {
            ResourcePolicy drp = dao.create();

            // copy over values
            drp.setResource(dest);
            drp.setAction(srp.getAction());
            drp.setEPerson(srp.getEPerson());
            drp.setGroup(srp.getGroup());
            drp.setStartDate(srp.getStartDate());
            drp.setEndDate(srp.getEndDate());

            // and write out new policy
            dao.update(drp);
        }
    }

    /**
     * removes ALL policies for an object.  FIXME doesn't check authorization
     * 
     * @param c
     *            DSpace context
     * @param o
     *            object to remove policies for
     */
    public static void removeAllPolicies(Context c, DSpaceObject dso)
    {
        ResourcePolicyDAO dao = ResourcePolicyDAOFactory.getInstance(c);

        for (ResourcePolicy rp : dao.getPolicies(dso))
        {
            dao.delete(rp.getID());
        }
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
     */
    public static void removePoliciesActionFilter(Context context,
            DSpaceObject dso, int actionID)
    {
        if (actionID == -1)
        {
            // remove all policies from object
            removeAllPolicies(context, dso);
        }
        else
        {
            ResourcePolicyDAO dao =
                ResourcePolicyDAOFactory.getInstance(context);
            for (ResourcePolicy rp : dao.getPolicies(dso, actionID))
            {
                dao.delete(rp.getID());
            }
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
     */
    public static void removeGroupPolicies(Context context, int groupID)
    {
        ResourcePolicyDAO dao = ResourcePolicyDAOFactory.getInstance(context);
        Group group = GroupDAOFactory.getInstance(context).retrieve(groupID);

        if (group == null)
        {
            throw new IllegalArgumentException("Couldn't find group with id " +
                    groupID);
        }

        for (ResourcePolicy rp : dao.getPolicies(group))
        {
            dao.delete(rp.getID());
        }
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
     */
    public static void removeGroupPolicies(Context c, DSpaceObject o, Group g)
    {
        ResourcePolicyDAO dao = ResourcePolicyDAOFactory.getInstance(c);

        for (ResourcePolicy rp : dao.getPolicies(o, g))
        {
            dao.delete(rp.getID());
        }
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
     */
    public static Group[] getAuthorizedGroups(Context c, DSpaceObject dso,
            int actionID)
    {
        ResourcePolicyDAO dao = ResourcePolicyDAOFactory.getInstance(c);
        List<Group> groups = new ArrayList<Group>();

        for (ResourcePolicy rp : dao.getPolicies(dso, actionID))
        {
            groups.add(rp.getGroup());
        }

        return groups.toArray(new Group[0]);
    }
}
