/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Factory class to generate DAOs based on the configuration
 * 
 * @author Richard Jones
 *
 */
public class BrowseDAOFactory
{
	/**
	 * Get an instance of the relevant Read Only DAO class, which will
	 * conform to the BrowseDAO interface
	 * 
	 * @param context	the DSpace context
	 * @return			the relevant DAO
	 * @throws BrowseException
	 */
	public static BrowseDAO getInstance(Context context)
		throws BrowseException
	{
	    String className = ConfigurationManager.getProperty("browseDAO.class");
        if (className == null)
        {
            // SOLR implementation is the default since DSpace 4.0        	
            return new SolrBrowseDAO(context);
        }
        try
        {
            return (BrowseDAO) Class
                    .forName(ConfigurationManager.getProperty("browseDAO.class"))
                    .getConstructor(Context.class).newInstance(context);
        }
        catch (Exception e)
        {
            throw new BrowseException("The configuration for browseDAO is invalid: "+className, e);
        }
	}
	
	/**
	 * Get an instance of the relevant Write Only DAO class, which will
	 * conform to the BrowseCreateDAO interface
	 * 
	 * @param context	the DSpace context
	 * @return			the relevant DAO
	 * @throws BrowseException
	 */
	public static BrowseCreateDAO getCreateInstance(Context context)
		throws BrowseException
	{
	    String className = ConfigurationManager.getProperty("browseCreateDAO.class");
        if (className == null)
        {
            // SOLR implementation is the default since DSpace 4.0
			return new SolrBrowseCreateDAO(context);
        }
        try
        {
            return (BrowseCreateDAO) Class
                    .forName(ConfigurationManager.getProperty("browseCreateDAO.class"))
                    .getConstructor(Context.class).newInstance(context);
        }
        catch (Exception e)
        {
            throw new BrowseException("The configuration for browseCreateDAO is invalid: "+className, e);
        }
	}

    /**
     * Get an instance of the relevant Read Only DAO class, which will
     * conform to the BrowseItemDAO interface
     *
     * @param context	the DSpace context
     * @return			the relevant DAO
     * @throws BrowseException
     */
    public static BrowseItemDAO getItemInstance(Context context)
        throws BrowseException
    {
        String db = ConfigurationManager.getProperty("db.name");
        if ("postgres".equals(db))
        {
            return new BrowseItemDAOPostgres(context);
        }
        else if ("oracle".equals(db))
        {
            return new BrowseItemDAOOracle(context);
        }
        else
        {
            throw new BrowseException("The configuration for db.name is either invalid, or contains an unrecognised database");
        }
    }

    /**
	 * Get an instance of the relevant DAO Utilities class, which will
	 * conform to the BrowseDAOUtils interface
	 * 
	 * @param context	the DSpace context
	 * @return			the relevant DAO
	 * @throws BrowseException
	 */
	public static BrowseDAOUtils getUtils(Context context)
		throws BrowseException
	{
		String db = ConfigurationManager.getProperty("db.name");
		if ("postgres".equals(db))
		{
			return new BrowseDAOUtilsPostgres();
		}
		else if ("oracle".equals(db))
		{
            return new BrowseDAOUtilsOracle();
		}
		else
		{
			throw new BrowseException("The configuration for db.name is either invalid, or contains an unrecognised database");
		}
	}
}
