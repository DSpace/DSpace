/*
 * AuthorizeManager.java
 *
 * $Id$
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
import java.util.Iterator;
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
 * ResourcePolicies now apply to single objects (such
 * as submit (ADD) permission to a collection.)
 * <p>
 * Note: If an eperson is a member of the administrator group (id 1), then
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
        if( !authorize(c, o, action, c.getCurrentUser()) )
        {
            // denied, assemble and throw exception
            int otype = o.getType();
            int oid   = o.getID();
            int userid;
            EPerson e = c.getCurrentUser();
            
            if( e == null ) { userid = 0;         }
            else            { userid = e.getID(); }
            
            AuthorizeException j = new AuthorizeException("Denied");
            j.printStackTrace();
            
            throw new AuthorizeException(
                "Authorization denied for action " +
                Constants.actionText[action]       +
                " on " + Constants.typeText[otype] +
                ":"    + oid + " by user " + userid,
                o,
                action
                );
        }
    }


    /**
     * same authorize, returns boolean for those who don't want to deal with
     *  catching exceptions.
     * @param context
     * @param object, a DSpaceObject
     * @param action
     */
    public static boolean authorizeActionBoolean(Context c, DSpaceObject o,
                                                    int a)
        throws SQLException
    {
        boolean isAuthorized = true;

        try
        {
            authorizeAction(c,o,a);
        }
        catch( AuthorizeException e )
        {
            isAuthorized = false;
        }

        return isAuthorized;
    }


    /**
     * authorize() is the authorize method that returns a boolean - always
     *  returns true if c.ignoreAuthorization is set
     *
     * @param resourcetype - found core.Constants (collection, item, etc.)
     * @param resorceidID of resource you're trying to do an authorize on
     * @param actionid - action to perform (read, write, etc)
     */
    private static boolean authorize(Context c,
            DSpaceObject o, int action, EPerson e)
        throws SQLException
    {
        int userid;

        // is authorization disabled for this context?
        if( c.ignoreAuthorization() ) return true;

        // is eperson set?  if not, userid = 0 (anonymous)
        if( e == null )
        {
            userid = 0;
        }
        else
        {
            userid = e.getID();

            // perform isadmin check since user 
            // is user part of admin group?
            if( isAdmin(c) )
            {
                return true;
            }
        }

        List policies = getPoliciesActionFilter(c, o, action);         
        Iterator i    = policies.iterator();
                
        while( i.hasNext() )
        {
            ResourcePolicy rp = (ResourcePolicy)i.next();
            
            // check policies for date validity
            if( rp.isDateValid() )
            {
                if( (rp.getEPersonID() != -1)
                    &&(rp.getEPersonID() == userid) )
                {
                    return true; // match
                }
                                
                if( (rp.getGroupID() != -1 )
                    &&( Group.isMember(c, rp.getGroupID() )) )
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

        if( e == null )
        {
            return false; // anonymous users can't be admins....
        }
        else
        {
            return Group.isMember(c,1);
        }
    }

    
    ///////////////////////////////////////////////
    // policy manipulation methods
    ///////////////////////////////////////////////


    /**
     * add a policy for an eperson
     * @param context
     * @param DSpaceObject to add policy to
     * @param actionID
     * @param Eperson who can perform the action
     */
    public static void addPolicy(Context c, DSpaceObject o, int actionID,
            EPerson e)
        throws SQLException, AuthorizeException
    {
        ResourcePolicy rp = ResourcePolicy.create(c);
        
        rp.setResource(o       );
        rp.setAction  (actionID);
        rp.setEPerson (e       );
        
        rp.update();
    }

    
    /**
     * add a policy for a group
     * @param context
     * @param DSpaceObject to add policy to
     * @param actionID
     * @param Group that can perform the action
     */
    public static void addPolicy(Context c, DSpaceObject o, int actionID,
            Group g)
        throws SQLException, AuthorizeException
    {
        ResourcePolicy rp = ResourcePolicy.create(c);
        
        rp.setResource(o       );
        rp.setAction  (actionID);
        rp.setGroup   (g       );
        
        rp.update();
    }


    /**
     * Return a List of the policies for an object
     *
     * @param context
     * @param dspace object
     */
    public static List getPolicies(Context c, DSpaceObject o)
        throws SQLException
    {
        TableRowIterator tri = DatabaseManager.query(c,
            "resourcepolicy",
            "SELECT * FROM resourcepolicy WHERE " +
            "resource_type_id=" + o.getType() + " AND " +
            "resource_id="      + o.getID()
            );
        
        List policies = new ArrayList();
        
        while( tri.hasNext() )
        {
            TableRow row = tri.next();
            
            // first check the cache (FIXME: is this right?)
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
                
        return policies;
    }


    /**
     * Return a list of policies for an object that match the action
     *
     * @param c context
     * @param o DSpaceObject policies relate to
     * @param actionID action (defined in class Constants)
     *
     */
    public static List getPoliciesActionFilter(Context c,
            DSpaceObject o, int actionID)
        throws SQLException
    {
        TableRowIterator tri = DatabaseManager.query(c,
            "resourcepolicy",
            "SELECT * FROM resourcepolicy WHERE " +
            "resource_type_id=" + o.getType() + " AND " +
            "resource_id="      + o.getID()   + " AND " +
            "action_id="        + actionID
            );
        
        List policies = new ArrayList();
        
        while( tri.hasNext() )
        {
            TableRow row = tri.next();
            
            // first check the cache (FIXME: is this right?)
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
        
        return policies;
    }


    /**
     * add policies to an object to match those from
     *  a previous object
     * @param context
     * @param DSpaceObject source of policies
     * @param DSpaceObject destination of inherited policies
     */
    public static void inheritPolicies(Context c,
                    DSpaceObject src, DSpaceObject dest )
        throws SQLException, AuthorizeException
    {
        // find all policies for the source object
        List policies = getPolicies(c, src);
        
        addPolicies(c, policies, dest);
    }                            
    
    
    /** adds List of policies to a DSpacObject.  The
    * list is copied
    *
    * @param context
    * @param List of policies
    * @param DSpaceObject to be modified
    *
    * @throws SQLException
    * @throws AuthorizeException
    */
    public static void addPolicies(Context c, List policies,
            DSpaceObject dest)
        throws SQLException, AuthorizeException
    {
        Iterator i = policies.iterator();
        
        // now add them to the destination object
        while( i.hasNext() )
        {
            ResourcePolicy srp = (ResourcePolicy)i.next();
            
            ResourcePolicy drp = ResourcePolicy.create(c);
            
            // copy over values
            drp.setResource ( dest               );
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
     * removes ALL policies for an object
     * @param context
     * @param dspace object
     */
    public static void removeAllPolicies(Context c, DSpaceObject o)
        throws SQLException
    {
        // FIXME: authorization check?
        DatabaseManager.updateQuery(c,
            "DELETE FROM resourcepolicy WHERE " +
            "resource_type_id=" + o.getType()   + " AND " +
            "resource_id="      + o.getID()   );
    }    
}
