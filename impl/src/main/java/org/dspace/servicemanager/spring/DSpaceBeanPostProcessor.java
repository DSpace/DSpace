/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.servicemanager.spring;

import org.dspace.servicemanager.DSpaceServiceManager;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;

/**
 * This processes beans as they are loaded into the system by spring.
 * Allows us to handle the init method and also push config options.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class DSpaceBeanPostProcessor implements BeanPostProcessor, DestructionAwareBeanPostProcessor {

    private DSpaceConfigurationService configurationService;
    @Autowired
    public DSpaceBeanPostProcessor(DSpaceConfigurationService configurationService) {
        if (configurationService == null) {
            throw new IllegalArgumentException("configuration service cannot be null");
        }
        this.configurationService = configurationService;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        DSpaceServiceManager.configureService(beanName, bean, configurationService.getServiceNameConfigs());
        return bean;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
     */
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        DSpaceServiceManager.initService(bean);
        return bean;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor#postProcessBeforeDestruction(java.lang.Object, java.lang.String)
     */
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        DSpaceServiceManager.shutdownService(bean);
    }

}
