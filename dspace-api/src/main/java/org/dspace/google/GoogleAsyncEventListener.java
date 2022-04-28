/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.google.common.base.Throwables;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Notifies Google Analytics of Bitstream VIEW events. These events are stored in memory and then
 * asynchronously processed by a single seperate thread.
 *
 * @author April Herron
 */
public class GoogleAsyncEventListener extends AbstractUsageEventListener {

    private static final int MAX_TIME_SINCE_EVENT = 14400000;
    // 20 is the event max set by the GA API
    private static final int GA_MAX_EVENTS = 20;
    private static final String ANALYTICS_BATCH_ENDPOINT = "https://www.google-analytics.com/batch";
    private final static Logger log = LogManager.getLogger();
    private static String analyticsKey;
    private static CloseableHttpClient httpclient;
    private static Buffer buffer;
    private static ExecutorService executor;
    private static Future future;
    private static boolean destroyed = false;

    @Autowired(required = true)
    ConfigurationService configurationService;

    @Autowired(required = true)
    ClientInfoService clientInfoService;

    @PostConstruct
    public void init() {
        analyticsKey = configurationService.getProperty("google.analytics.key");
        if (StringUtils.isNotEmpty(analyticsKey)) {
            int analyticsBufferlimit = configurationService.getIntProperty("google.analytics.buffer.limit", 256);
            buffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(analyticsBufferlimit));
            httpclient = HttpClients.createDefault();
            executor = Executors.newSingleThreadExecutor();
            future = executor.submit(new GoogleAnalyticsTask());
        }
    }

    @Override
    public void receiveEvent(Event event) {
        if ((event instanceof UsageEvent)) {
            if (StringUtils.isNotEmpty(analyticsKey)) {
                UsageEvent ue = (UsageEvent) event;
                log.debug("Usage event received " + event.getName());
                try {
                    if (ue.getAction() == UsageEvent.Action.VIEW &&
                            ue.getObject().getType() == Constants.BITSTREAM) {

                        // Client ID, should uniquely identify the user or device. If we have an X-CORRELATION-ID
                        // header or a session ID for the user, then lets use it, othwerwise generate a UUID.
                        String cid;
                        if (ue.getRequest().getHeader("X-CORRELATION-ID") != null) {
                            cid = ue.getRequest().getHeader("X-CORRELATION-ID");
                        } else if (ue.getRequest().getSession(false) != null) {
                            cid = ue.getRequest().getSession().getId();
                        } else {
                            cid = UUID.randomUUID().toString();
                        }
                        // Prefer the X-REFERRER header, otherwise falback to the referrer header
                        String referrer;
                        if (ue.getRequest().getHeader("X-REFERRER") != null) {
                            referrer = ue.getRequest().getHeader("X-REFERRER");
                        } else {
                            referrer = ue.getRequest().getHeader("referer");
                        }
                        buffer.add(new GoogleAnalyticsEvent(cid, clientInfoService.getClientIp(ue.getRequest()),
                                ue.getRequest().getHeader("USER-AGENT"), referrer,
                                ue.getRequest() .getRequestURI() + "?" + ue.getRequest().getQueryString(),
                                getObjectName(ue), System.currentTimeMillis()));
                    }
                } catch (Exception e) {
                    log.error("Failed to add event to buffer", e);
                    log.error("Event information: " + ue);
                    Context context = ue.getContext();
                    if (context != null) {
                        log.error("Context information:");
                        log.error("    Current User: " + context.getCurrentUser());
                        log.error("    Extra log info: " + context.getExtraLogInfo());
                        if (context.getEvents() != null && !context.getEvents().isEmpty()) {
                            for (int x = 1; x <= context.getEvents().size(); x++) {
                                log.error("    Context Event " + x + ": " + context.getEvents().get(x));
                            }
                        }
                    } else {
                        log.error("UsageEvent has no Context object");
                    }
                }
            }
        }
    }

    private String getObjectName(UsageEvent ue) {
        try {
            if (ue.getObject().getType() == Constants.BITSTREAM) {
                // For a bitstream download we really want to know the title of the owning item
                // rather than the bitstream name.
                return ContentServiceFactory.getInstance().getDSpaceObjectService(ue.getObject())
                        .getParentObject(ue.getContext(), ue.getObject()).getName();
            } else {
                return ue.getObject().getName();
            }
        } catch (SQLException e) {
            // This shouldn't merit interrupting the user's transaction so log the error and continue.
            log.error("Error in Google Analytics recording - can't determine ParentObjectName for bitstream " +
                    ue.getObject().getID(), e);
        }

        return null;

    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        destroyed = true;
        if (StringUtils.isNotEmpty(analyticsKey)) {
            future.cancel(true);
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    private static class GoogleAnalyticsTask implements Runnable {
        public void run() {
            while (!destroyed) {
                try {
                    boolean sleep = false;
                    StringBuilder request = null;
                    List<GoogleAnalyticsEvent> events = new ArrayList<>();
                    Iterator iterator = buffer.iterator();
                    for (int x = 0; x < GA_MAX_EVENTS && iterator.hasNext(); x++) {
                        GoogleAnalyticsEvent event = (GoogleAnalyticsEvent) iterator.next();
                        events.add(event);
                        if ((System.currentTimeMillis() - event.getTime()) < MAX_TIME_SINCE_EVENT) {
                            String download = "v=1" +
                                    "&tid=" + analyticsKey +
                                    "&cid=" + event.getCid() +
                                    "&t=event" +
                                    "&uip=" + URLEncoder.encode(event.getUip(), "UTF-8") +
                                    "&ua=" + URLEncoder.encode(event.getUa(), "UTF-8") +
                                    "&dr=" + URLEncoder.encode(event.getDr(), "UTF-8") +
                                    "&dp=" + URLEncoder.encode(event.getDp(), "UTF-8") +
                                    "&dt=" + URLEncoder.encode(event.getDt(), "UTF-8") +
                                    "&qt=" + (System.currentTimeMillis() - event.getTime()) +
                                    "&ec=bitstream" +
                                    "&ea=download" +
                                    "&el=item";
                            if (request == null) {
                                request = new StringBuilder(download);
                            } else {
                                request.append("\n").append(download);
                            }
                        }
                    }

                    if (request != null) {
                        HttpPost httpPost = new HttpPost(ANALYTICS_BATCH_ENDPOINT);
                        httpPost.setEntity(new StringEntity(request.toString()));
                        try (final CloseableHttpResponse response2 = httpclient.execute(httpPost)) {
                            // I can't find a list of what are acceptable responses,
                            // so I log the response but take no action.
                            log.debug("Google Analytics response is " + response2.getStatusLine());
                            // Cleanup processed events
                            buffer.removeAll(events);
                        } catch (IOException e) {
                            log.error("GA post failed", e);
                        }
                    } else {
                        sleep = true;
                    }

                    if (sleep) {
                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException e) {
                            log.debug("Interrupted; checking if we should stop");
                        }
                    }
                } catch (Throwable t) {
                    log.error("Unexpected error; aborting GA event recording", t);
                    Throwables.propagate(t);
                }
            }
            log.info("Stopping GA event recording");
        }
    }
}
