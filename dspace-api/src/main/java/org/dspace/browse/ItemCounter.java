/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.apache.log4j.Logger;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;

import java.sql.SQLException;

/**
 * This class provides a standard interface to all item counting
 * operations for communities and collections.  It can be run from the
 * command line to prepare the cached data if desired, simply by
 * running:
 * 
 * java org.dspace.browse.ItemCounter
 * 
 * It can also be invoked via its standard API.  In the event that
 * the data cache is not being used, this class will return direct
 * real time counts of content.
 * 
 * @author Richard Jones
 *
 */
public class ItemCounter
{
	/** Log4j logger */
	private static Logger log = Logger.getLogger(ItemCounter.class);
	
	/** DAO to use to store and retrieve data */
	private ItemCountDAO dao;
	
	/** DSpace Context */
	private Context context;
	
	/**
	 * method invoked by CLI which will result in the number of items
	 * in each community and collection being cached.  These counts will
	 * not update themselves until this is run again.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
		throws ItemCountException, SQLException
	{
        Context context = new Context();
        ItemCounter ic = new ItemCounter(context);
		ic.buildItemCounts();
        context.complete();
	}
	
	/**
	 * Construct a new item counter which will use the give DSpace Context
	 * 
	 * @param context
	 * @throws ItemCountException
	 */
	public ItemCounter(Context context)
		throws ItemCountException
		
	{
		this.context = context;
		this.dao = ItemCountDAOFactory.getInstance(this.context);
	}
	
	/**
	 * This method does the grunt work of drilling through and iterating
	 * over all of the communities and collections in the system and 
	 * obtaining and caching the item counts for each one.
	 * 
	 * @throws ItemCountException
	 */
	public void buildItemCounts()
		throws ItemCountException
	{
		try
		{
			Community[] tlc = Community.findAllTop(context);
			for (int i = 0; i < tlc.length; i++)
			{
				count(tlc[i]);
			}
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
	}
	
	/**
	 * Get the count of the items in the given container.  If the configuration
	 * value webui.strengths.cache is equal to 'true' this will return the
	 * cached value if it exists.  If it is equal to 'false' it will count
	 * the number of items in the container in real time.
	 * 
	 * @param dso
	 * @throws ItemCountException
	 * @throws SQLException 
	 */
	public int getCount(DSpaceObject dso)
		throws ItemCountException
	{
		boolean useCache = ConfigurationManager.getBooleanProperty(
				"webui.strengths.cache", true);
		
		if (useCache)
		{
			return dao.getCount(dso);
		}
		
		// if we make it this far, we need to manually count
		if (dso instanceof Collection)
		{
			try {
				return ((Collection) dso).countItems();
			} catch (SQLException e) {
				log.error("caught exception: ", e);
				throw new ItemCountException(e);
			}
		}
		
		if (dso instanceof Community)
		{
			try {
				return ((Community) dso).countItems();
			} catch (SQLException e) {
				log.error("caught exception: ", e);
				throw new ItemCountException(e);
			}
		}
		
		return 0;
	}
	
	/**
	 * Remove any cached data for the given container
	 * 
	 * @param dso
	 * @throws ItemCountException
	 */
	public void remove(DSpaceObject dso)
		throws ItemCountException
	{
		dao.remove(dso);
	}
	
	/**
	 * count and cache the number of items in the community.  This
	 * will include all sub-communities and collections in the
	 * community.  It will also recurse into sub-communities and
	 * collections and call count() on them also.
	 * 
	 * Therefore, the count the contents of the entire system, it is
	 * necessary just to call this method on each top level community
	 * 
	 * @param community
	 * @throws ItemCountException
	 */
	private void count(Community community)
		throws ItemCountException
	{
		try
		{
			// first count the community we are in
			int count = community.countItems();
			dao.communityCount(community, count);
			
			// now get the sub-communities
			Community[] scs = community.getSubcommunities();
			for (int i = 0; i < scs.length; i++)
			{
				count(scs[i]);
			}
			
			// now get the collections
			Collection[] cols = community.getCollections();
			for (int i = 0; i < cols.length; i++)
			{
				count(cols[i]);
			}
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
	}
	
	/**
	 * count and cache the number of items in the given collection
	 * 
	 * @param collection
	 * @throws ItemCountException
	 */
	private void count(Collection collection)
		throws ItemCountException
	{
		try
		{
			int ccount = collection.countItems();
			dao.collectionCount(collection, ccount);
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new ItemCountException(e);
		}
	}
}
