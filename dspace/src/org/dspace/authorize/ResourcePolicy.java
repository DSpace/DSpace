/*
 * ResourcePolicy
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

import org.dspace.core.Context;

import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
//import org.dspace.authorize.AuthorizeException;

import java.sql.SQLException;


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
     * @param context  the context this object exists in
     * @param row      the corresponding row in the table
     */
    ResourcePolicy( Context context, TableRow row )
    {
        myContext = context;
        myRow = row;
    }

    /**
     * Get an ResourcePolicy from the database.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the ResourcePolicy
     *   
     * @return  the ResourcePolicy format, or null if the ID is invalid.
     */
    public static ResourcePolicy find(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.find( context, "ResourcePolicy", id );

        if ( row == null )
        {
            return null;
        }
        else
        {
            return new ResourcePolicy( context, row );
        }
    }


    /**
     * Create a new ResourcePolicy
     *
     * @param  context  DSpace context object
     */
    public static ResourcePolicy create(Context context)
        throws SQLException, AuthorizeException
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
    public void delete()
        throws SQLException
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
     * Get the ResourcePolicy's policy statement.
     *
     * @return statement
     */
    public String getPolicy()
    {
        return myRow.getStringColumn("policy_statement");
    }


    /**
     * Set the ResourcePolicy's policy statement
     *
     * @param statement the policy statement
     */
    public void setPolicy(String policy)
    {
        myRow.setColumn("policy_statement", policy);
    }


// resource_type_id, resource_id, resource_filter, resource_filter_arg
// action_id, policy_statement, priority, notes, owner_eperson_id
// doing nothing for priority, notes, owner_eperson_id

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
     * Set the type of the resource referred to by the policy
     *
     * @param mytype type of the resource 
     */

    public void setResourceType( int mytype )
    {
        myRow.setColumn("resource_type_id", mytype );
    }

    /**
     * Get the ID of a resource pointed to by the policy
     *  (is null if policy doesn't apply to a single resource.)
     *
     * @return resource_id 
     */

    public int getResourceID()
    {
        return myRow.getIntColumn("resource_id");
    }

    /**
     * If the policy refers to a single resource, this
     *  is the ID of that resource.
     *
     * @param resource_id
     */

    public void setResourceID( int myid )
    {
        myRow.setColumn("resource_id", myid);
    }

    /**
     * Get the type of the container, if the policy refers to
     * a container full of objects. 
     * 
     */

    public int getContainerType()
    {
        return myRow.getIntColumn("resource_filter");
    }

    /**
     * Set the type of the container, if policy refers
     * to a container full of objects
     *
     * @param container type
     */

    public void setContainerType( int mytype )
    {
        myRow.setColumn("resource_id" ,mytype);
    }
    
    /**
     * Get the ID of the container
     *
     * @return container ID 
     */

    public int getContainerID()
    {
        return myRow.getIntColumn("resource_filter_arg");
    }

    /**
     * Set the ID of the container, if the policy refers to
     *  the contents of a container.  Combined with container type,
     *  you can determine exactly what the container is.
     *
     * @param myid container id
     */

    public void setContainerID( int myid )
    {
        myRow.setColumn("resource_filter_arg" ,myid);
    }


    /**
     * get the action this policy authorizes
     */

    public int getActionID()
    {
        return myRow.getIntColumn("action_id");
    }


    /**
     * set the action this policy authorizes
     *
     * @param id
     */

    public void setActionID( int myid )
    {
        myRow.setColumn("action_id",myid);
    }


    /**
     * Update the ResourcePolicy
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation

        DatabaseManager.update(myContext, myRow);
    }
}
