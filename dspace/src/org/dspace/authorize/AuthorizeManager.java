/*
 * AuthorizeManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

import java.util.StringTokenizer;
import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import org.dspace.history.HistoryManager;

/**
 * AuthorizeManager handles all authorization checks for DSpace.
 * For better security, DSpace assumes that you do not have the right
 * to do something unless that permission is spelled out somewhere.
 * That "somewhere" is the ResourcePolicy table.  The AuthorizeManager
 * is given a user, an object, and an action, and it then does a lookup
 * in the ResourcePolicy table to see if there are any policies giving
 * the user permission to do that action.
 * <p>
 * ResourcePolicies will usually apply to a single object (such
 * as submit permission to a collection,) or a container full
 * of objects, such as read permission for items contained in
 * a collection.
 * <p>
 * PolicyStatements are currently a very simple language - a list
 * of comma-delimited groups and users.  Groups are identified
 * by gID and users by uID, so <code>g10,u5432</code> is a policy
 * statement saying that user 5432 and members of group 10 are
 * allowed to do the referred action.
 * <p>
 * Note: If an eperson is a member of the administrator group (id -1), then
 *  they are automatically given permission for all requests.
 */

public class AuthorizeManager
{
    /**
     * primary authorization interface, assumes user is the context's
     *  currentuser, and throws an exception
     *
     * @param c Context object
     * @param o DSpace object
     * @param a action from org.dspace.core.Constants
     *
     * @throws AuthorizeException if permission denied or object type unknown
     */
    public static void authorizeAction(Context c,Object myobject, int myaction)
        throws SQLException, AuthorizeException
    {
        int otype;
        int oid;
        int userid = -1;  // -1 is anonymous

        // now figure out the type and object id
        if( myobject instanceof Item )
        {
            otype = Constants.ITEM;
            oid   = ((Item) myobject).getID();
        }
        else if( myobject instanceof Bitstream )
        {
            otype = Constants.BITSTREAM;
            oid   = ((Bitstream) myobject).getID();
        }
        else if( myobject instanceof Collection )
        {
            otype = Constants.COLLECTION;
            oid   = ((Collection) myobject).getID();
        }
        else if( myobject instanceof Community )
        {
            otype = Constants.COMMUNITY;
            oid   = ((Community) myobject).getID();
        }
        else if( myobject instanceof Bundle )
        {
            otype = Constants.BUNDLE;
            oid   = ((Bundle) myobject).getID();
        }
        else
        {
            throw new IllegalArgumentException("Unknown object type");
        }

        // now set the userid if context contains an eperson
        EPerson e = c.getCurrentUser();

        if( e != null ) userid = e.getID();

        if (!authorize(c,otype,oid,myaction, userid))
        {
            System.out.println( HistoryManager.getStackTrace() );
            
            throw new AuthorizeException("Authorization denied for action "
                                        + Constants.actiontext[myaction]
                                        + "on " + Constants.typetext[otype]
                                        + ":" + oid + " by user " + userid);
        }
    }


    /**
     * same authorize, returns boolean for those who don't want to bother
     *  catching exceptions.
     */
    public static boolean authorize(Context c, Object o, int a)
        throws SQLException
    {
        boolean isauthorized = true;

        try
        {
            authorizeAction(c,o,a);
        }
        catch( AuthorizeException e )
        {
            isauthorized = false;
        }

        return isauthorized;
    }


    /**
     * check to see if the current user is an admin,
     *  or always return true if c.ignoreAuthorization is set.
     *  Anonymous users can't be Admins (EPerson set to NULL)
     */
    public static boolean isAdmin(Context c)
        throws SQLException
    {
        // if we're ignoring authorization, user is member of admin
        if(c.ignoreAuthorization()) return true;

        EPerson e = c.getCurrentUser();

        if( e == null ) return false; // anonymous users can't be admins....

        else
            return isAdmin(c,e.getID());
    }


    /**
     * check to see if the a given userid is an admin
     */
    public static boolean isAdmin(Context c,int userid)
        throws SQLException
    {
        // group is hardcoded as 'admin', and admins can do everything
        if( Group.isMember(c,-1,userid) )
            return true;
        else
            return false;
    }


    /**
     * authorize() is the authorize method that returns a boolean - always
     *  returns true if c.ignoreAuthorization is set
     *
     * @param resourcetype - found core.Constants (collection, item, etc.)
     * @param resorceidID of resource you're trying to do an authorize on
     * @param actionid - action to perform (read, write, etc)
     */
    public static boolean authorize(Context c,int resourcetype, int resourceid, int actionid,
                                    int userid)
        throws SQLException
    {
        // ignore authorization? if so, return true
        if(c.ignoreAuthorization()) return true;

        // admins can do everything
        if( isAdmin(c,userid) ) return true;

        TableRowIterator i = policyLookup(c,resourcetype, resourceid, actionid);

        // no policies?  notify admins and give 'false'
        if (!i.hasNext())
        {
            //alert( "no policies for this object" );
        }

        while( i.hasNext() )
        {
            TableRow row = (TableRow)i.next();

            ResourcePolicy rp = new ResourcePolicy(c, row);

            // evaluate each statement
            StringTokenizer st = new StringTokenizer(rp.getPolicy(), ",");

            while (st.hasMoreTokens())
            {
                String t = st.nextToken();

                if (t.startsWith("u"))      // userid
                {
                    int uid = -1;

                    try
                    {
                        uid = Integer.parseInt(t.substring(1));
                    }
                    catch (NumberFormatException e)
                    {
                        // eek! this should never happen
                    }

                    if ((uid != -1) && (uid == userid)) return true;
                }
                else if (t.startsWith("g")) // groupid
                {
                    int gid = -1;

                    try
                    {
                        gid = Integer.parseInt(t.substring(1));
                    }
                    catch (NumberFormatException e)
                    {
                        // once again, eek!
                    }

                    if (Group.isMember(c,gid, userid)) return true;
                }
                else if (t.startsWith("p")) // predicate
                {
                    String p = t.substring(1);

                    if (p.equals("true")) return true;
                }
            }
        }

        return false;  // default policy
    }


