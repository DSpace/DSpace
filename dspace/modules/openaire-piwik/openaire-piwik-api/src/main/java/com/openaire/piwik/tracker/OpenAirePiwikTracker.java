/*
 * OpenAirePiwikTracker.java
 *
 * Version: 0.2
 * Date: 2018-05-20
* Initial version @mire.  All rights reserved.
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
package com.openaire.piwik.tracker;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;

import com.google.gson.Gson;
/**
 * User: dpie (dpierrakos at gmail.com)
 * Date:
 * Time:
 */
public class OpenAirePiwikTracker extends AbstractUsageEventListener
{
    /* Log4j logger */
    private static Logger log = Logger.getLogger(OpenAirePiwikTracker.class);

    // Base URl of the Piwik platform
    private String piwikbaseUrl;

    // Piwik Site ID
    private String piwikSiteID;

    // Piwik IP Anonumization Bytes
    private int piwikIPAnonymizationBytes;

    // Piwik Site Authentication Token
    private String piwikTokenAuth;

    // Flag if Piwik is enabled for current installation. Might be disabled for
    // test instances e.g..
    private boolean piwikEnabled;

    // Flag if mising requests are stored in local DB for retry
    private boolean piwikRetry;

    // Flag if a proxy is in front of the DSpace instance
    private boolean useProxies;

    // The URL of this DSpace instance
    private String dspaceURL;

    // The host name of this DSpace instance
    private String dspaceHostName;

    // Async http client to prevent waiting for piwik server
    private CloseableHttpAsyncClient httpClient;

    // Pooling connection manager for httpClient
    private PoolingNHttpClientConnectionManager connectionManager;

    // The time out for a single connection if piwik is slow or unreachable.
    private static final int CONNECTION_TIMEOUT = 5 * 1000;

    // The number of connections per route
    private static final int NUMBER_OF_CONNECTIONS_PER_ROUTE = 100;

    // If there are more than MAX_NUMBER_OF_PENDING_CONNECTIONS waiting to be
    // served events wont be send to Piwik
    private static final int MAX_NUMBER_OF_PENDING_CONNECTIONS = 10;

    /**
     * Constructor to initialize the HTTP Client. We only need one per instance
     * as it is able to handle multiple requests by multiple Threads at once.
     * 
     */
    public OpenAirePiwikTracker()
    {
     try
        {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(CONNECTION_TIMEOUT)
                    .setSocketTimeout(CONNECTION_TIMEOUT).build();
            DefaultConnectingIOReactor ioreactor;
            ioreactor = new DefaultConnectingIOReactor();
            connectionManager = new PoolingNHttpClientConnectionManager(
                    ioreactor);
            connectionManager
                    .setDefaultMaxPerRoute(NUMBER_OF_CONNECTIONS_PER_ROUTE);
            httpClient = HttpAsyncClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setConnectionManager(connectionManager).build();

            httpClient.start();
        }
        catch (Exception e)
        {
            log.error(
                    "Piwik Tracker couldn't be initialized. There will be no tracking until server restart.",
                    e);
            httpClient = null;
        }
}

    /**
     * Read the Piwik configuration options
     */
    private void readConfiguration()
    {
        // Piwik variables
        piwikEnabled = ConfigurationManager.getBooleanProperty("oapiwik", "piwik.enabled");
        piwikbaseUrl = ConfigurationManager.getProperty("oapiwik", "piwik.trackerURL");
        piwikSiteID = ConfigurationManager.getProperty("oapiwik", "piwik.siteID");
        piwikTokenAuth = ConfigurationManager.getProperty("oapiwik","piwik.tokenAuth");

        piwikIPAnonymizationBytes = ConfigurationManager.getIntProperty("oapiwik","piwik.ipanonymizationbytes");

        piwikRetry = ConfigurationManager.getBooleanProperty("oapiwik", "piwik.retry");

        // DSpace variables
        useProxies =  ConfigurationManager.getBooleanProperty("useProxies");

        dspaceURL = ConfigurationManager.getProperty("dspace.url");
        dspaceHostName = ConfigurationManager.getProperty("dspace.hostname");

    }

    @Override
    public void receiveEvent(final Event event)
    {

        if (!(event instanceof UsageEvent))
        {
            return;
        }

        try
        {
            this.readConfiguration();
            if (!piwikEnabled || httpClient == null)
            {
                return;
            }

            if (connectionManager.getTotalStats()
                    .getPending() >= MAX_NUMBER_OF_PENDING_CONNECTIONS)
            {
                log.error(
                        "Event could not be sent to Piwik server due to insufficient available connections");
                return;
            }

            log.debug("Usage event received " + event.getName());

            UsageEvent ue = (UsageEvent) event;
            if (ue.getAction() == UsageEvent.Action.VIEW)
            {
                // Item Download Case
                if (ue.getObject().getType() == Constants.BITSTREAM)
                {
                    Bitstream bitstream = (Bitstream) ue.getObject();
                    if (bitstream.getBundles().length > 0)
                    {
                        Bundle bundle = bitstream.getBundles()[0];
                        if (bundle.getItems().length > 0)
                        {
                            Item item = bundle.getItems()[0];
                            this.logEvent(item, bitstream, ue.getRequest());
                        }
                    }
                }
                // Item View Case
                if (ue.getObject().getType() == Constants.ITEM)
                {
                    Item item = (Item) ue.getObject();
                    this.logEvent(item, null, ue.getRequest());
                }
            }

        }
        catch (Exception e)
        {
            log.error(e.getMessage());
        }
    }

