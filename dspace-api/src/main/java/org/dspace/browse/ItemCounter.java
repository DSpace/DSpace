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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;

import java.sql.SQLException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

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

    protected ItemService itemService;
    protected ConfigurationService configurationService;
	
    /**
     * Construct a new item counter which will use the given DSpace Context
     * 
     * @param context current context
     * @throws ItemCountException if count error
     */
    public ItemCounter(Context context)
            throws ItemCountException

    {
        this.context = context;
        this.dao = ItemCountDAOFactory.getInstance(this.context);
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    /**
     * Get the count of the items in the given container.  If the configuration
     * value webui.strengths.cache is equal to 'true' this will return the
     * cached value if it exists.  If it is equal to 'false' it will count
     * the number of items in the container in real time.
     * 
     * @param dso DSpaceObject
     * @return count
     * @throws ItemCountException when error occurs
     */
    public int getCount(DSpaceObject dso)
            throws ItemCountException
    {
        boolean useCache = configurationService.getBooleanProperty(
                        "webui.strengths.cache", true);

        if (useCache)
        {
            return dao.getCount(dso);
        }

        // if we make it this far, we need to manually count
        if (dso instanceof Collection)
        {
            try {
                return itemService.countItems(context, (Collection) dso);
            } catch (SQLException e) {
                log.error("caught exception: ", e);
                throw new ItemCountException(e);
            }
        }

        if (dso instanceof Community)
        {
            try {
                return itemService.countItems(context, ((Community) dso));
            } catch (SQLException e) {
                log.error("caught exception: ", e);
                throw new ItemCountException(e);
            }
        }

        return 0;
    }
}
