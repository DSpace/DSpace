/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.factory;

import java.sql.SQLException;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestDetailsEnricherFactory {

/*
    // Matomo Custom Variable for OAI-PMH ID tracking
    Gson gson = new Gson();
    Map<String, String[]> jsonMatomoCustomVars = new HashMap<>();
    String[] oaipmhID =
        new String[]{"oaipmhID", "oai:" + dspaceHostName + ":" + item.getHandle()};
        jsonMatomoCustomVars.put("1", oaipmhID);
        builder.addParameter("cvar", gson.toJson(jsonMatomoCustomVars));
*/


    private MatomoRequestDetailsEnricherFactory() {}

    private static final Logger log = LogManager.getLogger(MatomoRequestDetailsEnricherFactory.class);

    public static MatomoRequestDetailsEnricher userAgentEnricher() {
        return (usageEvent, details) ->
            details.addParameter(
                "ua",
                StringUtils.defaultIfBlank(usageEvent.getRequest().getHeader("USER-AGENT"), "")
            );
    }

    public static MatomoRequestDetailsEnricher actionNameEnricher() {
        return (usageEvent, details) ->
            details.addParameter(
                "action_name",
                StringUtils.defaultIfBlank(actionName(usageEvent), "")
            );
    }

    public static MatomoRequestDetailsEnricher urlEnricher() {
        return (usageEvent, details) ->
            details.addParameter("url", url(usageEvent.getObject()));
    }

    public static MatomoRequestDetailsEnricher downloadEnricher() {
        return (usageEvent, details) ->
            details.addParameter(
                "download",
                Optional.ofNullable(usageEvent.getObject())
                        .filter(dso -> dso.getType() == Constants.BITSTREAM)
                        .map(MatomoRequestDetailsEnricherFactory::url)
                        .filter(StringUtils::isNotEmpty)
                        .map(url -> url + "/download")
                        .orElse("")
            );
    }

    private static String url(DSpaceObject dso) {
        if (dso == null) {
            return "";
        }
        String baseUrl = dspaceUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return switch (dso.getType()) {
            case Constants.BITSTREAM -> baseUrl + "/bitstreams/" + dso.getID();
            case Constants.ITEM -> baseUrl + "/items/" + dso.getID();
            default -> "";
        };
    }

    private static String dspaceUrl() {
        return DSpaceServicesFactory.getInstance()
                                    .getConfigurationService().getProperty("dspace.ui.url");
    }

    private static String actionName(UsageEvent ue) {
        try {
            if (ue.getObject().getType() == Constants.BITSTREAM) {
                // For a bitstream download we really want to know the title of the owning item
                // rather than the bitstream name.
                return ContentServiceFactory.getInstance()
                                            .getDSpaceObjectService(ue.getObject())
                                            .getParentObject(ue.getContext(), ue.getObject())
                                            .getName();
            } else {
                return ue.getObject().getName();
            }
        } catch (SQLException e) {
            // This shouldn't merit interrupting the user's transaction so log the error and continue.
            log.error("Error in Google Analytics recording - can't determine ParentObjectName for bitstream " +
                      ue.getObject().getID(), e);
        }

        return null;

    }
}
