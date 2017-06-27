/*
 * ExportUsageEventListener.java
 *
 * Version: $Revision: 1 $
 * Date: $Date: 2010-04-09 11:01:28 +0200 (vr, 09 apr 2010) $
 * Copyright (c) @mire.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package com.atmire.statistics.export;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.services.model.Event;
import org.dspace.statistics.util.SpiderDetector;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 30-mrt-2010
 * Time: 16:37:56
 */
public class ExportUsageEventListener extends AbstractUsageEventListener {
    /*  Log4j logger*/
    private static Logger log = Logger.getLogger(ExportUsageEventListener.class);

    /* The metadata field which is to be checked for */
    private static Metadatum trackerType;


    /* A list of values the type might have */
    private static List<String> trackerValues;

    /* The base url of the tracker */
    private static String baseUrl;

    private static String trackerUrlVersion;


    public void init() {
        try {
            if (trackerType == null) {
                trackerType = resolveConfigPropertyToMetadataField("tracker.type-field");

                String metadataValues = ConfigurationManager.getProperty("stats", "tracker.type-value");
                if (metadataValues != null && 0 < metadataValues.trim().length()) {
                    trackerValues = new ArrayList<String>();
                    if (metadataValues.contains(",")) {
                        //We have more then one value
                        String[] values = metadataValues.split(",");
                        //We add all the values withouth spaces & lowercase so we don't have to worry about the cases.
                        for (String val : values)
                            trackerValues.add(val.trim().toLowerCase());
                    } else {
                        trackerValues.add(metadataValues);
                    }
                } else
                    trackerValues = null;

                baseUrl = ConfigurationManager.getProperty("stats", "tracker.baseurl");
                trackerUrlVersion = ConfigurationManager.getProperty("stats", "tracker.urlversion");


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
            try {
                //First of all check for a bitstream download event
                if (ue.getObject() instanceof Bitstream) {
                    Bitstream bit = (Bitstream) ue.getObject();
                    //Check for an item
                    if (0 < bit.getBundles().length) {
                        if (!SpiderDetector.isSpider(ue.getRequest())) {
                            Bundle bundle = bit.getBundles()[0];
                            if (bundle.getName() == null || !bundle.getName().equals("ORIGINAL"))
                                return;

                            if (0 < bundle.getItems().length) {
                                Item item = bundle.getItems()[0];
                                if(item.isArchived() && !item.canEdit()) {
                                    //Check if we have a valid type of item !
                                    init();
                                    if (trackerType != null && trackerValues != null) {
                                        Metadatum[] types = item.getMetadata(trackerType.schema, trackerType.element, trackerType.qualifier, Item.ANY);
                                        if(types.length>0){
                                            //Find out if we have a type that matches one of our values
                                            for (Metadatum type : types) {
                                                if (!trackerValues.contains(type.value.toLowerCase())) {
                                                    //We have found a type so process this bitstream
                                                    processItem(ue.getContext(), item, bit, ue.getRequest());
                                                    break;
                                                }
                                            }
                                        }
                                        else {
                                            processItem(ue.getContext(), item, bit, ue.getRequest());
                                        }
                                    } else {
                                        //no tracker.type-field => process all items
                                        processItem(ue.getContext(), item, bit, ue.getRequest());
                                    }
                                }
                            }
                        } else {
                            log.info("Robot (" + ue.getRequest().getHeader("user-agent") + ") accessed  " + bit.getName() + "/" + bit.getSource());
                        }
                    }
                }
            } catch (Exception e) {
                int id;
                try {
                    id = ue.getObject().getID();
                } catch (Exception e1) {
                    id = -1;
                }
                int type;
                try {
                    type = ue.getObject().getType();
                } catch (Exception e1) {
                    type = -1;
                }
                log.error(LogManager.getHeader(ue.getContext(), "Error while processing export of use event", "Id: " + id + " type: " + type), e);
                e.printStackTrace();
            }
        }
    }

    private void processItem(Context context, Item item, Bitstream bitstream, HttpServletRequest request) throws IOException, SQLException {
        //We have a valid url collect the rest of the data
        String clientIP = request.getRemoteAddr();
        if (ConfigurationManager.getBooleanProperty("useProxies", false) && request.getHeader("X-Forwarded-For") != null) {
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
        String mimeType = bitstream.getFormat().getMIMEType();

        //Start adding our data
        String data = "";
        data += URLEncoder.encode("url_ver", "UTF-8") + "=" + URLEncoder.encode(trackerUrlVersion, "UTF-8");
        data += "&" + URLEncoder.encode("req_id", "UTF-8") + "=" + URLEncoder.encode("urn:ip:" + clientIP, "UTF-8");
        data += "&" + URLEncoder.encode("req_dat", "UTF-8") + "=" + URLEncoder.encode(clientUA, "UTF-8");
        data += "&" + URLEncoder.encode("rft.artnum", "UTF-8") + "=" + URLEncoder.encode("oai:" + ConfigurationManager.getProperty("dspace.hostname") + ":" + item.getHandle(), "UTF-8");
        data += "&" + URLEncoder.encode("rfr_dat", "UTF-8") + "=" + URLEncoder.encode(referer, "UTF-8");
//        data += "&" + URLEncoder.encode("svc.format", "UTF-8") + "=" + URLEncoder.encode(mimeType, "UTF-8");
        data += "&" + URLEncoder.encode("rfr_id", "UTF-8") + "=" + URLEncoder.encode(ConfigurationManager.getProperty("dspace.hostname"), "UTF-8");
        data += "&" + URLEncoder.encode("url_tim", "UTF-8") + "=" + URLEncoder.encode(new DCDate(new Date()).toString(), "UTF-8");

        //only for jsp ui
        // http://demo.dspace.org/jspui/handle/10673/2235
        // http://demo.dspace.org/jspui/bitstream/10673/2235/1/Captura.JPG
        //


        //only fror xmlui
        // http://demo.dspace.org/xmlui/handle/10673/2235
        // http://demo.dspace.org/xmlui/bitstream/handle/10673/2235/Captura.JPG?sequence=1
        //

        String uiType = ConfigurationManager.getProperty("stats", "dspace.type");
        StringBuffer sb = new StringBuffer(ConfigurationManager.getProperty("dspace.url"));
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

        data += "&" + URLEncoder.encode("svc_dat", "UTF-8") + "=" + URLEncoder.encode(sb.toString(), "UTF-8");

        processUrl(context, baseUrl + "?" + data);

    }

    public static void processUrl(Context c, String urlStr) throws IOException, SQLException {
        log.debug("Prepared to send url to tracker URL: " + urlStr); 
        //System.out.println(urlStr);
        URLConnection conn;

        try {
            // Send data
            URL url = new URL(urlStr);
            conn = url.openConnection();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while (rd.readLine() != null) ;

            rd.close();
            if (((HttpURLConnection) conn).getResponseCode() != 200) {
                ExportUsageEventListener.logfailed(c, urlStr);
            }
        } catch (Exception e) {
            log.error("Failed to send url to tracker URL: " + urlStr);
            ExportUsageEventListener.logfailed(c, urlStr);
        } finally {
//            // @TODO: REMOVE THIS FOR PRODUCTION - ADDING VALID URL's ONLY TO TEST CMD LINE SCRIPT
//            ExportUsageEventListener.logfailed(c, urlStr);
        }
    }

    private static void tryReprocessFailed(OpenUrlTrackerLogger tracker) throws SQLException {
        boolean success = false;
        URLConnection conn;
        try {
            URL url = new URL(tracker.getUrl());
            conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while (rd.readLine() != null) ;
            rd.close();
            if (((HttpURLConnection) conn).getResponseCode() == 200) {
                success = true;
            }
        } catch (Exception e) {
            success = false;
        } finally {
            if (success) {
                // If the tracker was able to post successfully, we remove it from the database
                tracker.delete();
                log.info("Successfully posted " + tracker.getUrl() + " from " + tracker.getUploaddate());
            } else {
                // Still no luck - write an error msg but keep the entry in the table for future executions
                log.error("Failed attempt from " + tracker.getUrl() + " originating from " + tracker.getUploaddate());
            }
        }
    }

    public static void reprocessFailedQueue() throws SQLException {
        Context c = new Context();
        OpenUrlTrackerLogger[] trackerLoggers = OpenUrlTrackerLogger.findAll(c);
        for (OpenUrlTrackerLogger openUrlTrackerLogger : trackerLoggers) {
            ExportUsageEventListener.tryReprocessFailed(openUrlTrackerLogger);
        }
        try {
            c.abort();
        } catch (Exception ignored) {
        }
    }

    public static void logfailed(Context context, String url) throws SQLException {
        Date now = new Date();
        if (url.equals("")) return;
        OpenUrlTrackerLogger tracker = OpenUrlTrackerLogger.create(context);
        tracker.setUploaddate(now);
        tracker.setUrl(url);
        tracker.update();
    }

    private static Metadatum resolveConfigPropertyToMetadataField(String fieldName) {
        String metadataField = ConfigurationManager.getProperty("stats", fieldName);
        if (metadataField != null && 0 < metadataField.trim().length()) {
            metadataField = metadataField.trim();
            Metadatum dcVal = new Metadatum();
            dcVal.schema = metadataField.split("\\.")[0];
            dcVal.element = metadataField.split("\\.")[1];
            dcVal.qualifier = metadataField.split("\\.").length == 2 ? null : metadataField.split("\\.")[2];
            return dcVal;
        }
        return null;
    }
}
