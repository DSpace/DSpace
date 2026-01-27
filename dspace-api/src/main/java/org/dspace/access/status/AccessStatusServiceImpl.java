/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.AccessStatus;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation for the access status calculation service.
 */
public class AccessStatusServiceImpl implements AccessStatusService {
    private static final Logger log = LogManager.getLogger(AccessStatusServiceImpl.class);

    // Plugin implementation, set from the DSpace configuration by init().
    protected AccessStatusHelper helper = null;

    protected LocalDate forever_date = null;

    protected String itemCalculationType = null;
    protected String bitstreamCalculationType = null;

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

            forever_date = LocalDate.of(year, month, day)
                    .atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            itemCalculationType = getAccessStatusCalculationType("access.status.for-user.item");
            bitstreamCalculationType = getAccessStatusCalculationType("access.status.for-user.bitstream");
        }
    }

    @Override
    public AccessStatus getAccessStatus(Context context, Item item) throws SQLException {
        return helper.getAccessStatusFromItem(context, item, forever_date, itemCalculationType);
    }

    @Override
    public AccessStatus getAnonymousAccessStatus(Context context, Item item) throws SQLException {
        return helper.getAnonymousAccessStatusFromItem(context, item, forever_date);
    }

    @Override
    public AccessStatus getAccessStatus(Context context, Bitstream bitstream) throws SQLException {
        return helper.getAccessStatusFromBitstream(context, bitstream, forever_date, bitstreamCalculationType);
    }

    private String getAccessStatusCalculationType(String key) {
        String value = configurationService.getProperty(key, DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS);
        if (!Strings.CI.equals(value, DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS) &&
            !Strings.CI.equals(value, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER)) {
            log.warn("The configuration parameter \"" + key
                + "\" contains an invalid value. Valid values include: 'anonymous' and 'current'.");
            value = DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS;
        }
        return value;
    }
}
