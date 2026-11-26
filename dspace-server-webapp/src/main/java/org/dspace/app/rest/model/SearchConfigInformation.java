/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Class storing discovery configuration search information
 * The purpose is to share the information with repositories/converters to parse the discovery configuration in a
 * centralised place
 */
public class SearchConfigInformation {
    private String configurationId;
    private String uuid;

    public static SearchConfigInformation fromRequest(HttpServletRequest request) {
        SearchConfigInformation information = new SearchConfigInformation();
        Map<String, String> pathVariables =
            (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        information.setConfigurationId(pathVariables.get("id"));
        information.setUuid(request.getParameter("uuid"));
        return information;
    }

    /**
     * Resolve the {@link DiscoveryConfiguration} from the discovery ID and potential scope UUID
     * If the discovery ID is "scope", the UUID is expected to be present from the request parameter, from which
     * an object will be resolved and their discovery configuration retrieved
     * If no configuration can be found by ID or scope, "default" is returned
     */
    public DiscoveryConfiguration getConfiguration(Context context, ScopeResolver scopeResolver,
                                                   DiscoveryConfigurationService searchConfigurationService) {
        DiscoveryConfiguration discoveryConfiguration = null;
        if (configurationId.equals("scope")) {
            IndexableObject scopeObject = scopeResolver.resolveScope(context, uuid);
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(context, scopeObject);
        } else {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration(configurationId);
        }

        if (discoveryConfiguration == null) {
            discoveryConfiguration = searchConfigurationService.getDiscoveryConfiguration("default");
        }

        return discoveryConfiguration;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
