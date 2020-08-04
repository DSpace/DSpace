/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.service;

import javax.servlet.http.HttpServletRequest;

/**
 * Service that can be used to retrieve information about DSpace clients
 */
public interface ClientInfoService {

    /**
     * Get the client IP of this request taking into account the X-Forwarded-For header and the "useProxies" setting
     * @param request The client HTTP request
     * @return The IP address of the originating client
     */
    String getClientIp(HttpServletRequest request);

    /**
     * Get the client IP of this request taking into account the X-Forwarded-For header and the "useProxies" setting
     * @param remoteIp the remote address of the current request
     * @param xForwardedForHeaderValue The value of the X-Forwarded-For header
     * @return The IP address of the originating client
     */
    String getClientIp(String remoteIp, String xForwardedForHeaderValue);

    /**
     * Does DSpace take into account HTTP proxy headers or not
     * @return true if this is the case, false otherwise
     */
    boolean isUseProxiesEnabled();
}
