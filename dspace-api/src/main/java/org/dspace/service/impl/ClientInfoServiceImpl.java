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
        this.trustedProxies = parseTrustedProxyRanges(
                configurationService.getArrayProperty("proxies.trusted.ipranges"));
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
            log.info("useProxies=" + useProxiesEnabled);
        }

        return useProxiesEnabled;
    }

    private IPTable parseTrustedProxyRanges(String[] proxyProperty) {
        if (ArrayUtils.isEmpty(proxyProperty)) {
            return null;
        } else {
            //Load all supplied proxy IP ranges into the IP table
            IPTable ipTable = new IPTable();
            try {
                for (String proxyRange : proxyProperty) {
                    ipTable.add(proxyRange);
                }
            } catch (IPTable.IPFormatException e) {
                log.error("Property proxies.trusted.ipranges contains an invalid IP range", e);
                ipTable = null;
            }

            return ipTable;
        }
    }

    private boolean isRequestFromTrustedProxy(String ipAddress) {
        try {
            return trustedProxies == null || trustedProxies.contains(ipAddress);
        } catch (IPTable.IPFormatException e) {
            log.error("Request contains invalid remote address", e);
            return false;
        }
    }

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
                    //if we have trusted proxies, we'll assume that they are not the client IP
                    && (trustedProxies == null || !isRequestFromTrustedProxy(xfip))) {

                ip = xfip.trim();
            }
        }

        return ip;
    }
}
