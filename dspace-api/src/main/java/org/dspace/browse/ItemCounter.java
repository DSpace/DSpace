/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides a standard interface to all item counting
 * operations for communities and collections.
 *
 * In the event that the data cache is not being used, this class will return direct
 * real time counts of content.
 */
public class ItemCounter {
    /**
     * Log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemCounter.class);

    @Autowired
    protected ItemService itemService;
    @Autowired
    protected ConfigurationService configurationService;

    /**
     * Construct a new item counter
     */
    protected ItemCounter() {
    }

    /**
     * Get the count of the items in the given container. If the configuration
     * value webui.strengths.show is equal to 'true' this method will return all
     * archived items. If the configuration value webui.strengths.show is equal to
     * 'false' this method will return -1.
     * If the configuration value webui.strengths.cache
     * is equal to 'true' this will return the cached value if it exists.
     * If it is equal to 'false' it will count the number of items
     * in the container in real time.
     *
     * @param context DSpace Context
     * @param dso DSpaceObject
     * @return count (-1 is returned if count could not be determined or is disabled)
     */
    public int getCount(Context context, DSpaceObject dso) {
        boolean showStrengths = configurationService.getBooleanProperty("webui.strengths.show", false);
        boolean useCache = configurationService.getBooleanProperty("webui.strengths.cache", true);
        if (!showStrengths) {
            return -1;
        }

        if (useCache) {
            // NOTE: This bean is NOT Autowired above because it's a "prototype" bean which we want to reload
            // occasionally. Each time the bean reloads it will update the cached item counts.
            ItemCountDAO dao =
                DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("itemCountDAO",
                                                                                         ItemCountDAO.class);
            return dao.getCount(context, dso);
        }

        // if we make it this far, we need to manually count
        if (dso instanceof Collection) {
            try {
                return itemService.countItems(context, (Collection) dso);
            } catch (SQLException e) {
                log.error("Error counting number of Items in Collection {} :", dso.getID(), e);
                return -1;
            }
        }

        if (dso instanceof Community) {
            try {
                return itemService.countItems(context, ((Community) dso));
            } catch (SQLException e) {
                log.error("Error counting number of Items in Community {} :", dso.getID(), e);
                return -1;
            }
        }

        return 0;
    }
}