    /**
     * Fetches policies that apply to an object and action pair
     * looks for policies specific to that object, and if not
     * found, then looks for containers that may have policies
     * that apply (bitstreams look for containing items & collections,
     * items look for containing collections.)
     * <p>
     * Policies that apply specifically to an object override
     * any policies that may apply due to reference by a container.
     * This override is done simply by ceasing to look for other
     * policies once a specific policy is found.
     *
     */
    private static TableRowIterator policyLookup(Context c, int resource_type, int resource_id,
                                                int action_id)
        throws SQLException
    {
        String myquery = "";

        // get the policies eonly for this object (rare)
        TableRowIterator specific_policies = DatabaseManager.query(c,
            "resourcepolicy",
            "SELECT resourcepolicy.* FROM resourcepolicy WHERE" +
            " resource_type_id=" + resource_type +
            " AND action_id=" + action_id +
            " AND resource_id=" + resource_id );

        if (resource_type == Constants.BITSTREAM)
        {
            // if there are item specific policies, they have
            //  the highest priority - return them
            if (specific_policies.hasNext())
            {
                return specific_policies;
            }

            // need to look for policies from containing items
            myquery = "SELECT * from ResourcePolicy where" +
                    " resource_type_id = " + resource_type +
                    " AND action_id = " + action_id +
                    " AND container_type_id = " + Constants.ITEM +
                    " AND container_id in " +
                    "(select item_id from Item2Bundle where bundle_id in" +
                    "(select bundle_id from Bundle2Bitstream where " +
                    " bitstream_id = " + resource_id + "))";

            TableRowIterator item_policies = DatabaseManager.query(c,
                "resourcepolicy",
               myquery );

            if (item_policies.hasNext())
            {
                return item_policies;
            }

            // now look for policies from containing collections
            myquery = "SELECT * from ResourcePolicy where" +
                    " resource_type_id = " + resource_type +
                    " AND action_id = " + action_id +
                    " AND container_type_id = " + Constants.COLLECTION +
                    " AND container_id in " +
                    "(select collection_id from Collection2Item where" +
                    " item_id in " +
                    "(select item_id from Item2Bundle where bundle_id in" +
                    "(select bundle_id from Bundle2Bitstream where " +
                    " bitstream_id = " + resource_id + ")))";

            TableRowIterator collection_policies = DatabaseManager.query(c,
                "resourcepolicy",
                myquery );

            if (collection_policies.hasNext())
            {
                return collection_policies;
            }
        }

        // bundles use the same permissions as bitstreams?
        // inheriting from items and collections
        if (resource_type == Constants.BUNDLE)
        {
            if (specific_policies.hasNext())
            {
                return specific_policies;
            }

            // need to look for policies from containing items
            myquery = "SELECT * from ResourcePolicy where" +
                    " resource_type_id = " + resource_type +
                    " AND action_id = " + action_id +
                    " AND container_type_id = " + Constants.ITEM +
                    " AND container_id in " +
                    "(select item_id from Item2Bundle where " +
                    " bundle_id = " + resource_id + ")";

            TableRowIterator item_policies = DatabaseManager.query(c,
                "resourcepolicy",
                myquery );

            if (item_policies.hasNext())
            {
                return item_policies;
            }

            // now look for policies from containing collections
            myquery = "SELECT * from ResourcePolicy where" +
                    " resource_type_id = " + resource_type +
                    " AND action_id = " + action_id +
                    " AND container_type_id = " + Constants.COLLECTION +
                    " AND container_id in " +
                    "(select collection_id from Collection2Item where" +
                    " item_id in " +
                    "(select item_id from Item2Bundle where " +
                    " bundle_id =" + resource_id + "))";

            TableRowIterator collection_policies = DatabaseManager.query(c,
                "resourcepolicy",
                myquery );

            if (collection_policies.hasNext())
            {
                return collection_policies;
            }
        }

        // items inherit policies from containing collections
        if (resource_type == Constants.ITEM)
        {
            if (specific_policies.hasNext())
            {
                return specific_policies;
            }

            myquery = "SELECT * from ResourcePolicy where" +
                    " resource_type_id = " + resource_type +
                    " AND action_id = " + action_id +
                    " AND container_type_id = " + Constants.COLLECTION +
                    " AND container_id in " +
                    "(select collection_id from Collection2Item where" +
                    " item_id = " + resource_id + ")";

            TableRowIterator collection_policies = DatabaseManager.query(c,
                "resourcepolicy",
                myquery );

            if (collection_policies.hasNext())
            {
                return collection_policies;
            }
        }

        // end of inheritance check - handle any other
        //  type (warning: may be an empty list of policies)

        return specific_policies;
    }
}
