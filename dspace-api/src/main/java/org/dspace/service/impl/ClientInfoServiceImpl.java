/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.service.impl;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Utils;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.util.IPTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ClientInfoService} that can provide information on DSpace client requests
 *
 * @author tom dot desair at gmail dot com
 */
public class ClientInfoServiceImpl implements ClientInfoService {

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final Logger log = LoggerFactory.getLogger(ClientInfoServiceImpl.class);

    private Boolean useProxiesEnabled;

    private ConfigurationService configurationService;

    /**
     * Sparse HashTable structure to hold IP address ranges of trusted proxies
     */
    private IPTable trustedProxies;

    @Autowired(required = true)
    public ClientInfoServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.trustedProxies = parseTrustedProxyRanges();
    }

    @Override
    public String getClientIp(HttpServletRequest request) {
        return getClientIp(request.getRemoteAddr(), request.getHeader(X_FORWARDED_FOR_HEADER));
    }

    @Override
    public String getClientIp(String remoteIp, String xForwardedForHeaderValue) {
        String ip = remoteIp;

        if (isUseProxiesEnabled()) {
            String xForwardedForIp = getXForwardedForIpValue(remoteIp, xForwardedForHeaderValue);

            if (StringUtils.isNotBlank(xForwardedForIp) && isRequestFromTrustedProxy(ip)) {
                ip = xForwardedForIp;
            }

        } else if (StringUtils.isNotBlank(xForwardedForHeaderValue)) {
            log.warn("X-Forwarded-For header sent from client, but useProxies is not enabled. " +
                         "To trust X-Forwarded-For headers, set useProxies=true.");
        }

        return ip;
    }

    @Override
    public boolean isUseProxiesEnabled() {
        if (useProxiesEnabled == null) {
            useProxiesEnabled = configurationService.getBooleanProperty("useProxies", true);
            log.info("Proxies (useProxies) enabled? " + useProxiesEnabled);
        }

        return useProxiesEnabled;
    }

    /**
     * Parse / Determine trusted proxies based on configuration. "Trusted" proxies are the IP addresses from which we'll
     * allow the X-FORWARDED-FOR header. We don't accept that header from any IP address, as the header could be used
     * to spoof/fake your IP address.
     * <P>
     * If "proxies.trusted.include_ui_ip = true" (which is the default), then we lookup the IP address(es) associated
     * with ${dspace.ui.url}, and append them to the list of trusted proxies. This is necessary to allow the Angular
     * UI server-side rendering (SSR) to send us the X-FORWARDED-FOR header, which it usually uses to specify the
     * original client IP address.
     * <P>
     * If "proxies.trusted.ipranges" configuration is specified, those IP addresses/ranges are also included in the
     * list of trusted proxies.
     * <P>
     * Localhost (127.0.0.1) is ALWAYS included in the list of trusted proxies
     *
     * @return IPTable of trusted IP addresses/ranges, or null if none could be found.
     */
    private IPTable parseTrustedProxyRanges() {
        String localhostIP = "127.0.0.1";
        IPTable ipTable = new IPTable();

        // Get list of trusted proxy IP ranges
        String[] trustedIpRanges = configurationService.getArrayProperty("proxies.trusted.ipranges");
        // Always append localhost (127.0.0.1) to the list of trusted proxies, if not already included
        if (!ArrayUtils.contains(trustedIpRanges, localhostIP)) {
            trustedIpRanges = ArrayUtils.add(trustedIpRanges, localhostIP);
        }
        try {
            // Load all IPs into our IP Table
            for (String ipRange : trustedIpRanges) {
                ipTable.add(ipRange);
            }
        } catch (IPTable.IPFormatException e) {
            log.error("Property 'proxies.trusted.ipranges' contains an invalid IP range", e);
        }

        // Is the UI IP address always trusted (default = true)
        boolean uiIsTrustedProxy = configurationService.getBooleanProperty("proxies.trusted.include_ui_ip", true);

        // As long as the UI is a trusted proxy, determine IP(s) of ${dspace.ui.url}
        if (uiIsTrustedProxy) {
            String uiUrl = configurationService.getProperty("dspace.ui.url");
            // Get any IP address(es) associated with our UI
            String[] uiIpAddresses = Utils.getIPAddresses(uiUrl);

            if (ArrayUtils.isNotEmpty(uiIpAddresses)) {
                try {
                    // Load all UI IPs into our IP Table
                    for (String ipRange : uiIpAddresses) {
                        ipTable.add(ipRange);
                    }
                } catch (IPTable.IPFormatException e) {
                    log.error("IP address lookup for dspace.ui.url={} was invalid and could not be added to trusted" +
                                  " proxies", uiUrl, e);
                }
            }
        }

        // If our IPTable is not empty, log the trusted proxies and return it
        if (!ipTable.isEmpty()) {
            log.info("Trusted proxies (configure via 'proxies.trusted.ipranges'): {}", ipTable);
            return ipTable;
        } else {
            return null;
        }
    }

    /**
     * Whether a request is from a trusted proxy or not. Only returns true if trusted proxies are specified
     * and the ipAddress is contained in those proxies. False in all other cases
     * @param ipAddress IP address to check for
     * @return true if trusted, false otherwise
     */
    public boolean isRequestFromTrustedProxy(String ipAddress) {
        try {
            return trustedProxies != null && trustedProxies.contains(ipAddress);
        } catch (IPTable.IPFormatException e) {
            log.error("Request contains invalid remote address", e);
            return false;
        }
    }

    /**
     * Get the first X-FORWARDED-FOR header value which does not match the IP or another proxy IP. This is the most
     * likely client IP address when proxies are in use.
     * <P>
     * NOTE: This method does NOT validate the X-FORWARDED-FOR header value is accurate, so be aware this header
     * could contain a spoofed value. Therefore, any code calling this method should verify the source is somehow
     * trusted, e.g. by using isRequestFromTrustedProxy() or similar.
     * @param remoteIp remote IP address
     * @param xForwardedForValue X-FORWARDED-FOR header value passed by that address
     * @return likely client IP address from X-FORWARDED-FOR header
     */
    private String getXForwardedForIpValue(String remoteIp, String xForwardedForValue) {
        String ip = null;

        /* This header is a comma delimited list */
        String headerValue = StringUtils.trimToEmpty(xForwardedForValue);
        for (String xfip : headerValue.split(",")) {
            xfip = xfip.trim();
            /* proxy itself will sometime populate this header with the same value in
               remote address. ordering in spec is vague, we'll just take the last
               not equal to the proxy
            */
            if (!StringUtils.equals(remoteIp, xfip) && StringUtils.isNotBlank(xfip)
                    // if we have trusted proxies, we'll assume that they are not the client IP
                    && !isRequestFromTrustedProxy(xfip)) {
                ip = xfip;
            }
        }

        return ip;
    }
}
