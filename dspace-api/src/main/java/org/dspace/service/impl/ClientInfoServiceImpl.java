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
            log.warn(
                    "X-Forwarded-For header detected but useProxiesEnabled is not enabled. " +
                            "If your dspace is behind a proxy set it to true");
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
     * If "proxies.trusted.ipranges" configuration is specified, we trust ONLY those IP addresses.
     * <P>
     * If "proxies.trusted.ipranges" is UNSPECIFIED, we only trust the IP address(es) associated with ${dspace.ui.url}.
     * This is necessary to allow the Angular UI server-side rendering (SSR) to send us the X-FORWARDED-FOR header,
     * which it usually uses to specify the original client IP address.
     * @return IPTable of trusted IP addresses/ranges, or null if none could be found.
     */
    private IPTable parseTrustedProxyRanges() {
        IPTable ipTable = null;
        // Whether we had to look up the UI's IP address (based on its URL), or not
        boolean uiUrlLookup = false;

        String[] ipAddresses = configurationService.getArrayProperty("proxies.trusted.ipranges");
        String uiUrl = configurationService.getProperty("dspace.ui.url");

        // If configuration is empty, determine IPs of ${dspace.ui.url} as the default trusted proxy
        if (ArrayUtils.isEmpty(ipAddresses)) {
            // Get any IP address(es) associated with our UI
            ipAddresses = Utils.getIPAddresses(uiUrl);
            uiUrlLookup = true;
        }

        // Only continue if we have IPs to trust.
        if (ArrayUtils.isNotEmpty(ipAddresses)) {
            ipTable = new IPTable();
            try {
                // Load all IPs into our IP Table
                for (String ipAddress : ipAddresses) {
                    ipTable.add(ipAddress);
                }
            } catch (IPTable.IPFormatException e) {
                if (uiUrlLookup) {
                    log.error("IP address found for dspace.ui.url={} was invalid", uiUrl, e);
                } else {
                    log.error("Property 'proxies.trusted.ipranges' contains an invalid IP range", e);
                }
                ipTable = null;
            }
        }

        if (ipTable != null) {
            log.info("Trusted proxy IP ranges/addresses: {}" + ipTable.toSet().toString());
        }

        return ipTable;
    }

    /**
     * Whether a request is from a trusted proxy or not. Only returns true if trusted proxies are specified
     * and the ipAddress is contained in those proxies. False in all other cases
     * @param ipAddress IP address to check for
     * @return true if trusted, false otherwise
     */
    private boolean isRequestFromTrustedProxy(String ipAddress) {
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
     * @param remoteIp remote IP address
     * @param xForwardedForValue X-FORWARDED-FOR header value passed by that address
     * @return likely client IP address from X-FORWARDED-FOR header
     */
    private String getXForwardedForIpValue(String remoteIp, String xForwardedForValue) {
        String ip = null;

        /* This header is a comma delimited list */
        String headerValue = StringUtils.trimToEmpty(xForwardedForValue);
        for (String xfip : headerValue.split(",")) {
            /* proxy itself will sometime populate this header with the same value in
               remote address. ordering in spec is vague, we'll just take the last
               not equal to the proxy
            */
            if (!StringUtils.equals(remoteIp, xfip) && StringUtils.isNotBlank(xfip)
                    // if we have trusted proxies, we'll assume that they are not the client IP
                    && !isRequestFromTrustedProxy(xfip)) {
                ip = xfip.trim();
            }
        }

        return ip;
    }
}
