/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.config;

import org.dspace.core.Utils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xoai.services.api.config.ConfigurationService;

public class DSpaceConfigurationService implements ConfigurationService {

    private org.dspace.services.ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Initialize the OAI Configuration Service
     */
    public DSpaceConfigurationService() {
        // Check the DSpace ConfigurationService for required OAI-PMH settings.
        // If they do not exist, set sane defaults as needed.

        // Per OAI Spec, "oai.identifier.prefix" should be the hostname / domain name of the site.
        // This configuration is needed by the [dspace]/config/crosswalks/oai/description.xml template, so if
        // unspecified we will dynamically set it to the hostname of the "dspace.ui.url" configuration.
        if (!configurationService.hasProperty("oai.identifier.prefix")) {
            configurationService.setProperty("oai.identifier.prefix",
                                             Utils.getHostName(configurationService.getProperty("dspace.ui.url")));
        }
    }


    @Override
    public String getProperty(String key) {
        return configurationService.getProperty(key);
    }

    @Override
    public String getProperty(String module, String key) {
        return configurationService.getProperty(module, key);
    }

    @Override
    public boolean getBooleanProperty(String module, String key, boolean defaultValue) {
        if (module == null) {
            return configurationService.getBooleanProperty(key, defaultValue);
        }

        // Assume "module" properties are always prefixed with the module name
        return configurationService.getBooleanProperty(module + "." + key, defaultValue);
    }
}
