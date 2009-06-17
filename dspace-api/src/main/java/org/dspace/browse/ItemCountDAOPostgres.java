/*
 * ItemCountDAOPostgres.java
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.browse;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.DatabaseManager;

import java.sql.SQLException;

/**
 * Postgres driver implementing ItemCountDAO interface to cache item
 * count information in communities and collections
 * 
 * @author Richard Jones
 *
 */
public class ItemCountDAOPostgres implements ItemCountDAO
{
	/** Log4j logger	 */
	private static Logger log = Logger.getLogger(ItemCountDAOPostgres.class);
	
	/** DSpace context */
	private Context context;
	
	/** SQL to select on a collection id */
	private String collectionSelect = "SELECT * FROM collection_item_count WHERE collection_id = ?";
	
	/** SQL to insert a new collection record */
	private String collectionInsert = "INSERT INTO collection_item_count (collection_id, count) VALUES (?, ?)";
	
	/** SQL to update an existing collection record */
	private String collectionUpdate = "UPDATE collection_item_count SET count = ? WHERE collection_id = ?";
	
	/** SQL to remove a collection record */
	private String collectionRemove = "DELETE FROM collection_item_count WHERE collection_id = ?";
	
	/** SQL to select on a community id */
	private String communitySelect = "SELECT * FROM community_item_count WHERE community_id = ?";
	
	/** SQL to insert a new community record */
	private String communityInsert = "INSERT INTO community_item_count (community_id, count) VALUES (?, ?)";
	
	/** SQL to update an existing community record */
	private String communityUpdate = "UPDATE community_item_count SET count = ? WHERE community_id = ?";
	
	/** SQL to remove a community record */
	private String communityRemove = "DELETE FROM community_item_count WHERE community_id = ?";
	
	/**
	 * Store the count of the given collection
	 * 
	 * @param collection
	 * @param count
	 * @throws ItemCountException
	 */
	public void collectionCount(Collection collection, int count) 
		throws ItemCountException
	{
        TableRowIterator tri = null;
        try
		{
			// first find out if we have a record
			Object[] sparams = { new Integer(collection.getID()) };
			tri = DatabaseManager.query(context, collectionSelect, sparams);
			
			if (tri.hasNext())
			{
				Object[] params = { new Integer(count), new Integer(collection.getID()) };
				DatabaseManager.updateQuery(context, collectionUpdate, params);
			}
			else
			{
				Object[] params = { new Integer(collection.getID()), new Integer(count) };
				DatabaseManager.updateQuery(context, collectionInsert, params);
			}
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
        finally
        {
            if (tri != null)
                tri.close();
        }
    }

	/**
	 * Store the count of the given community
	 * 
	 * @param community
	 * @param count
	 * @throws ItemCountException
	 */
	public void communityCount(Community community, int count) 
		throws ItemCountException
	{
        TableRowIterator tri = null;
        try
		{
			// first find out if we have a record
			Object[] sparams = { new Integer(community.getID()) };
			tri = DatabaseManager.query(context, communitySelect, sparams);
			
			if (tri.hasNext())
			{
				Object[] params = { new Integer(count), new Integer(community.getID()) };
				DatabaseManager.updateQuery(context, communityUpdate, params);
			}
			else
			{
				Object[] params = { new Integer(community.getID()), new Integer(count) };
				DatabaseManager.updateQuery(context, communityInsert, params);
			}
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
        finally
        {
            if (tri != null)
                tri.close();
        }
    }

	/**
	 * Set the dspace context to use
	 * 
	 * @param context
	 * @throws ItemCountException
	 */
	public void setContext(Context context) 
		throws ItemCountException
	{
		this.context = context;
	}

	/**
	 * get the count of the items in the given container
	 * 
	 * @param dso
	 * @return
	 * @throws ItemCountException
	 */
	public int getCount(DSpaceObject dso) 
		throws ItemCountException
	{
		if (dso instanceof Collection)
		{
			return getCollectionCount((Collection) dso);
		}
		else if (dso instanceof Community)
		{
			return getCommunityCount((Community) dso);
		}
		else
		{
			throw new ItemCountException("We can only count items in Communities or Collections");
		}
	}

	/**
	 * remove the cache for the given container
	 * 
	 * @param dso
	 * @throws ItemCountException
	 */
	public void remove(DSpaceObject dso) throws ItemCountException
	{
		if (dso instanceof Collection)
		{
			removeCollection((Collection) dso);
		}
		else if (dso instanceof Community)
		{
			removeCommunity((Community) dso);
		}
		else
		{
			throw new ItemCountException("We can only delete count of items from Communities or Collections");
		}
	}

	/**
	 * remove the cache for the given collection 
	 * 
	 * @param collection
	 * @throws ItemCountException
	 */
	private void removeCollection(Collection collection)
		throws ItemCountException
	{
		try
		{
			Object[] params = { new Integer(collection.getID()) };
			DatabaseManager.updateQuery(context, collectionRemove, params);
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
	}
	
	/**
	 * Remove the cache for the given community
	 * 
	 * @param community
	 * @throws ItemCountException
	 */
	private void removeCommunity(Community community)
		throws ItemCountException
	{
		try
		{
			Object[] params = { new Integer(community.getID()) };
			DatabaseManager.updateQuery(context, communityRemove, params);
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
	}
	
	/**
	 * Get the count for the given collection
	 * 
	 * @param collection
	 * @return
	 * @throws ItemCountException
	 */
	private int getCollectionCount(Collection collection)
		throws ItemCountException
	{
        TableRowIterator tri = null;
        try
		{
			Object[] params = { new Integer(collection.getID()) };
			tri = DatabaseManager.query(context, collectionSelect, params);
			
			if (!tri.hasNext())
			{
				return 0;
			}
			
			TableRow tr = tri.next();
			
			if (tri.hasNext())
			{
				throw new ItemCountException("More than one count row in the database");
			}

			return tr.getIntColumn("count");
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
        finally
        {
            if (tri != null)
                tri.close();
        }
    }
	
	/**
	 * get the count for the given community
	 * 
	 * @param community
	 * @return
	 * @throws ItemCountException
	 */
	private int getCommunityCount(Community community)
		throws ItemCountException
	{
        TableRowIterator tri = null;
        try
		{
			Object[] params = { new Integer(community.getID()) };
			tri = DatabaseManager.query(context, communitySelect, params);
			
			if (!tri.hasNext())
			{
				return 0;
			}
			
			TableRow tr = tri.next();
			
			if (tri.hasNext())
			{
				throw new ItemCountException("More than one count row in the database");
			}

			return tr.getIntColumn("count");
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
        finally
        {
            if (tri != null)
                tri.close();
        }
    }
}
