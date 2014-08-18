/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.google.recording;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.utils.DSpace;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import org.apache.log4j.Logger;


/**
 * User: Robin Taylor
 * Date: 14/08/2014
 * Time: 10:05
 */
public class GoogleRecorder {

    private volatile static GoogleRecorder uniqueInstance;
    private CloseableHttpClient httpclient;

    private static Logger log = Logger.getLogger(GoogleRecorder.class);

    private GoogleRecorder() {
        httpclient = HttpClients.createDefault();
    }

    public static GoogleRecorder getInstance() {
        if (uniqueInstance == null) {
            synchronized (GoogleRecorder.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new GoogleRecorder();
                }
            }
        }

        return uniqueInstance;
    }

    /**
     * Notify Google Analytics of a bitstream download
     *
     * @param requestURI
     * @param bitstreamMimeType
     * @throws IOException
     */
    public void recordJSPUIBitstreamDownload(String requestURI, String bitstreamMimeType) throws IOException {
        String analyticsKey = new DSpace().getConfigurationService().getProperty("jspui.google.analytics.key");

        if (analyticsKey != null ) {
            // Comment - I thought about sticking this in config but it is already hardcoded elsewhere so I went
            // for consistency.
            HttpPost httpPost = new HttpPost("https://www.google-analytics.com/collect");

            // Question - what values to post? I've tried to record values that we might want to subsequently
            // query on. Any other suggestions?
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("v", "1"));
            nvps.add(new BasicNameValuePair("tid", analyticsKey));
            //Note: cid is not relevant here but is a required field.
            nvps.add(new BasicNameValuePair("cid", "999"));
            nvps.add(new BasicNameValuePair("t", "event"));
            nvps.add(new BasicNameValuePair("dp", requestURI));
            nvps.add(new BasicNameValuePair("ec", "bitstream"));
            nvps.add(new BasicNameValuePair("ea", "download"));
            nvps.add(new BasicNameValuePair("el", bitstreamMimeType));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            try (CloseableHttpResponse response2 = httpclient.execute(httpPost)) {
                // I can't find a list of what are acceptable responses, so I log the response but take no action.
                log.debug("Google Analytics response is " + response2.getStatusLine());
            }

            log.debug("Posted to Google Analytics - " + requestURI);
        }
    }

    /**
     * Notify Google Analytics of a bitstream download
     *
     * @param requestURI
     * @param bitstreamMimeType
     * @throws IOException
     */
    public void recordXMLUIBitstreamDownload(String requestURI, String bitstreamMimeType) throws IOException {
        String analyticsKey = new DSpace().getConfigurationService().getProperty("xmlui.google.analytics.key");

        if (analyticsKey != null ) {
            // Comment - I thought about sticking this in config but it is already hardcoded elsewhere so I went
            // for consistency.
            HttpPost httpPost = new HttpPost("https://www.google-analytics.com/collect");

            // Question - what values to post? I've tried to record values that we might want to subsequently
            // query on. Any other suggestions?
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("v", "1"));
            nvps.add(new BasicNameValuePair("tid", analyticsKey));
            //Note: cid is not relevant here but is a required field.
            nvps.add(new BasicNameValuePair("cid", "999"));
            nvps.add(new BasicNameValuePair("t", "event"));
            nvps.add(new BasicNameValuePair("dp", requestURI));
            nvps.add(new BasicNameValuePair("ec", "bitstream"));
            nvps.add(new BasicNameValuePair("ea", "download"));
            nvps.add(new BasicNameValuePair("el", bitstreamMimeType));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));

            try (CloseableHttpResponse response2 = httpclient.execute(httpPost)) {
                // I can't find a list of what are acceptable responses, so I log the response but take no action.
                log.debug("Google Analytics response is " + response2.getStatusLine());
            }

            log.debug("Posted to Google Analytics - " + requestURI);
        }
    }
}
