/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.google;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * User: Robin Taylor
 * Date: 14/08/2014
 * Time: 10:05
 *
 * Notify Google Analytics of... well anything we want really.
 * @deprecated Use org.dspace.google.GoogleAsyncEventListener instead
 */
@Deprecated
public class GoogleRecorderEventListener extends AbstractUsageEventListener {

    private String analyticsKey;
    private CloseableHttpClient httpclient;
    private String GoogleURL = "https://www.google-analytics.com/collect";
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(GoogleRecorderEventListener.class);

    protected ContentServiceFactory contentServiceFactory;
    protected ConfigurationService configurationService;
    protected ClientInfoService clientInfoService;

    public GoogleRecorderEventListener() {
        // httpclient is threadsafe so we only need one.
        httpclient = HttpClients.createDefault();
    }

    @Autowired
    public void setContentServiceFactory(ContentServiceFactory contentServiceFactory) {
        this.contentServiceFactory = contentServiceFactory;
    }

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Autowired
    public void setClientInfoService(ClientInfoService clientInfoService) {
        this.clientInfoService = clientInfoService;
    }

    @Override
    public void receiveEvent(Event event) {
        if ((event instanceof UsageEvent)) {
            log.debug("Usage event received " + event.getName());

            // This is a wee bit messy but these keys should be combined in future.
            analyticsKey = configurationService.getProperty("google.analytics.key");

            if (StringUtils.isNotBlank(analyticsKey)) {
                try {
                    UsageEvent ue = (UsageEvent) event;

                    if (ue.getAction() == UsageEvent.Action.VIEW) {
                        if (ue.getObject().getType() == Constants.BITSTREAM) {
                            logEvent(ue, "bitstream", "download");

                            //  Note: I've left this commented out code here to show how we could record page views
                            // as events,
                            //  but since they are already taken care of by the Google Analytics Javascript there is
                            // not much point.

                            //}  else if (ue.getObject().getType() == Constants.ITEM) {
                            //    logEvent(ue, "item", "view");
                            //}  else if (ue.getObject().getType() == Constants.COLLECTION) {
                            //    logEvent(ue, "collection", "view");
                            //}  else if (ue.getObject().getType() == Constants.COMMUNITY) {
                            //    logEvent(ue, "community", "view");
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    private void logEvent(UsageEvent ue, String category, String action) throws IOException, SQLException {
        HttpPost httpPost = new HttpPost(GoogleURL);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("v", "1"));
        nvps.add(new BasicNameValuePair("tid", analyticsKey));

        // Client Id, should uniquely identify the user or device. If we have a session id for the user
        // then lets use it, else generate a UUID.
        if (ue.getRequest().getSession(false) != null) {
            nvps.add(new BasicNameValuePair("cid", ue.getRequest().getSession().getId()));
        } else {
            nvps.add(new BasicNameValuePair("cid", UUID.randomUUID().toString()));
        }

        nvps.add(new BasicNameValuePair("t", "event"));
        nvps.add(new BasicNameValuePair("uip", getIPAddress(ue.getRequest())));
        nvps.add(new BasicNameValuePair("ua", ue.getRequest().getHeader("USER-AGENT")));
        nvps.add(new BasicNameValuePair("dr", ue.getRequest().getHeader("referer")));
        nvps.add(new BasicNameValuePair("dp", ue.getRequest().getRequestURI()));
        nvps.add(new BasicNameValuePair("dt", getObjectName(ue)));
        nvps.add(new BasicNameValuePair("ec", category));
        nvps.add(new BasicNameValuePair("ea", action));

        if (ue.getObject().getType() == Constants.BITSTREAM) {
            // Bitstream downloads may occasionally be for collection or community images, so we need to label them
            // with the parent object type.
            nvps.add(new BasicNameValuePair("el", getParentType(ue)));
        }

        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response2 = httpclient.execute(httpPost)) {
            // I can't find a list of what are acceptable responses, so I log the response but take no action.
            log.debug("Google Analytics response is " + response2.getStatusLine());
        }

        log.debug("Posted to Google Analytics - " + ue.getRequest().getRequestURI());
    }

    private String getParentType(UsageEvent ue) {
        try {
            int parentType = contentServiceFactory.getDSpaceObjectService(ue.getObject())
                                                  .getParentObject(ue.getContext(), ue.getObject()).getType();
            if (parentType == Constants.ITEM) {
                return "item";
            } else if (parentType == Constants.COLLECTION) {
                return "collection";
            } else if (parentType == Constants.COMMUNITY) {
                return "community";
            }
        } catch (SQLException e) {
            // This shouldn't merit interrupting the user's transaction so log the error and continue.
            log.error(
                "Error in Google Analytics recording - can't determine ParentObjectType for bitstream " + ue.getObject()
                                                                                                            .getID());
            e.printStackTrace();
        }

        return null;
    }

    private String getObjectName(UsageEvent ue) {
        try {
            if (ue.getObject().getType() == Constants.BITSTREAM) {
                // For a bitstream download we really want to know the title of the owning item rather than the
                // bitstream name.
                return contentServiceFactory.getDSpaceObjectService(ue.getObject())
                                            .getParentObject(ue.getContext(), ue.getObject()).getName();
            } else {
                return ue.getObject().getName();
            }
        } catch (SQLException e) {
            // This shouldn't merit interrupting the user's transaction so log the error and continue.
            log.error(
                "Error in Google Analytics recording - can't determine ParentObjectName for bitstream " + ue.getObject()
                                                                                                            .getID());
            e.printStackTrace();
        }

        return null;

    }

    private String getIPAddress(HttpServletRequest request) {
        return clientInfoService.getClientIp(request);
    }

}
