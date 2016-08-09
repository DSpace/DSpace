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
 * @author Ivan Masár
 *
 */
public class ItemCountDAOFactory
{
    /**
     * Get an instance of ItemCountDAO which supports the correct storage backend
     * for the specific DSpace instance.
     * 
     * @param context DSpace Context
     * @return DAO
     * @throws ItemCountException if count error
     */
    public static ItemCountDAO getInstance(Context context)
        throws ItemCountException
    {
        
        /** Log4j logger */
        ItemCountDAO dao = null;
        
        String className = ConfigurationManager.getProperty("ItemCountDAO.class");
        
        // SOLR implementation is the default since DSpace 4.0
        if (className == null)
        {
            dao = new ItemCountDAOSolr();
        }
        else
        {
            try
            {
                dao = (ItemCountDAO) Class
                        .forName(className.trim()).newInstance();
            }
            catch (Exception e)
            {
                throw new ItemCountException("The configuration for ItemCountDAO is invalid: " + className, e);
            }
        }
        
        dao.setContext(context);
        return dao;
    }
}
