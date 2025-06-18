/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.client;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.dspace.services.ConfigurationService;

/**
 * Extension of {@link DefaultRoutePlanner} that determine the proxy based on
 * the configuration service, ignoring configured hosts.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DSpaceProxyRoutePlanner extends DefaultRoutePlanner {

    private ConfigurationService configurationService;

    public DSpaceProxyRoutePlanner(ConfigurationService configurationService) {
        super(null);
        this.configurationService = configurationService;
    }

    @Override
    protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        if (isTargetHostConfiguredToBeIgnored(target)) {
            return null;
        }
        String proxyHost = configurationService.getProperty("http.proxy.host");
        String proxyPort = configurationService.getProperty("http.proxy.port");
        if (StringUtils.isAnyBlank(proxyHost, proxyPort)) {
            return null;
        }
        try {
            return new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid proxy port configuration: " + proxyPort);
        }
    }

    private boolean isTargetHostConfiguredToBeIgnored(HttpHost target) {
        String[] hostsToIgnore = configurationService.getArrayProperty("http.proxy.hosts-to-ignore");
        if (ArrayUtils.isEmpty(hostsToIgnore)) {
            return false;
        }
        return Arrays.stream(hostsToIgnore)
                .anyMatch(host -> matchesHost(host, target.getHostName()));
    }

    private boolean matchesHost(String hostPattern, String hostName) {
        if (hostName.equals(hostPattern)) {
            return true;
        } else if (hostPattern.startsWith("*")) {
            return hostName.endsWith(StringUtils.removeStart(hostPattern, "*"));
        } else if (hostPattern.endsWith("*")) {
            return hostName.startsWith(StringUtils.removeEnd(hostPattern, "*"));
        }
        return false;
    }
}