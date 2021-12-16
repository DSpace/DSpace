/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.spring.ConfigurationPropertySource;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Utility class that will initialize the DSpace Configuration on Spring startup.
 * Adapted from the class of the same name in {@code dspace-server-webapp}.
 * <P>
 * NOTE: MUST be loaded after DSpaceKernelInitializer, as it requires the kernel
 * is already initialized.
 * <P>
 * This initializer ensures that our DSpace Configuration is loaded into Spring's
 * list of {@code PropertySource}s very early in the Spring startup process.
 * That is important as it allows us to use DSpace configurations within
 * {@code @ConditionalOnProperty} annotations on beans, as well as {@code @Value}
 * annotations and XML bean definitions.
 */
public class DSpaceConfigurationInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        // Load DSpace Configuration service (requires kernel already initialized)
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        Configuration configuration = configurationService.getConfiguration();

        // Create an Apache Commons Configuration Property Source from our configuration
        ConfigurationPropertySource apacheCommonsConfigPropertySource =
            new ConfigurationPropertySource(configuration.getClass().getName(), configuration);

        // Prepend it to the Environment's list of PropertySources
        // NOTE: This is added *first* in the list so that settings in DSpace's
        // ConfigurationService *override* any default values in Spring's
        // application.properties (or similar).
        applicationContext.getEnvironment()
                .getPropertySources()
                .addFirst(apacheCommonsConfigPropertySource);
    }
}

