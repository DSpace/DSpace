/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

/**
 * This class setup the basic attributes in Mapped Diagnostic Context useful for
 * trouble-shooting of the DSpace Server Webapp and make sure to include a first
 * log entry for all the request nothing which is the referer.
 * 
 * This logging filter can be modified at runtime altering the value of the
 * logging.server.* configuration properties to include more details about the
 * incoming request (payload, headers, query string, client info)
 * 
 * The MDC attributes are as follow: - an unique randomly generated UUID is
 * assigned to every request (requestID) - the correlation ID provided by
 * friendly client applications (such as our angular UI), if specified as
 * X-Correlation-ID (correlationID)
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class DSpaceAPIRequestLoggingFilter extends AbstractRequestLoggingFilter {
    @Autowired
    private ConfigurationService configurationService;

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        return true;
    }

    @Override
    protected boolean isIncludePayload() {
        return configurationService.getBooleanProperty("logging.server.include-payload", false);
    }

    @Override
    protected boolean isIncludeHeaders() {
        return configurationService.getBooleanProperty("logging.server.include-headers", false);
    }

    @Override
    protected boolean isIncludeQueryString() {
        return configurationService.getBooleanProperty("logging.server.include-query-string", false);
    }

    @Override
    protected boolean isIncludeClientInfo() {
        return configurationService.getBooleanProperty("logging.server.include-client-info", false);
    }

    @Override
    protected int getMaxPayloadLength() {
        return configurationService.getIntProperty("logging.server.max-payload-length", 10000);
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        ThreadContext.put("requestID", UUID.randomUUID().toString()); // Add the fishtag;
        String clientID = request.getHeader("x-correlation-id");
        if (StringUtils.isBlank(clientID)) {
            clientID = "unknown";
        }
        ThreadContext.put("correlationID", clientID);
        String referrer = request.getHeader("x-referrer");
        if (StringUtils.isBlank(referrer)) {
            referrer = request.getHeader("referer");
            if (StringUtils.isBlank(referrer)) {
                referrer = "unknown";
            }
        }
        logger.info(message + " originated from " + referrer);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        if (isAfterRequestLoggingEnabled()) {
            logger.info(message);
        }
        ThreadContext.clearAll();
    }

    private boolean isAfterRequestLoggingEnabled() {
        return configurationService.getBooleanProperty("logging.server.include-after-request");
    }
}
