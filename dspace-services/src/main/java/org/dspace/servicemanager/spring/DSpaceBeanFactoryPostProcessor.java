/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.spring;

import org.dspace.servicemanager.ServiceManagerSystem;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * This will allow us to put the configuration into beans as they are
 * being created.  It also handles activator classes from the
 * configuration.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class DSpaceBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private ServiceManagerSystem serviceManager;
    private DSpaceConfigurationService configurationService;
    private boolean testMode = false;

    public DSpaceBeanFactoryPostProcessor(ServiceManagerSystem serviceManager,
            DSpaceConfigurationService configurationService, boolean testMode) {
        if (configurationService == null) {
            throw new IllegalArgumentException("Configuration service cannot be null");
        }
        this.serviceManager = serviceManager;
        this.configurationService = configurationService;
        this.testMode = testMode;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org
     * .springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // force config service to be registered first
        beanFactory.registerSingleton(ConfigurationService.class.getName(), configurationService);
        beanFactory.registerSingleton(ServiceManagerSystem.class.getName(), serviceManager);
    }

}
