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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.content.Entity;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.OpenUrlService;

/**
 * Abstract export event processor that contains all shared logic to handle both Items and Bitstreams
 * from the ExportUsageEventListener
 */
public abstract class ExportEventProcessor {

    private static Logger log = Logger.getLogger(ExportEventProcessor.class);

    /* The metadata field which is to be checked for */
    protected String trackerType;

    /* A list of entity types that will be processed */
    protected List<String> entityTypes;
    protected static final String ENTITY_TYPE_DEFAULT = "Publication";

    /* A list of values the type might have */
    protected List<String> trackerValues;

    /* The base url of the tracker */
    protected String baseUrl;

    protected String trackerUrlVersion;

    protected static final String ITEM_VIEW = "Investigation";
    protected static final String BITSTREAM_DOWNLOAD = "Request";

    protected static ConfigurationService configurationService;

    protected static EntityTypeService entityTypeService;
    protected static EntityService entityService;

    protected static OpenUrlService openUrlService;


    protected Context context;
    protected HttpServletRequest request;
    protected ItemService itemService;

    /**
     * Creates a new ExportEventProcessor based on the params and initializes the services
     *
     * @param context
     * @param request
     */
    ExportEventProcessor(Context context, HttpServletRequest request) {
        this.context = context;
        this.request = request;
        initServices();
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

        openUrlService.processUrl(context, baseUrl + "?" + urlParameters);
    }

    /**
     * Get the base parameters for the url to be transmitted
     *
     * @param item
     * @return the parameter string to be used in the url
     * @throws UnsupportedEncodingException
     */
    protected String getBaseParamaters(Item item)
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
        data.append(URLEncoder.encode("url_ver", "UTF-8") + "=" + URLEncoder.encode(trackerUrlVersion, "UTF-8"));
        data.append("&").append(URLEncoder.encode("req_id", "UTF-8")).append("=")
            .append(URLEncoder.encode(clientIP, "UTF-8"));
        data.append("&").append(URLEncoder.encode("req_dat", "UTF-8")).append("=")
            .append(URLEncoder.encode(clientUA, "UTF-8"));
        data.append("&").append(URLEncoder.encode("rft.artnum", "UTF-8")).append("=").
                append(URLEncoder.encode("oai:" + configurationService.getProperty("dspace.hostname") + ":" + item
                        .getHandle(), "UTF-8"));
        data.append("&").append(URLEncoder.encode("rfr_dat", "UTF-8")).append("=")
            .append(URLEncoder.encode(referer, "UTF-8"));
        data.append("&").append(URLEncoder.encode("rfr_id", "UTF-8")).append("=")
            .append(URLEncoder.encode(configurationService.getProperty("dspace.hostname"), "UTF-8"));
        data.append("&").append(URLEncoder.encode("url_tim", "UTF-8")).append("=")
            .append(URLEncoder.encode(getCurrentDateString(), "UTF-8"));

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
     *
     * @param item to be checked
     * @return whether the item should be processed
     * @throws SQLException
     */
    protected boolean shouldProcessEntityType(Item item) throws SQLException {
        Entity entity = entityService.findByItemId(context, item.getID());
        EntityType type = entityService.getType(context, entity);

        if (type != null && entityTypes.contains(type.getLabel())) {
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
        if (trackerType != null && trackerValues != null) {
            List<MetadataValue> types = itemService
                    .getMetadata(item, trackerType.split("\\.")[0], trackerType.split("\\.")[1],
                                 trackerType.split("\\.").length == 2 ? null : trackerType.split("\\.")[2], Item.ANY);

            if (!types.isEmpty()) {
                //Find out if we have a type that needs to be excluded
                for (MetadataValue type : types) {
                    if (trackerValues.contains(type.getValue().toLowerCase())) {
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

    /**
     * Initializes services and params obtained from DSpace config
     */
    private void initServices() {
        try {
            if (configurationService == null) {
                configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
            }
            if (entityService == null) {
                entityService = ContentServiceFactory.getInstance().getEntityService();
            }
            if (itemService == null) {
                itemService = ContentServiceFactory.getInstance().getItemService();
            }
            if (entityTypeService == null) {
                entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
            }
            if (openUrlService == null) {
                openUrlService = OpenURLTrackerLoggerServiceFactory.getInstance().getOpenUrlService();
            }
            if (trackerType == null) {
                trackerType = configurationService.getProperty("stats.tracker.type-field");

                String[] metadataValues = configurationService.getArrayProperty("stats.tracker.type-value");
                if (metadataValues.length > 0) {
                    trackerValues = new ArrayList<>();
                    for (String metadataValue : metadataValues) {
                        trackerValues.add(metadataValue.toLowerCase());
                    }
                } else {
                    trackerValues = null;
                }

                if (StringUtils.equals(configurationService.getProperty("stats.tracker.environment"), "production")) {
                    baseUrl = configurationService.getProperty("stats.tracker.produrl");
                } else {
                    baseUrl = configurationService.getProperty("stats.tracker.testurl");
                }

                trackerUrlVersion = configurationService.getProperty("stats.tracker.urlversion");
                String[] entityTypeStrings = configurationService.getArrayProperty("stats.tracker.entity-types");
                entityTypes = new ArrayList<>();
                if (entityTypeStrings.length != 0) {
                    entityTypes.addAll(Arrays.asList(entityTypeStrings));
                } else {
                    entityTypes.add(ENTITY_TYPE_DEFAULT);
                }
            }
        } catch (Exception e) {
            log.error("Unknown error resolving configuration for the export usage event.", e);
            trackerType = null;
            trackerValues = null;
            baseUrl = null;
            trackerUrlVersion = null;
        }
    }
}
