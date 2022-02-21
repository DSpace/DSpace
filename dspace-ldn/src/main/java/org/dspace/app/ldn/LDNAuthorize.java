/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
@Component("LDNAuthorize")
public class LDNAuthorize {

    private static final Logger log = LogManager.getLogger(LDNAuthorize.class);

    private static final String HEADER_X_REAL_IP = "x-real-ip";

    private static final String HEADER_X_FORWARDED_FOR = "x-forwarded-for";

    private final String[] authorized;

    @Autowired
    private HttpServletRequest request;

    public LDNAuthorize(ConfigurationService configurationService) {
        this.authorized = configurationService.getArrayProperty("ldn.trusted");
        log.info("Authorized for LDN: {}", Arrays.toString(this.authorized));
    }

    public boolean isAllowed() throws UnknownHostException {
        String ip = request.getRemoteAddr();
        log.debug("Request ip: " + ip);

        String realIp = request.getHeader(HEADER_X_REAL_IP);
        log.debug("Request real ip: " + realIp);

        String forwardForIps = request.getHeader(HEADER_X_FORWARDED_FOR);
        log.debug("Request forwarded for ip: " + forwardForIps);

        boolean allowed = false;

        for (String entry : authorized) {
            if (ip != null && entry.trim().equals(ip.trim())) {
                log.info("Allowing ip " + entry + " for " + request.getRequestURI());
                allowed = true;
                break;
            }
            if (realIp != null && entry.trim().equals(realIp.trim())) {
                log.info("Allowing real ip " + realIp + " for " + request.getRequestURI());
                allowed = true;
                break;
            }
            if (forwardForIps != null && forwardForIps.contains(entry.trim())) {
                log.info("Allowing forwarded for ips " + forwardForIps + " for " + request.getRequestURI());
                allowed = true;
                break;
            }
        }

        String realHost = null;
        String forwardForHosts = null;

        if (!allowed) {
            if (realIp != null) {
                realHost = getHostFromIp(realIp);
                if (realHost != null) {
                    log.info("Request real host: " + realHost);
                }
            }

            for (String entry : authorized) {
                if (realHost != null && entry.trim().equals(realHost.trim())) {
                    log.info("Allowing real host " + realHost + " for " + request.getRequestURI());
                    allowed = true;
                    break;
                }
            }
        }

        if (!allowed) {
            if (forwardForIps != null) {
                forwardForHosts = getForwardedForHosts(forwardForIps);
            }

            for (String entry : authorized) {
                if (forwardForHosts != null && forwardForHosts.contains(entry.trim())) {
                    log.info("Allowing forwarded for hosts " + forwardForHosts + " for " + request.getRequestURI());
                    allowed = true;
                    break;
                }
            }
        }

        if (!allowed) {
            log.warn("Disallowing request for " + request.getRequestURI());
            if (ip != null) {
                log.warn("  ip: " + ip);
            }
            if (realIp != null) {
                log.warn("  real ip: " + realIp);
            }
            if (forwardForIps != null) {
                log.warn("  forwarded for IPs: " + forwardForIps);
            }
            if (realHost != null) {
                log.warn("  real host: " + realHost);
            }
            if (forwardForIps != null) {
                log.warn("  forwarded for IPs: " + forwardForIps);
            }
        }
        return allowed;
    }

    private String getHostFromIp(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.getHostName();
        } catch (UnknownHostException e) {
            log.warn("Unknown host for ip: " + ip);
        }
        return null;
    }

    private String getForwardedForHosts(String forwardForIps) {
        String forwardForHosts = "";
        Iterator<String> forwardForIpsIterator = Arrays.stream(forwardForIps.split(",")).iterator();
        while (forwardForIpsIterator.hasNext()) {
            String forwardForHost = getHostFromIp(forwardForIpsIterator.next());
            if (forwardForHost != null) {
                forwardForHosts += forwardForHost;
                if (forwardForIpsIterator.hasNext()) {
                    forwardForHosts += ",";
                }
            }
        }
        if (forwardForHosts.isEmpty()) {
            return null;
        } else {
            log.info("Request forwarded for hosts: " + forwardForHosts);
            return forwardForHosts;
        }
    }

}
