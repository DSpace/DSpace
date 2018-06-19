/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
public final class DSpaceBeanPostProcessor implements BeanPostProcessor, DestructionAwareBeanPostProcessor {

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
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        // Before initializing the service, first configure it based on any related settings in the configurationService
        // NOTE: configs related to this bean MUST be prefixed with the bean's name (e.g. [beanName].setting = value)
        DSpaceServiceManager.configureService(beanName, bean, configurationService);
        return bean;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        DSpaceServiceManager.initService(bean);
        return bean;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor#postProcessBeforeDestruction(java.lang.Object, java.lang.String)
     */
    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        DSpaceServiceManager.shutdownService(bean);
    }

}
