/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.servicemanager.spring;

import java.util.ArrayList;
import java.util.List;

import org.dspace.servicemanager.ServiceManagerSystem;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will allow us to put the configuration into beans as they are 
 * being created.  It also handles activator classes from the 
 * configuration.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static Logger log = LoggerFactory.getLogger(DSpaceBeanFactoryPostProcessor.class);

    private DSpaceConfigurationService configurationService;
    private ServiceManagerSystem parent;
    private boolean testMode = false;

    public DSpaceBeanFactoryPostProcessor(ServiceManagerSystem parent,
                                          DSpaceConfigurationService configurationService, boolean testMode) {
        if (parent == null || configurationService == null) {
            throw new IllegalArgumentException("parent and configuration service cannot be null");
        }
        this.configurationService = configurationService;
        this.parent = parent;
        this.testMode = testMode;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // force config service to be registered first
        beanFactory.registerSingleton(ConfigurationService.class.getName(), configurationService);
        beanFactory.registerSingleton(ServiceManagerSystem.class.getName(), parent);
    }

}
