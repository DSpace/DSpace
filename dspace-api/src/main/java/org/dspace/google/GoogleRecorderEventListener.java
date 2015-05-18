/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.google;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * User: Robin Taylor
 * Date: 14/08/2014
 * Time: 10:05
 *
 * Notify Google Analytics of... well anything we want really.
 *
 */
public class GoogleRecorderEventListener extends AbstractUsageEventListener {

    private String analyticsKey;
    private CloseableHttpClient httpclient;
    private String GoogleURL = "https://www.google-analytics.com/collect";
    private static Logger log = Logger.getLogger(GoogleRecorderEventListener.class);


    public GoogleRecorderEventListener() {
        // httpclient is threadsafe so we only need one.
        httpclient = HttpClients.createDefault();
    }

    public void receiveEvent(Event event) {
        if((event instanceof UsageEvent))
        {
            log.debug("Usage event received " + event.getName());

            // This is a wee bit messy but these keys should be combined in future.
            analyticsKey = new DSpace().getConfigurationService().getProperty("jspui.google.analytics.key");
            if (analyticsKey == null ) {
                analyticsKey = new DSpace().getConfigurationService().getProperty("xmlui.google.analytics.key");
            }

            if (analyticsKey != null ) {
                try {
                    UsageEvent ue = (UsageEvent)event;
                    if(UsageEvent.Action.VIEW == ue.getAction() && (Constants.BITSTREAM == ue.getObject().getType())) {
                        bitstreamDownload(ue);
                    }
                }
                catch(Exception e)
                {
                    log.error(e.getMessage());
                }
            }
        }
    }

    private void bitstreamDownload(UsageEvent ue) throws IOException {
        HttpPost httpPost = new HttpPost(GoogleURL);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("v", "1"));
        nvps.add(new BasicNameValuePair("tid", analyticsKey));
        nvps.add(new BasicNameValuePair("cid", "999"));
        nvps.add(new BasicNameValuePair("t", "event"));
        nvps.add(new BasicNameValuePair("dp", ue.getRequest().getRequestURI()));
        nvps.add(new BasicNameValuePair("ec", "bitstream"));
        nvps.add(new BasicNameValuePair("ea", "download"));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response2 = httpclient.execute(httpPost)) {
            // I can't find a list of what are acceptable responses, so I log the response but take no action.
            log.debug("Google Analytics response is " + response2.getStatusLine());
        }

        log.debug("Posted to Google Analytics - " + ue.getRequest().getRequestURI());
    }

}
