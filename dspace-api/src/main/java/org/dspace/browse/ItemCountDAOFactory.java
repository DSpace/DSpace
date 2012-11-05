/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

/**
 * Factory class to allow us to load the correct DAO for registering
 * item count information
 * 
 * @author Richard Jones
 *
 */
public class ItemCountDAOFactory
{
	/**
	 * Get an instance of ItemCountDAO which supports the correct database
	 * for the specific DSpace instance.
	 * 
	 * @param context
	 * @throws ItemCountException
	 */
	public static ItemCountDAO getInstance(Context context)
		throws ItemCountException
	{
		String db = ConfigurationManager.getProperty("db.name");
		ItemCountDAO dao;
		if ("postgres".equals(db))
		{
			dao = new ItemCountDAOPostgres();
		}
		else if ("oracle".equals(db))
		{
			dao = new ItemCountDAOOracle();
		}
		else
		{
			throw new ItemCountException("Database type: " + db + " is not currently supported");
		}
		
		dao.setContext(context);
		return dao;
	}
}
