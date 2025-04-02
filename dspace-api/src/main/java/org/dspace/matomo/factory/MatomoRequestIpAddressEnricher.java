/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import org.apache.commons.lang3.StringUtils;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.service.ClientInfoService;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adds the IP address of the client to the {@code MatomoRequestDetails}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestIpAddressEnricher implements MatomoRequestDetailsEnricher {

    private final ClientInfoService clientInfoService;

    public MatomoRequestIpAddressEnricher(@Autowired ClientInfoService clientInfoService) {
        this.clientInfoService = clientInfoService;
    }

    @Override
    public MatomoRequestDetails enrich(UsageEvent usageEvent, MatomoRequestDetails matomoRequestDetails) {
        String clientIp = "";
        if (usageEvent != null && usageEvent.getRequest() != null) {
            clientIp = clientInfoService.getClientIp(usageEvent.getRequest());
        }
        return matomoRequestDetails.addParameter("cip", StringUtils.defaultIfEmpty(clientIp, ""));
    }
}
