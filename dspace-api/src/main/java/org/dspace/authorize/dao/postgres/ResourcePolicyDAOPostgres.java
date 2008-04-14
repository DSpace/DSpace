/*
 * ResourcePolicyDAOPostgres.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.authorize.dao.postgres;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.uri.ObjectIdentifierService;
import org.dspace.uri.SimpleIdentifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author James Rutherford
 */
public class ResourcePolicyDAOPostgres extends ResourcePolicyDAO
{
    public ResourcePolicyDAOPostgres(Context context)
    {
        this.context = context;
    }

    @Override
    public ResourcePolicy create()
    {
        try
        {
            SimpleIdentifier sid = ObjectIdentifierService.mintSimple();
            TableRow row = DatabaseManager.create(context, "resourcepolicy");
            row.setColumn("uuid", sid.getUUID().toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("policy_id");
            ResourcePolicy rp = new ResourcePolicy(context, id);
            rp.setSimpleIdentifier(sid);

            return rp;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public ResourcePolicy retrieve(int id)
    {
        ResourcePolicy rp = super.retrieve(id);

        if (rp != null)
        {
            return rp;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "resourcepolicy", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public ResourcePolicy retrieve(UUID uuid)
    {
        ResourcePolicy rp = super.retrieve(uuid);

        if (rp != null)
        {
            return rp;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "resourcepolicy", "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(ResourcePolicy rp)
    {
        try
        {
            TableRow row =
                DatabaseManager.find(context, "resourcepolicy", rp.getID());

            if (row != null)
            {
                populateTableRowFromResourcePolicy(rp, row);
                DatabaseManager.update(context, row);
            }
            else
            {
                throw new RuntimeException("Didn't find resource policy " +
                        rp.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id)
    {
        super.delete(id);

        try
        {
            DatabaseManager.delete(context, "resourcepolicy", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(");
        }
    }

    @Override
    public List<ResourcePolicy> getPolicies(DSpaceObject dso)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "resourcepolicy",
                    "SELECT policy_id FROM resourcepolicy " +
                    "WHERE resource_type_id = ? AND resource_id = ? ",
                    dso.getType(), dso.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(");
        }
    }

    @Override
    public List<ResourcePolicy> getPolicies(Group group)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "resourcepolicy",
                    "SELECT policy_id FROM resourcepolicy " +
                    "WHERE epersongroup_id = ?",
                    group.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(");
        }
    }

    @Override
    public List<ResourcePolicy> getPolicies(DSpaceObject dso, Group group)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "resourcepolicy",
                    "SELECT policy_id FROM resourcepolicy " +
                    "WHERE resource_type_id = ? AND resource_id = ? " +
                    "AND epersongroup_id = ? ",
                    dso.getType(), dso.getID(), group.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(");
        }
    }

    @Override
    public List<ResourcePolicy> getPolicies(DSpaceObject dso, int actionID)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "resourcepolicy",
                    "SELECT policy_id FROM resourcepolicy " +
                    "WHERE resource_type_id = ? AND resource_id = ? " +
                    "AND action_id = ? ",
                    dso.getType(), dso.getID(), actionID);

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(":(");
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private ResourcePolicy retrieve(TableRow row)
    {
        if (row == null)
        {
            return null;
        }

        int id = row.getIntColumn("policy_id");
        ResourcePolicy rp = new ResourcePolicy(context, id);
        populateResourcePolicyFromTableRow(rp, row);

        return rp;
    }

    private List<ResourcePolicy> returnAsList(TableRowIterator tri)
            throws SQLException
    {
        List<ResourcePolicy> policies = new ArrayList<ResourcePolicy>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("policy_id");
            policies.add(retrieve(id));
        }

        return policies;
    }

    private void populateResourcePolicyFromTableRow(ResourcePolicy rp,
            TableRow row)
    {
        UUID uuid = UUID.fromString(row.getStringColumn("uuid"));
        int resourceID = row.getIntColumn("resource_id");
        int resourceTypeID = row.getIntColumn("resource_type_id");
        int actionID = row.getIntColumn("action_id");
        int epersonID = row.getIntColumn("eperson_id");
        int groupID = row.getIntColumn("epersongroup_id");
        Date startDate = row.getDateColumn("start_date");
        Date endDate = row.getDateColumn("end_date");

        rp.setSimpleIdentifier(new SimpleIdentifier(uuid));
        rp.setResourceID(resourceID);
        rp.setResourceType(resourceTypeID);
        rp.setAction(actionID);
        rp.setEPersonID(epersonID);
        rp.setGroupID(groupID);
        rp.setStartDate(startDate);
        rp.setEndDate(endDate);
    }

    private void populateTableRowFromResourcePolicy(ResourcePolicy rp,
            TableRow row)
    {
        int resourceID = rp.getResourceID();
        int resourceTypeID = rp.getResourceType();
        int actionID = rp.getAction();
        int epersonID = rp.getEPersonID();
        int groupID = rp.getGroupID();
        Date startDate = rp.getStartDate();
        Date endDate = rp.getEndDate();

        // FIXME This would be much cleaner with a ResourcePolicyMetadata enum
        if (resourceID > 0)
        {
            row.setColumn("resource_id", resourceID);
        }
        else
        {
            row.setColumnNull("resource_id");
        }
        if (resourceTypeID >= 0)
        {
            row.setColumn("resource_type_id", resourceTypeID);
        }
        else
        {
            row.setColumnNull("resource_type_id");
        }
        if (actionID >= 0)
        {
            row.setColumn("action_id", actionID);
        }
        else
        {
            row.setColumnNull("action_id");
        }
        if (epersonID > 0)
        {
            row.setColumn("eperson_id", epersonID);
        }
        else
        {
            row.setColumnNull("eperson_id");
        }
        if (groupID >= 0)
        {
            row.setColumn("epersongroup_id", groupID);
        }
        else
        {
            row.setColumnNull("epersongroup_id");
        }
        if (startDate != null)
        {
            row.setColumn("start_date", startDate);
        }
        else
        {
            row.setColumnNull("start_date");
        }
        if (endDate != null)
        {
            row.setColumn("end_date", endDate);
        }
        else
        {
            row.setColumnNull("end_date");
        }
    }
}
