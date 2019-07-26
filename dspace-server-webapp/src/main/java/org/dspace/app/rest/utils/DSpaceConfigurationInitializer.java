/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.spring.ConfigurationPropertySource;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Utility class that will initialize the DSpace Configuration on Spring Boot startup.
 * <P>
 * NOTE: MUST be loaded after DSpaceKernelInitializer, as it requires the kernel is already initialized.
 * <P>
 * This initializer ensures that our DSpace Configuration is loaded into Spring's list of PropertySources
 * very early in the Spring Boot startup process. That is important as it allows us to use DSpace configurations
 * within @ConditionalOnProperty annotations on beans, as well as @Value annotations and XML bean definitions.
 * <P>
 * Used by org.dspace.app.rest.Application
 */
public class DSpaceConfigurationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(DSpaceConfigurationInitializer.class);

    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        // Load DSpace Configuration service (requires kernel already initialized)
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        Configuration configuration = configurationService.getConfiguration();

        // Create an Apache Commons Configuration Property Source from our configuration
        ConfigurationPropertySource apacheCommonsConfigPropertySource =
            new ConfigurationPropertySource(configuration.getClass().getName(), configuration);

        // Append it to the Environment's list of PropertySources
        applicationContext.getEnvironment().getPropertySources().addLast(apacheCommonsConfigPropertySource);
    }
}

