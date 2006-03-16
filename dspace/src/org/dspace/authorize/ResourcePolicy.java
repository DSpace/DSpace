/*
 * ResourcePolicy.java
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
import java.util.Date;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Class representing a ResourcePolicy
 * 
 * @author David Stuve
 * @version $Revision$
 */
public class ResourcePolicy
{
    /** Our context */
    private Context myContext;

    /** The row in the table representing this object */
    private TableRow myRow;

    /**
     * Construct an ResourcePolicy
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    ResourcePolicy(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
    }

    /**
     * Get an ResourcePolicy from the database.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the ResourcePolicy
     * 
     * @return the ResourcePolicy format, or null if the ID is invalid.
     */
    public static ResourcePolicy find(Context context, int id)
            throws SQLException
    {
        TableRow row = DatabaseManager.find(context, "ResourcePolicy", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new ResourcePolicy(context, row);
        }
    }

    /**
     * Create a new ResourcePolicy
     * 
     * @param context
     *            DSpace context object
     */
    public static ResourcePolicy create(Context context) throws SQLException,
            AuthorizeException
    {
        // FIXME: Check authorisation
        // Create a table row
        TableRow row = DatabaseManager.create(context, "ResourcePolicy");

        return new ResourcePolicy(context, row);
    }

    /**
     * Delete an ResourcePolicy
     *  
     */
    public void delete() throws SQLException
    {
        // FIXME: authorizations
        // Remove ourself
        DatabaseManager.delete(myContext, myRow);
    }

    /**
     * Get the e-person's internal identifier
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("policy_id");
    }

    /**
     * Get the type of the objects referred to by policy
     * 
     * @return type of object/resource
     */
    public int getResourceType()
    {
        return myRow.getIntColumn("resource_type_id");
    }

    /**
     * set both type and id of resource referred to by policy
     *  
     */
    public void setResource(DSpaceObject o)
    {
        setResourceType(o.getType());
        setResourceID(o.getID());
    }

    /**
     * Set the type of the resource referred to by the policy
     * 
     * @param mytype
     *            type of the resource
     */
    public void setResourceType(int mytype)
    {
        myRow.setColumn("resource_type_id", mytype);
    }

    /**
     * Get the ID of a resource pointed to by the policy (is null if policy
     * doesn't apply to a single resource.)
     * 
     * @return resource_id
     */
    public int getResourceID()
    {
        return myRow.getIntColumn("resource_id");
    }

    /**
     * If the policy refers to a single resource, this is the ID of that
     * resource.
     * 
     * @param myid   id of resource (database primary key)
     */
    public void setResourceID(int myid)
    {
        myRow.setColumn("resource_id", myid);
    }

    /**
     * @return get the action this policy authorizes
     */
    public int getAction()
    {
        return myRow.getIntColumn("action_id");
    }

    /**
     * @return action text or 'null' if action row empty
     */
    public String getActionText()
    {
        int myAction = myRow.getIntColumn("action_id");

        if (myAction == -1)
        {
            return "...";
        }
        else
        {
            return Constants.actionText[myAction];
        }
    }

    /**
     * set the action this policy authorizes
     * 
     * @param myid  action ID from <code>org.dspace.core.Constants</code>
     */
    public void setAction(int myid)
    {
        myRow.setColumn("action_id", myid);
    }

    /**
     * @return eperson ID, or -1 if EPerson not set
     */
    public int getEPersonID()
    {
        return myRow.getIntColumn("eperson_id");
    }

    /**
     * get EPerson this policy relates to
     * 
     * @return EPerson, or null
     */
    public EPerson getEPerson() throws SQLException
    {
        int eid = myRow.getIntColumn("eperson_id");

        if (eid == -1)
        {
            return null;
        }

        return EPerson.find(myContext, eid);
    }

    /**
     * assign an EPerson to this policy
     * 
     * @param e EPerson
     */
    public void setEPerson(EPerson e)
    {
        if (e != null)
        {
            myRow.setColumn("eperson_id", e.getID());
        }
        else
        {
            myRow.setColumnNull("eperson_id");
        }
    }

    /**
     * gets ID for Group referred to by this policy
     * 
     * @return groupID, or -1 if no group set
     */
    public int getGroupID()
    {
        return myRow.getIntColumn("epersongroup_id");
    }

    /**
     * gets Group for this policy
     * 
     * @return Group, or -1 if no group set
     */
    public Group getGroup() throws SQLException
    {
        int gid = myRow.getIntColumn("epersongroup_id");

        if (gid == -1)
        {
            return null;
        }
        else
        {
            return Group.find(myContext, gid);
        }
    }

    /**
     * set Group for this policy
     * 
     * @param g group
     */
    public void setGroup(Group g)
    {
        if (g != null)
        {
            myRow.setColumn("epersongroup_id", g.getID());
        }
        else
        {
            myRow.setColumnNull("epersongroup_id");
        }
    }

    /**
     * figures out if the date is valid for the policy
     * 
     * @return true if policy has begun and hasn't expired yet (or no dates are
     *         set)
     */
    public boolean isDateValid()
    {
        Date sd = getStartDate();
        Date ed = getEndDate();

        // if no dates set, return true (most common case)
        if ((sd == null) && (ed == null))
        {
            return true;
        }

        // one is set, now need to do some date math
        Date now = new Date();

        // check start date first
        if (sd != null)
        {
            // start date is set, return false if we're before it
            if (now.before(sd))
            {
                return false;
            }
        }

        // now expiration date
        if (ed != null)
        {
            // end date is set, return false if we're after it
            if (now.after(sd))
            {
                return false;
            }
        }

        // if we made it this far, start < now < end
        return true; // date must be okay
    }

    /**
     * Get the start date of the policy
     * 
     * @return start date, or null if there is no start date set (probably most
     *         common case)
     */
    public java.util.Date getStartDate()
    {
        return myRow.getDateColumn("start_date");
    }

    /**
     * Set the start date for the policy
     * 
     * @param d
     *            date, or null for no start date
     */
    public void setStartDate(java.util.Date d)
    {
        myRow.setColumn("start_date", d);
    }

    /**
     * Get end date for the policy
     * 
     * @return end date or null for no end date
     */
    public java.util.Date getEndDate()
    {
        return myRow.getDateColumn("end_date");
    }

    /**
     * Set end date for the policy
     * 
     * @param d
     *            end date, or null
     */
    public void setEndDate(java.util.Date d)
    {
        myRow.setColumn("end_date", d);
    }

    /**
     * Update the ResourcePolicy
     */
    public void update() throws SQLException
    {
        // FIXME: Check authorisation
        DatabaseManager.update(myContext, myRow);
    }
}
