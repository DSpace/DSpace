/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation for the access status calculation service.
 */
public class AccessStatusServiceImpl implements AccessStatusService {
    // Plugin implementation, set from the DSpace configuration by init().
    protected AccessStatusHelper helper = null;

    protected Date forever_date = null;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected PluginService pluginService;

    /**
     * Initialize the bean (after dependency injection has already taken place).
     * Ensures the configurationService is injected, so that we can get the plugin
     * and the forever embargo date threshold from the configuration.
     * Called by "init-method" in Spring configuration.
     *
     * @throws Exception on generic exception
     */
    public void init() throws Exception {
        if (helper == null) {
            helper = (AccessStatusHelper) pluginService.getSinglePlugin(AccessStatusHelper.class);
            if (helper == null) {
                throw new IllegalStateException("The AccessStatusHelper plugin was not defined in "
                        + "DSpace configuration.");
            }

            // Defines the embargo forever date threshold for the access status.
            // Look at EmbargoService.FOREVER for some improvements?
            int year = configurationService.getIntProperty("access.status.embargo.forever.year");
            int month = configurationService.getIntProperty("access.status.embargo.forever.month");
            int day = configurationService.getIntProperty("access.status.embargo.forever.day");

            forever_date = new LocalDate(year, month, day).toDate();
        }
    }

    @Override
    public String getAccessStatus(Context context, Item item) throws SQLException {
        return helper.getAccessStatusFromItem(context, item, forever_date);
    }

    @Override
    public String getEmbargoFromItem(Context context, Item item) throws SQLException {
        return helper.getEmbargoFromItem(context, item, forever_date);
    }
}
