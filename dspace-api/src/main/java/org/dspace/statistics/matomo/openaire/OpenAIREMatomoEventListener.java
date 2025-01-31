/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.matomo.openaire;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;

public class OpenAIREMatomoEventListener extends AbstractUsageEventListener {
    private static final Logger log = LogManager.getLogger(OpenAIREMatomoEventListener.class);
    private ObjectMapper objectMapper;
    private boolean matomoIsEnabled;
    private String matomoUrl;
    private String matomoSiteID;
    private String matomoAuthToken;
    private int matomoBulkRequestSize;
    private int matomoIPAnonymizationBytes;
    private String dspaceURL;
    private boolean useProxies;
    private CloseableHttpClient httpClient;
    private final Queue<Map<String, String>> queue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        matomoIsEnabled = configService.getBooleanProperty("matomo.openaire.analytics.enabled", false);
        matomoUrl = configService.getProperty("matomo.openaire.analytics.trackerURL");
        matomoSiteID = configService.getProperty("matomo.openaire.analytics.siteID");
        matomoAuthToken = configService.getProperty("matomo.openaire.analytics.authToken");
        matomoBulkRequestSize = configService.getIntProperty("matomo.openaire.analytics.bulk_request_size");
        matomoIPAnonymizationBytes = configService.getIntProperty("matomo.openaire.analytics.ipAnonymizationBytes");

        dspaceURL = configService.getProperty("dspace.ui.url");
        useProxies = configService.getPropertyAsType("useProxies", false);
        httpClient = HttpClients.createDefault();
    }

    @Override
    public void receiveEvent(final Event event) {
        if (!(event instanceof UsageEvent) || !matomoIsEnabled || httpClient == null) {
            return;
        }

        UsageEvent ue = (UsageEvent) event;
        if (ue.getAction() == UsageEvent.Action.VIEW) {
            processViewEvent(ue);
        }
    }

    private void processViewEvent(UsageEvent ue) {
        try {
            if (ue.getObject().getType() == Constants.BITSTREAM) {
                processBitstreamView(ue);
            } else if (ue.getObject().getType() == Constants.ITEM) {
                processItemView((Item) ue.getObject(), ue.getRequest());
            }
        } catch (URISyntaxException e) {
            log.error("Error constructing OpenAIRE request URL", e);
        }
    }

    private void processBitstreamView(UsageEvent ue) {
        Bitstream bitstream = (Bitstream) ue.getObject();
        try {
            if (!bitstream.getBundles().isEmpty()) {
                Bundle bundle = bitstream.getBundles().get(0);
                if (!bundle.getItems().isEmpty()) {
                    Item item = bundle.getItems().get(0);
                    logEvent(item, bitstream, ue.getRequest());
                }
            }
        } catch (SQLException | URISyntaxException e) {
            throw new RuntimeException("Error in processing bitstream view " + e.getMessage());
        }
    }

    private void processItemView(Item item, HttpServletRequest request) throws URISyntaxException {
        logEvent(item, null, request);
    }

    private void logEvent(Item item, Bitstream bitstream, HttpServletRequest request) throws URISyntaxException {
        buildMatomoRequest(item, bitstream, request);
        if (queue.size() >= matomoBulkRequestSize) {
            sendBulkRequest();
        }
    }

    private synchronized void buildMatomoRequest(Item item, Bitstream bitstream, HttpServletRequest httpRequest) {
        Map<String, String> matomoRequest = new HashMap<>();
        matomoRequest.put("idsite", matomoSiteID);
        matomoRequest.put("cip", this.getIPAddress(httpRequest));

        // Country information in case of IPAnonymization
        if (matomoIPAnonymizationBytes > 0 && matomoIPAnonymizationBytes < 4) {
            String country = "";
            try {
                Locale locale = httpRequest.getLocale();
                country = locale.getCountry();
            } catch (Exception e) {
                log.error("Cannot get locale", e);
            }
            matomoRequest.put("country", country);
        }

        matomoRequest.put("rec", "1");
        matomoRequest.put("action_name", item.getName());
        matomoRequest.put("ua", StringUtils.defaultIfBlank(httpRequest.getHeader("USER-AGENT"), ""));
        matomoRequest.put("urlref", StringUtils.defaultIfBlank(httpRequest.getHeader("referer"), ""));

        String trackingUrl = dspaceURL + (bitstream != null
                ? "/bitstreams/" + bitstream.getID() + "/download"
                : "/items/" + item.getID());
        matomoRequest.put("url", trackingUrl);
        if (bitstream != null) {
            matomoRequest.put("download", trackingUrl);
        }

        Map<String, String[]> customVars = new HashMap<>();
        customVars.put("1", new String[]{"oaipmhID", "oai:" + dspaceURL + ":" + item.getHandle()});
        matomoRequest.put("cvar", new Gson().toJson(customVars));

        queue.add(matomoRequest);
    }

    private void sendBulkRequest() {
        if (queue.isEmpty()) {
            return;
        }

        List<Map<String, String>> bulkRequests = new ArrayList<>();
        while (!queue.isEmpty() && bulkRequests.size() < matomoBulkRequestSize) {
            bulkRequests.add(queue.poll());
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("requests", bulkRequests);
        requestBody.put("token_auth", matomoAuthToken);

        try {
            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpPost request = new HttpPost(matomoUrl);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonPayload));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.info("Matomo Response: {}", jsonResponse);
            }
        } catch (IOException e) {
            log.error("request to Matomo failed: {}", e.getMessage());
        }
    }

    /**
     * Get the IP-Address from the given request. Handles cases where a Proxy is
     * involved and IP-Address anonymization. Not yet working with IPv6
     */
    private String getIPAddress(final HttpServletRequest request) {
        String clientIP = request.getRemoteAddr();
        if (useProxies && request.getHeader("X-Forwarded-For") != null) {
            for (String xfip : request.getHeader("X-Forwarded-For").split(",")) {
                /*
                 * proxy itself will sometime populate this header with the same
                 * value in remote address. ordering in spec is vague, we'll
                 * just take the last not equal to the proxy
                 */
                if (!request.getHeader("X-Forwarded-For").contains(clientIP)) {
                    clientIP = xfip.trim();
                }
            }
        }

        // IP anonymization case
        if (matomoIPAnonymizationBytes > 0 && matomoIPAnonymizationBytes < 4) {
            // Check IPv4 or IPv6
            InetAddress ipadress = null;
            try {
                ipadress = InetAddress.getByName(clientIP);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            if (ipadress instanceof Inet6Address) {
                clientIP = "0.0.0.0";
            } else {
                switch (matomoIPAnonymizationBytes) {
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
                                "Invalid IP bytes: " + matomoIPAnonymizationBytes);
                }
            }
        }

        return clientIP;
    }
}
