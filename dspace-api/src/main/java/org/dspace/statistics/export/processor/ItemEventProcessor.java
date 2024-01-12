/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.processor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Processor that handles Item events from the IrusExportUsageEventListener
 */
public class ItemEventProcessor extends ExportEventProcessor {

    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private final Item item;

    /**
     * Creates a new ItemEventProcessor that will set the params
     *
     * @param context
     * @param request
     * @param item
     */
    public ItemEventProcessor(Context context, HttpServletRequest request, Item item) {
        super(context, request);
        this.item = item;
    }

    /**
     * Process the event
     * Check if the item should be processed
     * Create the url to be transmitted based on item data
     *
     * @throws SQLException
     * @throws IOException
     */
    @Override
    public void processEvent() throws SQLException, IOException {
        if (shouldProcessItem(item)) {
            String baseParam = getBaseParameters(item);
            String fullParam = addObjectSpecificData(baseParam, item);
            processObject(fullParam);
        }
    }

    /**
     * Adds additional item data to the url
     *
     * @param string to which the additional data needs to be added
     * @param item
     * @return the string with additional data
     * @throws UnsupportedEncodingException
     */
    protected String addObjectSpecificData(final String string, Item item) throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder(string);
        String itemInfo = getItemInfo(item);
        data.append("&").append(URLEncoder.encode("svc_dat", UTF_8)).append("=")
            .append(URLEncoder.encode(itemInfo, UTF_8));
        data.append("&").append(URLEncoder.encode("rft_dat", UTF_8)).append("=")
            .append(URLEncoder.encode(ITEM_VIEW, UTF_8));
        return data.toString();
    }

    /**
     * Get Item info used for the url
     *
     * @param item
     * @return item info
     */
    private String getItemInfo(final Item item) {
        StringBuilder sb = new StringBuilder(configurationService.getProperty("dspace.ui.url"));
        sb.append("/handle/").append(item.getHandle());

        return sb.toString();
    }


}