    /**
     * Builds the URI to send the event to the configured Piwik instance and
     * sends the request.
     */
    private void logEvent(final Item item, final Bitstream bitstream,
        final HttpServletRequest request)
    throws IOException, URISyntaxException
    {
        URIBuilder builder = new URIBuilder();
        builder.setPath(piwikbaseUrl);
        builder.addParameter("idsite", piwikSiteID);
        builder.addParameter("cip", this.getIPAddress(request));
        builder.addParameter("rec", "1");
        builder.addParameter("token_auth", piwikTokenAuth);
        builder.addParameter("action_name", item.getName());

        // Agent Information
        String agent = StringUtils
        .defaultIfBlank(request.getHeader("USER-AGENT"), "");
        builder.addParameter("ua", agent);

        // Referer Information
        String urlref = StringUtils.defaultIfBlank(request.getHeader("referer"),
            "");
        builder.addParameter("urlref", urlref);

        // Country information in case of IPAnonymization
        if (piwikIPAnonymizationBytes > 0 && piwikIPAnonymizationBytes < 4)
        {
            String country = "";
            try
            {
                Locale locale = request.getLocale();
                country = locale.getCountry();
            }
            catch (Exception e)
            {
                log.error("Cannot get locale", e);
            }
            builder.addParameter("country", country);
        }

        if (bitstream != null)
        {
            // Bitstream information in case of download event
            StringBuffer sb = new StringBuffer(dspaceURL);
            sb.append("/bitstream/handle/").append(item.getHandle())
            .append("/");
            sb.append(bitstream.getName());
            builder.addParameter("url", sb.toString());
            builder.addParameter("download", sb.toString());

        }
        else
        {
            // Item information in case of Item view event
            builder.addParameter("url",
                dspaceURL + "/handle/" + item.getHandle());
        }

        // Piwik Custom Variable for OAI-PMH ID tracking
        Gson gson = new Gson();
        Map<String, String[]> jsonPiwikCustomVars = new HashMap<>();
        String[] oaipmhID = new String[] { "oaipmhID",
        "oai:" + dspaceHostName + ":" + item.getHandle() };
        jsonPiwikCustomVars.put("1", oaipmhID);
        builder.addParameter("cvar", gson.toJson(jsonPiwikCustomVars));

        this.sendRequest(builder.build());
    }

    /**
     * Get the IP-Address from the given request. Handles cases where a Proxy is
     * involved and IP-Address anonymization. Not yet working with IPv6
     * 
     * @param request
     * @return
     * @throws UnknownHostException
     */
    private String getIPAddress(final HttpServletRequest request)
    throws UnknownHostException
    {
        String clientIP = request.getRemoteAddr();
        if (useProxies && request.getHeader("X-Forwarded-For") != null)
        {
            /* This header is a comma delimited list */
            for (String xfip : request.getHeader("X-Forwarded-For").split(","))
            {
                /*
                 * proxy itself will sometime populate this header with the same
                 * value in remote address. ordering in spec is vague, we'll
                 * just take the last not equal to the proxy
                 */
                if (!request.getHeader("X-Forwarded-For").contains(clientIP))
                {
                    clientIP = xfip.trim();
                }
            }
        }

        // IP anonymization case

        if (piwikIPAnonymizationBytes > 0 && piwikIPAnonymizationBytes < 4)
        {

            // Check IPv4 or IPv6
            InetAddress ipadress = InetAddress.getByName(clientIP);
            if (ipadress instanceof Inet6Address)
            {
                clientIP = "0.0.0.0";
            }
            else
            {
                switch (piwikIPAnonymizationBytes)
                {
                    case 1:
                    clientIP = clientIP.substring(0,
                        StringUtils.ordinalIndexOf(clientIP, ".", 3))
                    + ".0";
                    break;
                    case 2:
                    clientIP = clientIP.substring(0,
                        StringUtils.ordinalIndexOf(clientIP, ".", 2))
                    + ".0.0";
                    break;
                    case 3:
                    clientIP = clientIP.substring(0,
                        StringUtils.ordinalIndexOf(clientIP, ".", 1))
                    + ".0.0.0";
                    break;
                    default:
                    throw new IllegalArgumentException(
                        "Invalid IP bytes: " + piwikIPAnonymizationBytes);
                }
            }
        }

        return clientIP;
    }

    /**
     * Send the request to the given URI. Ignores the result except
     * for a status code check.
     */
      private void sendRequest(final URI uri)
    {
        final HttpGet request = new HttpGet(uri);
        httpClient.execute(request, new FutureCallback<HttpResponse>()
        {
            @Override
            public void completed(final HttpResponse response)
            {
                if (response.getStatusLine()
                        .getStatusCode() == HttpStatus.SC_OK)
                {
                    log.info("Sent usage event to piwik");
                }
                else
                {
                    log.error("Error sending reqeust to Piwik." + " -> "
                            + response.getStatusLine());
                }
            }

            @Override
            public void failed(final Exception ex)
            {
                log.error("Error sending usage event to Piwik", ex);
                try
                {
                    if (piwikRetry)
                    {
                        OpenAirePiwikTrackerUnreported unreportedReq = new OpenAirePiwikTrackerUnreported();
                        unreportedReq.storeRequest(uri.toString());
                        log.info("Missing request stored to local DB");
                    }
                }
                catch (Exception e)
                {
                    log.error("Error storing unreported request");
                }
            }

            @Override
            public void cancelled()
            {
                log.info("Request cancelled");
            }

        });
    }

    protected void destroy() throws IOException
    {
        if (httpClient != null)
        {
            httpClient.close();
        }
    }

}
