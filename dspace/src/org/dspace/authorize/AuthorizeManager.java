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

//import org.dspace.db.*;
//import org.dspace.db.generated.*;
//import org.dspace.util.beans.BeanUtils;

import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.DSpaceTypes;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.util.StringTokenizer;

/**
 */

public class AuthorizeManager
{
    public static void authorizeAction(Context c,Object o, int a, EPerson e)
        throws java.sql.SQLException, AuthorizeException
    {
        // initialize so that -1s get passed on unknown objects
        int otype	= -1;
        int oid 	= -1;
        int userid	= -1;

        // figure out eperson id
        userid = e.getID();

        // now figure out the type and object id
        if( o instanceof Item )
        {
            otype = DSpaceTypes.ITEM;
            oid   = ((Item) o).getID();
        }
        else if( o instanceof Bitstream )
        {
            otype = DSpaceTypes.BITSTREAM;
            oid   = ((Bitstream) o).getID();
        }		
        else if( o instanceof Collection )
        {
            otype = DSpaceTypes.COLLECTION;
            oid   = ((Collection) o).getID();
        }		
        else if( o instanceof Bundle )
        {
            otype = DSpaceTypes.BUNDLE;
            oid   = ((Bundle) o).getID();
        }
        else
        {
            throw new AuthorizeException("Unknown object type");
        }
		
        authorizeAction( c,otype, oid, a, userid );
    }

    /**
     * authorizeAction()is the authorize method that throws an AuthorizeException
	 *
     * @param resourcetype	constant from dspacetypes (collection, item, etc.)
     * @param resorceidID	of resource you're trying to do an authorize on
     * @param actionid		action to perform (read, write, etc) DSpaceActions
     * @param userid		who wants to perform the action?
     * @throws AuthorizeException
     */
    public static void authorizeAction(Context c,int resourcetype, int resourceid, int actionid, int userid)
        throws java.sql.SQLException, AuthorizeException
    {
        if (!authorize(c,resourcetype, resourceid, actionid, userid))
            throw new AuthorizeException("Authorization denied for action " + actionid + " by user " + userid);

    }

    /**
     * authorize() is the authorize method that returns a boolean
	 *
     * @param resourcetype - found inspacetypes (collection, item, etc.)
     * @param resorceidID of resource you're trying to do an authorize on
     * @param actionid - action to perform (read, write, etc)
     * @param userid - who wants to perform the action?
     */
    public static boolean authorize(Context c,int resourcetype, int resourceid, int actionid, int userid)
        throws java.sql.SQLException
    {
   
        // group is hardcoded as 'admin', and admins can do everything
        if (Group.isMember(c,-1, userid)) return true;
      
        TableRowIterator i = policyLookup(c,resourcetype, resourceid, actionid);

        // no policies?  notify admins and give 'false'
        if (!i.hasNext())
        {//alert( "no policies for this object" );
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
                    {// eek! this should never happen
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
                    {// once again, eek!
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
	 *
	 */
	 
    static TableRowIterator policyLookup(Context c, int resource_type, int resource_id, int action_id)
        throws java.sql.SQLException
    {
        String myquery = "";
        
        // get the policies eonly for this object (rare)
        TableRowIterator specific_policies = DatabaseManager.query(c,
            "resourcepolicy",
            "SELECT resourcepolicy.* FROM resourcepolicy WHERE" +
            " resource_type_id=" + resource_type +
            " AND action_id=" + action_id +
            " AND resource_id=" + resource_id );

/*
        // find all policies specific to this object
        String myquery = "SELECT * from ResourcePolicy where" +
            " resource_type_id = " + resource_type +
            " AND action_id = " + action_id +
            " AND resource_id = " + resource_id;



        DatabaseBeanIterator specific_policies = ResourcePolicy.query(myquery);
        DatabaseBeanIterator item_policies = null;
        DatabaseBeanIterator collection_policies = null;
*/
        if (resource_type == DSpaceTypes.BITSTREAM)
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
                    " AND resource_filter = " + DSpaceTypes.ITEM +
                    " AND resource_filter_arg in " +
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
                    " AND resource_filter = " + DSpaceTypes.COLLECTION +
                    " AND resource_filter_arg in " +
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
        if (resource_type == DSpaceTypes.BUNDLE)
        {
            if (specific_policies.hasNext())
            {
                return specific_policies;
            }

            // need to look for policies from containing items
            myquery = "SELECT * from ResourcePolicy where" +
                    " resource_type_id = " + resource_type +
                    " AND action_id = " + action_id +
                    " AND resource_filter = " + DSpaceTypes.ITEM +
                    " AND resource_filter_arg in " +
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
                    " AND resource_filter = " + DSpaceTypes.COLLECTION +
                    " AND resource_filter_arg in " +
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

        // items just inherit from collections
        if (resource_type == DSpaceTypes.ITEM)
        {
            if (specific_policies.hasNext())
            {
                return specific_policies;
            }

            myquery = "SELECT * from ResourcePolicy where" +
                    " resource_type_id = " + resource_type +
                    " AND action_id = " + action_id +
                    " AND resource_filter = " + DSpaceTypes.COLLECTION +
                    " AND resource_filter_arg in " +
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

        // end of special inheritance - handle any other
        //  type

        return specific_policies;
    }
}
