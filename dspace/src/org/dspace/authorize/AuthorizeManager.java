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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
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
 * Note: If an eperson is a member of the administrator group (id 0), then
 *  they are automatically given permission for all requests.
 */





public class AuthorizeManager
{
    /**
     * primary authorization interface, assumes user is the context's
     *  currentuser, and throws an exception
     *
     * @param context
     * @param object, a DSpaceObject
     * @param action
     *
     * @throws AuthorizeException if the user is denied
     */
    public static void authorizeAction(Context c, DSpaceObject o, int action)
        throws AuthorizeException, SQLException
    {
        int otype = o.getType();
        int oid   = o.getID();
    
        if( !authorize(c, otype, oid, action, c.getCurrentUser()) )
        {
            int userid = c.getCurrentUser().getID();
            AuthorizeException j = new AuthorizeException("test");
            j.printStackTrace();
            
            throw new AuthorizeException(
                "Authorization denied for action " +
                Constants.actiontext[action]       +
                " on " + Constants.typetext[otype] +
                ":"    + oid + " by user " + userid,
                o,
                action
                );
        }
    }


    /**
     * same authorize, returns boolean for those who don't want to deal with
     *  catching exceptions.
     */
    public static boolean authorizeActionBoolean(Context c, DSpaceObject o, int a)
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
     * admin is group 0
     */
    public static boolean isAdmin(Context c,int userid)
        throws SQLException
    {
        // group is hardcoded as 'admin', and admins can do everything
        if( Group.isMember(c,0,userid) )
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
    
    /**
     * add a policy for an eperson
     */
    public static void addPolicy(Context c, DSpaceObject o, int action,
            EPerson e)
        throws SQLException, AuthorizeException
    {
        ResourcePolicy rp = ResourcePolicy.create(c);
        
        rp.setResource(o);
        rp.setAction(action);
        rp.setEPerson(e);
        
        rp.update();
    }

    
    /**
     * add a policy for a group
     */
    public static void addPolicy(Context c, DSpaceObject o, int action, Group g)
        throws SQLException, AuthorizeException
    {
        ResourcePolicy rp = ResourcePolicy.create(c);
        
        rp.setResource(o);
        rp.setAction(action);
        rp.setGroup(g);
        
        rp.update();
    }


    public static ResourcePolicy [] getPolicies(Context c, DSpaceObject o)
        throws SQLException
    {
        TableRowIterator tri = getAllObjectPolicies(c, o);
        
        List policies = new ArrayList();
        
        while( tri.hasNext() )
        {
            TableRow row = tri.next();
            
            // first check the cache - what does this mean?
            ResourcePolicy cachepolicy = (ResourcePolicy)c.fromCache(
                ResourcePolicy.class, row.getIntColumn("policy_id") );
            
            if( cachepolicy != null )
            {
                policies.add(cachepolicy);
            }
            else
            {
                policies.add(new ResourcePolicy(c, row));
            }
        }
        
        ResourcePolicy[] policyArray = new ResourcePolicy[policies.size()];
        
        policyArray = (ResourcePolicy[])policies.toArray(policyArray);
        
        return policyArray;
    }
    


    /**
     * get all of the policies for an object
     */
    public static TableRowIterator getAllObjectPolicies(Context c, DSpaceObject o)
        throws SQLException
    {
        TableRowIterator tri = DatabaseManager.query(c,
            "resourcepolicy",
            "SELECT * FROM resourcepolicy WHERE " +
            "resource_type_id=" + o.getType() + " AND " +
            "resource_id="      + o.getID()
            );
    
        return tri;
    }


    /**
     * add policies to an object to match those from
     *  a previous object
     */
    public static void inheritPolicies(Context c, DSpaceObject src,
                                        DSpaceObject dest )
        throws SQLException, AuthorizeException
    {
        // find all policies for the source object
        TableRowIterator tri = getAllObjectPolicies(c, src);
        
        addPolicies(c, tri, dest);
    }                            
    
    
    public static void addPolicies(Context c, TableRowIterator src,
            DSpaceObject dest)
        throws SQLException, AuthorizeException
    {
        // now add them to the destination object
        while( src.hasNext() )
        {
            TableRow row = src.next();
            
            ResourcePolicy srp = new ResourcePolicy(c, row);
            ResourcePolicy drp = ResourcePolicy.create(c);
            
            // copy over values
            drp.setResource (dest);
            drp.setPublic   ( srp.isPublic()     );
            drp.setAction   ( srp.getAction()    );
            drp.setEPerson  ( srp.getEPerson()   );
            drp.setGroup    ( srp.getGroup()     );
            drp.setStartDate( srp.getStartDate() );
            drp.setEndDate  ( srp.getEndDate()   );
            
            // and write out new policy
            drp.update();
        }
    }
    

    /**
     * removes all policies for an object
     */
    public static void removeAllPolicies(Context c, DSpaceObject o)
        throws SQLException
    {
        DatabaseManager.updateQuery(c,
            "DELETE FROM resourcepolicy WHERE " +
            "resource_type_id=" + o.getType()   + " AND " +
            "resource_id="      + o.getID()   );
    }


    /**
     * removes an eperson from all policies to do with an object
     */
/*    public static void removeAllPolicies(Context c, DSpaceObject o, EPerson e)
        throws SQLException
    {
        DatabaseManager.updateQuery(c,
            "DELETE FROM resourcepolicy WHERE "         +
            "resource_type_id=" + o.getType() + " AND " +
            "resource_id="      + o.getID()   + " AND " +
            "eperson_id="       + e.getID() );
    }
*/    
    
    /**
     * removes a group from all policies to do with an object
     */
/*    public static void removePolicies(Context c, DSpaceObject o, Group g)
        throws SQLException
    {
        DatabaseManager.updateQuery(c,
            "DELETE FROM resourcepolicy WHERE "         +
            "resource_type_id=" + o.getType() + " AND " +
            "resource_id="      + o.getID()   + " AND " +
            "epersongroup_id="  + g.getID() );
    }
*/    

    /**
     * remove policies
     *
     * @param object or null
     * @param action or -1 for all
     * @param EPerson or null
     */
/*    public static void removePolicy(Context c, DSpaceObject o, int action,
                            EPerson e )
    {
        // if object is set, delete policies for that object
        if( o != null )
        {
            if( e != null )
            {
                // remove a policy for a single eperson
            }
            else
            {
                // remove policy for all epeople in object
            }            
        }
        
    }
*/

    public static boolean authorize(Context c, int object_type, int object_id,
            int action, EPerson e)
        throws SQLException
    {
        // is authorization disabled for this context?
        if( c.ignoreAuthorization() ) return true;

        // is user part of admin group?
        if( isAdmin( c,e.getID() ) ) return true;

        // get policies for this object and action
        TableRowIterator tri = DatabaseManager.query(c,
            "resourcepolicy",
            "SELECT * FROM resourcepolicy WHERE " +
            "resource_type_id=" + object_type     + " AND " +
            "resource_id="      + object_id       + " AND " +
            "action_id="        + action
            );
        
        while( tri.hasNext() )
        {
            TableRow row = (TableRow)tri.next();
            ResourcePolicy rp = new ResourcePolicy(c, row);
            
            // check policies for date validity
            if( rp.isDateValid() )
            {
                // if public flag is set, action is authorize
                //  for everyone (even anonymous use)
                if( rp.isPublic() ) { return true; }
            
                if( (rp.getEPersonID() != -1)
                    &&(rp.getEPersonID() == e.getID()) )
                {
                    return true; // match
                }
                                
                if( (rp.getGroupID() != -1 )
                    &&(Group.isMember(c, rp.getGroupID(), e.getID())) )
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

/* old authorize() - ready to delete yet?

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
//            System.out.println("Error - no policies for this object " +
//                Constants.typetext[resourcetype] + ":" +
//                Constants.actiontext[actionid]);
        }

        while( i.hasNext() )
        {
            TableRow row = (TableRow)i.next();

            ResourcePolicy rp = new ResourcePolicy(c, row);

//            String policymessage = Constants.typetext[resourcetype] + ":" +
//                resourceid + ":" + Constants.actiontext[actionid]   + ":" +
//                rp.getPolicy();
            
//            System.out.println( policymessage );

            // evaluate each statement
            StringTokenizer st = new StringTokenizer(rp.getPolicy(), ",");

            while (st.hasMoreTokens())
            {
                String t = st.nextToken();

//                System.out.println("Token: " + t);
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
//                        System.out.println("number format exception" + e);
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
//                        System.out.println("numero format exception" + e);
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
*/

}
