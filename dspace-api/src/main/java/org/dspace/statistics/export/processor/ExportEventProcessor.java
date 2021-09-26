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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DCDate;
import org.dspace.content.Entity;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.OpenUrlService;

/**
 * Abstract export event processor that contains all shared logic to handle both Items and Bitstreams
 * from the IrusExportUsageEventListener
 */
public abstract class ExportEventProcessor {

    protected static final String ENTITY_TYPE_DEFAULT = "Publication";

    protected static final String ITEM_VIEW = "Investigation";
    protected static final String BITSTREAM_DOWNLOAD = "Request";

    protected final static String UTF_8 = CharEncoding.UTF_8;

    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final EntityService entityService
            = ContentServiceFactory.getInstance().getEntityService();
    private final ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();
    private final OpenUrlService openUrlService
            = OpenURLTrackerLoggerServiceFactory.getInstance().getOpenUrlService();

    private final Context context;
    private final HttpServletRequest request;

    /**
     * Creates a new ExportEventProcessor based on the params and initializes the services
     *
     * @param context
     * @param request
     */
    ExportEventProcessor(Context context, HttpServletRequest request) {
        this.context = context;
        this.request = request;
    }

    /**
     * Processes the event
     *
     * @throws SQLException
     * @throws IOException
     */
    public abstract void processEvent() throws SQLException, IOException;

    /**
     * Process the url obtained from the object to be transmitted
     *
     * @param urlParameters
     * @throws IOException
     * @throws SQLException
     */
    protected void processObject(String urlParameters) throws IOException, SQLException {
        String baseUrl;
        if (StringUtils.equals(configurationService.getProperty("irus.statistics.tracker.environment"), "production")) {
            baseUrl = configurationService.getProperty("irus.statistics.tracker.produrl");
        } else {
            baseUrl = configurationService.getProperty("irus.statistics.tracker.testurl");
        }

        openUrlService.processUrl(context, baseUrl + "?" + urlParameters);
    }

    /**
     * Get the base parameters for the url to be transmitted
     *
     * @param item
     * @return the parameter string to be used in the url
     * @throws UnsupportedEncodingException
     */
    protected String getBaseParameters(Item item)
            throws UnsupportedEncodingException {

        //We have a valid url collect the rest of the data
        String clientIP = request.getRemoteAddr();
        if (configurationService.getBooleanProperty("useProxies", false) && request
                .getHeader("X-Forwarded-For") != null) {
            /* This header is a comma delimited list */
            for (String xfip : request.getHeader("X-Forwarded-For").split(",")) {
                /* proxy itself will sometime populate this header with the same value in
                    remote address. ordering in spec is vague, we'll just take the last
                    not equal to the proxy
                */
                if (!request.getHeader("X-Forwarded-For").contains(clientIP)) {
                    clientIP = xfip.trim();
                }
            }
        }
        String clientUA = StringUtils.defaultIfBlank(request.getHeader("USER-AGENT"), "");
        String referer = StringUtils.defaultIfBlank(request.getHeader("referer"), "");

        //Start adding our data
        StringBuilder data = new StringBuilder();
        data.append(URLEncoder.encode("url_ver", UTF_8))
                .append("=")
                .append(URLEncoder.encode(configurationService.getProperty("irus.statistics.tracker.urlversion"),
                        UTF_8));
        data.append("&").append(URLEncoder.encode("req_id", UTF_8)).append("=")
            .append(URLEncoder.encode(clientIP, UTF_8));
        data.append("&").append(URLEncoder.encode("req_dat", UTF_8)).append("=")
            .append(URLEncoder.encode(clientUA, UTF_8));

        String hostName = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));

        data.append("&").append(URLEncoder.encode("rft.artnum", UTF_8)).append("=").
                append(URLEncoder.encode("oai:" + hostName + ":" + item
                        .getHandle(), UTF_8));
        data.append("&").append(URLEncoder.encode("rfr_dat", UTF_8)).append("=")
            .append(URLEncoder.encode(referer, UTF_8));
        data.append("&").append(URLEncoder.encode("rfr_id", UTF_8)).append("=")
            .append(URLEncoder.encode(hostName, UTF_8));
        data.append("&").append(URLEncoder.encode("url_tim", UTF_8)).append("=")
            .append(URLEncoder.encode(getCurrentDateString(), UTF_8));

        return data.toString();
    }

    /**
     * Get the current date
     *
     * @return the current date as a string
     */
    protected String getCurrentDateString() {
        return new DCDate(new Date()).toString();
    }

    /**
     * Checks if an item should be processed
     *
     * @param item to be checked
     * @return whether the item should be processed
     * @throws SQLException
     */
    protected boolean shouldProcessItem(Item item) throws SQLException {
        if (item == null) {
            return false;
        }
        if (!item.isArchived()) {
            return false;
        }
        if (itemService.canEdit(context, item)) {
            return false;
        }
        if (!shouldProcessItemType(item)) {
            return false;
        }
        if (!shouldProcessEntityType(item)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the item's entity type should be processed
     * When no entity type is present, the check will not be performed and true will be returned.
     *
     * @param item to be checked
     * @return whether the item should be processed
     * @throws SQLException
     */
    protected boolean shouldProcessEntityType(Item item) throws SQLException {
        Entity entity = entityService.findByItemId(context, item.getID());
        EntityType type = entityService.getType(context, entity);

        if (type == null) {
            return true;
        }

        String[] entityTypeStrings = configurationService.getArrayProperty("irus.statistics.tracker.entity-types");
        List<String> entityTypes = new ArrayList<>();

        if (entityTypeStrings.length != 0) {
            entityTypes.addAll(Arrays.asList(entityTypeStrings));
        } else {
            entityTypes.add(ENTITY_TYPE_DEFAULT);
        }

        if (entityTypes.contains(type.getLabel())) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the item should be excluded based on the its type
     *
     * @param item to be checked
     * @return whether the item should be processed
     */
    protected boolean shouldProcessItemType(Item item) {
        String trackerTypeMetadataField = configurationService.getProperty("irus.statistics.tracker.type-field");
        String[] metadataValues = configurationService.getArrayProperty("irus.statistics.tracker.type-value");
        List<String> trackerTypeMetadataValues;
        if (metadataValues.length > 0) {
            trackerTypeMetadataValues = new ArrayList<>();
            for (String metadataValue : metadataValues) {
                trackerTypeMetadataValues.add(metadataValue.toLowerCase());
            }
        } else {
            trackerTypeMetadataValues = null;
        }

        if (trackerTypeMetadataField != null && trackerTypeMetadataValues != null) {

            // Contains the schema, element and if present qualifier of the metadataField
            String[] metadataFieldSplit = trackerTypeMetadataField.split("\\.");

            List<MetadataValue> types = itemService
                    .getMetadata(item, metadataFieldSplit[0], metadataFieldSplit[1],
                                 metadataFieldSplit.length == 2 ? null : metadataFieldSplit[2], Item.ANY);

            if (!types.isEmpty()) {
                //Find out if we have a type that needs to be excluded
                for (MetadataValue type : types) {
                    if (trackerTypeMetadataValues.contains(type.getValue().toLowerCase())) {
                        //We have found no type so process this item
                        return false;
                    }
                }
                return true;
            } else {
                // No types in this item, so not excluded
                return true;
            }
        } else {
            // No types to be excluded
            return true;
        }
    }
}
