/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Event;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.OpenURLTrackerLoggerService;
import org.dspace.statistics.util.SpiderDetector;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 30-mrt-2010
 * Time: 16:37:56
 */
public class ExportUsageEventListener extends AbstractUsageEventListener {
    /*  Log4j logger*/
    private static Logger log = Logger.getLogger(ExportUsageEventListener.class);

    /* The metadata field which is to be checked for */
    private static MetadataField trackerType;

    /* A list of values the type might have */
    private static List<String> trackerValues;

    /* The base url of the tracker */
    private static String baseUrl;

    private static String trackerUrlVersion;

    private static final String ITEM_VIEW = "Investigation";
    private static final String BITSTREAM_DOWNLOAD = "Request";

    private static ConfigurationService configurationService;


    public void init(Context context) {
        try {
            if (configurationService == null) {
                configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
            }
            if (trackerType == null) {
                trackerType = resolveConfigPropertyToMetadataField(context, "tracker.type-field");

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


            }
        } catch (Exception e) {
            log.error("Unknown error resolving configuration for the export usage event.", e);
            trackerType = null;
            trackerValues = null;
            baseUrl = null;
            trackerUrlVersion = null;
        }
    }

    public void receiveEvent(Event event) {
        if (event instanceof UsageEvent) {
            UsageEvent ue = (UsageEvent) event;
            Context context = ue.getContext();
            try {
                //Check for item investigation
                if (ue.getObject() instanceof Item) {
                    Item item = (Item) ue.getObject();
                    if (item.isArchived() && !ContentServiceFactory.getInstance().getItemService()
                                                                   .canEdit(context, item)) {
                        init(context);

                        if (shouldProcessItem(item)) {
                            processItem(ue.getContext(), item, null, ue.getRequest(), ITEM_VIEW);
                        }
                    }
                }
                //Check for bitstream download
                if (ue.getObject() instanceof Bitstream) {
                    Bitstream bit = (Bitstream) ue.getObject();
                    //Check for an item
                    if (0 < bit.getBundles().size()) {
                        if (!SpiderDetector.isSpider(ue.getRequest())) {
                            Bundle bundle = bit.getBundles().get(0);
                            if (bundle.getName() == null || !bundle.getName().equals("ORIGINAL")) {
                                return;
                            }

                            if (0 < bundle.getItems().size()) {
                                Item item = bundle.getItems().get(0);

                                if (item.isArchived() && !ContentServiceFactory.getInstance().getItemService()
                                                                               .canEdit(context, item)) {
                                    //Check if we have a valid type of item !
                                    init(context);
                                    if (shouldProcessItem(item)) {
                                        processItem(ue.getContext(), item, bit, ue.getRequest(), BITSTREAM_DOWNLOAD);
                                    }
                                }
                            }
                        } else {
                            log.info("Robot (" + ue.getRequest().getHeader("user-agent") + ") accessed  " + bit
                                    .getName() + "/" + bit.getSource());
                        }
                    }
                }
            } catch (Exception e) {
                UUID id;
                id = ue.getObject().getID();

                int type;
                try {
                    type = ue.getObject().getType();
                } catch (Exception e1) {
                    type = -1;
                }
                log.error(LogManager.getHeader(ue.getContext(), "Error while processing export of use event",
                                               "Id: " + id + " type: " + type), e);
                e.printStackTrace();
            }
        }
    }

