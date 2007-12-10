/*
 * SubscriptionDAO.java
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
package org.dspace.eperson.dao.postgres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.content.Collection;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 */
public class SubscriptionDAOPostgres extends SubscriptionDAO
{
    public SubscriptionDAOPostgres(Context context)
    {
        this.context = context;
    }

    @Override
    public Subscription create()
    {
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context, "subscription");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("subscription_id");
            
            return new Subscription(id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Subscription retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "subscription", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Subscription retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "subscription", "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(Subscription subscription)
    {
        try
        {
            int id = subscription.getID();
            TableRow row = DatabaseManager.find(context, "subscription", id);

            if (row != null)
            {
                int epersonID = subscription.getEPersonID();
                int collectionID = subscription.getCollectionID();

                row.setColumn("eperson_id", epersonID);
                row.setColumn("collection_id", collectionID);
                DatabaseManager.update(context, row);
            }
            else
            {
                throw new RuntimeException("Didn't find subscription " + id);
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
        try
        {
            DatabaseManager.delete(context, "subscription", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public boolean isSubscribed(EPerson eperson, Collection collection)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT subscription_id FROM subscription " +
                    "WHERE eperson_id = ? AND collection_id = ?", 
                    eperson.getID(), collection.getID());
            
            boolean result = tri.hasNext();
            tri.close();
            
            return result;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Subscription> getSubscriptions()
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT subscription_id FROM subscription " +
                    "ORDER BY eperson_id");

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<Subscription> getSubscriptions(EPerson eperson)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.query(context,
                    "SELECT subscription_id FROM subscription " +
                    "WHERE eperson_id = ?", eperson.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private Subscription retrieve(TableRow row) throws SQLException
    {
        if (row == null)
        {
            return null;
        }
        else
        {
            int id = row.getIntColumn("subscription_id");
            Subscription sub = new Subscription(id);

            sub.setEPersonID(row.getIntColumn("eperson_id"));
            sub.setCollectionID(row.getIntColumn("collection_id"));

            return sub;
        }
    }

    private List<Subscription> returnAsList(TableRowIterator tri)
        throws SQLException
    {
        List<Subscription> subscriptions = new ArrayList<Subscription>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("subscription_id");
            subscriptions.add(retrieve(id));
        }

        return subscriptions;
    }
}