    private boolean shouldProcessItem(Item item) {
        if (trackerType != null && trackerValues != null) {
            List<MetadataValue> types = ContentServiceFactory.getInstance().getItemService()
                                 .getMetadata(item, trackerType.getMetadataSchema().getName(),
                                        trackerType.getElement(),
                                        trackerType.getQualifier(), Item.ANY);

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

    private void processItem(Context context, Item item, Bitstream bitstream, HttpServletRequest request,
                             String eventType) throws IOException, SQLException {
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
            .append(URLEncoder.encode(new DCDate(new Date()).toString(), "UTF-8"));

        if (BITSTREAM_DOWNLOAD.equals(eventType)) {
            String bitstreamInfo = getBitstreamInfo(item, bitstream);
            data.append("&").append(URLEncoder.encode("svc_dat", "UTF-8")).append("=")
                .append(URLEncoder.encode(bitstreamInfo, "UTF-8"));
            data.append("&").append(URLEncoder.encode("rft_dat", "UTF-8")).append("=")
                .append(URLEncoder.encode(BITSTREAM_DOWNLOAD, "UTF-8"));
        } else if (ITEM_VIEW.equals(eventType)) {
            String itemInfo = getItemInfo(item);
            data.append("&").append(URLEncoder.encode("svc_dat", "UTF-8")).append("=")
                .append(URLEncoder.encode(itemInfo, "UTF-8"));
            data.append("&").append(URLEncoder.encode("rft_dat", "UTF-8")).append("=")
                .append(URLEncoder.encode(ITEM_VIEW, "UTF-8"));
        }

        processUrl(context, baseUrl + "?" + data.toString());

    }

    private String getBitstreamInfo(final Item item, final Bitstream bitstream) {
        //only for jsp ui
        // http://demo.dspace.org/jspui/handle/10673/2235
        // http://demo.dspace.org/jspui/bitstream/10673/2235/1/Captura.JPG
        //


        //only fror xmlui
        // http://demo.dspace.org/xmlui/handle/10673/2235
        // http://demo.dspace.org/xmlui/bitstream/handle/10673/2235/Captura.JPG?sequence=1
        //

        String uiType = configurationService.getProperty("stats.dspace.type");
        StringBuilder sb = new StringBuilder(configurationService.getProperty("dspace.url"));
        if ("jspui".equals(uiType)) {

            sb.append("/bitstream/").append(item.getHandle()).append("/").append(bitstream.getSequenceID());

            // If we can, append the pretty name of the bitstream to the URL
            try {
                if (bitstream.getName() != null) {
                    sb.append("/").append(Util.encodeBitstreamName(bitstream.getName(), "UTF-8"));
                }
            } catch (UnsupportedEncodingException uee) {
                // just ignore it, we don't have to have a pretty
                // name at the end of the URL because the sequence id will
                // locate it. However it means that links in this file might
                // not work....
            }


        } else { //xmlui

            String identifier = null;
            if (item != null && item.getHandle() != null) {
                identifier = "handle/" + item.getHandle();
            } else if (item != null) {
                identifier = "item/" + item.getID();
            } else {
                identifier = "id/" + bitstream.getID();
            }


            sb.append("/bitstream/").append(identifier).append("/");

            // If we can, append the pretty name of the bitstream to the URL
            try {
                if (bitstream.getName() != null) {
                    sb.append(Util.encodeBitstreamName(bitstream.getName(), "UTF-8"));
                }
            } catch (UnsupportedEncodingException uee) {
                // just ignore it, we don't have to have a pretty
                // name at the end of the URL because the sequence id will
                // locate it. However it means that links in this file might
                // not work....
            }

            sb.append("?sequence=").append(bitstream.getSequenceID());
        }
        return sb.toString();
    }

    private String getItemInfo(final Item item) {
        StringBuilder sb = new StringBuilder(configurationService.getProperty("dspace.url"));
        sb.append("/handle/").append(item.getHandle());

        return sb.toString();
    }


    private static void processUrl(Context c, String urlStr) throws IOException, SQLException {
        log.debug("Prepared to send url to tracker URL: " + urlStr);
        System.out.println(urlStr);
        URLConnection conn;

        try {
            // Send data
            URL url = new URL(urlStr);
            conn = url.openConnection();

            if (((HttpURLConnection) conn).getResponseCode() != 200) {
                ExportUsageEventListener.logfailed(c, urlStr);
            } else if (log.isDebugEnabled()) {
                log.debug("Successfully posted " + urlStr + " on " + new Date());
            }
        } catch (Exception e) {
            log.error("Failed to send url to tracker URL: " + urlStr);
            ExportUsageEventListener.logfailed(c, urlStr);
        }
    }

    private static void tryReprocessFailed(Context context, OpenURLTracker tracker) throws SQLException {
        boolean success = false;
        URLConnection conn;
        try {
            URL url = new URL(tracker.getUrl());
            conn = url.openConnection();

            if (((HttpURLConnection) conn).getResponseCode() == HttpURLConnection.HTTP_OK) {
                success = true;
            }
        } catch (Exception e) {
            success = false;
        } finally {
            if (success) {
                OpenURLTrackerLoggerServiceFactory.getInstance().getOpenUrlTrackerLoggerService()
                                                  .remove(context, tracker);
                // If the tracker was able to post successfully, we remove it from the database
                log.info("Successfully posted " + tracker.getUrl() + " from " + tracker.getUploadDate());
            } else {
                // Still no luck - write an error msg but keep the entry in the table for future executions
                log.error("Failed attempt from " + tracker.getUrl() + " originating from " + tracker.getUploadDate());
            }
        }
    }

    public static void reprocessFailedQueue(Context context) throws SQLException {
        Context c = new Context();
        OpenURLTrackerLoggerServiceFactory instance = OpenURLTrackerLoggerServiceFactory.getInstance();
        if (instance == null) {
            log.error("Error retrieving the \"OpenURLTrackerLoggerServiceFactory\" instance, aborting the processing");
            return;
        }
        OpenURLTrackerLoggerService openUrlTrackerLoggerService = instance.getOpenUrlTrackerLoggerService();
        if (openUrlTrackerLoggerService == null) {
            log.error("Error retrieving the \"openUrlTrackerLoggerService\" instance, aborting the processing");
            return;
        }
        List<OpenURLTracker> openURLTrackers = openUrlTrackerLoggerService.findAll(c);
        for (OpenURLTracker openURLTracker : openURLTrackers) {
            ExportUsageEventListener.tryReprocessFailed(context, openURLTracker);
        }

        try {
            c.abort();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void logfailed(Context context, String url) throws SQLException {
        Date now = new Date();
        if (url.equals("")) {
            return;
        }
        OpenURLTrackerLoggerService service = OpenURLTrackerLoggerServiceFactory.getInstance()
                                                                                .getOpenUrlTrackerLoggerService();
        OpenURLTracker tracker = service.create(context);
        tracker.setUploadDate(now);
        tracker.setUrl(url);
        // TODO service tracker update
    }

    private static MetadataField resolveConfigPropertyToMetadataField(Context context, String fieldName)
            throws SQLException {
        String metadataField = configurationService.getProperty("stats." + fieldName);
        if (metadataField != null && 0 < metadataField.trim().length()) {
            metadataField = metadataField.trim();
            MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
            return metadataFieldService
                    .findByElement(context, metadataField.split("\\.")[0], metadataField.split("\\.")[1],
                                   metadataField.split("\\.").length == 2 ? null : metadataField.split("\\.")[2]);
        }
        return null;
    }
}
