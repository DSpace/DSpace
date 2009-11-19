/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager.spring;

import java.util.ArrayList;
import java.util.List;

import org.dspace.servicemanager.ServiceManagerSystem;
import org.dspace.servicemanager.ServiceMixinManager;
import org.dspace.servicemanager.config.DSpaceConfig;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will allow us to put the configuration into beans as they are being created,
 * it also handles activator classes from the configuration
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

        // register any beans which need to be registered now
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        // get out the list of all activator classes
        List<DSpaceConfig> allConfigs = configurationService.getConfiguration();
        List<DSpaceConfig> configs = new ArrayList<DSpaceConfig>();
        for (DSpaceConfig config : allConfigs) {
            if (config.isActivatorClass()) {
                configs.add(config);
            }
        }

        if (testMode) {
            log.info("Spring Service Manager running in test mode, no activators will be started");
        } else {
            // now register all autowire configured beans
            for (DSpaceConfig config : configs) {
                try {
                    Class<?> c = ServiceMixinManager.getClassByName(config.getActivatorClassName());
                    String autowire = config.getActivatorAutowire();
                    int autowireSpring = AbstractBeanDefinition.AUTOWIRE_AUTODETECT;
                    if ("none".equals(autowire)) {
                        autowireSpring = AbstractBeanDefinition.AUTOWIRE_NO;
                    } else if ("constructor".equals(autowire)) {
                        autowireSpring = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
                    } else if ("setter".equals(autowire)) {
                        autowireSpring = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
                    }
                    RootBeanDefinition beanDef = new RootBeanDefinition(c, autowireSpring);
                    beanDef.setScope(AbstractBeanDefinition.SCOPE_SINGLETON);
                    registry.registerBeanDefinition(config.getActivatorName(), beanDef);
                } catch (Exception e) {
                    log.error("Failed to register activator class from config: " + config + " :" + e, e);
                }
            }
        }
//        System.out.println("Registered beans: " + registry.getBeanDefinitionCount());
//        String[] bns = registry.getBeanDefinitionNames();
//        for (String bn : bns) {
//            BeanDefinition bd = registry.getBeanDefinition(bn);
//            System.out.println(" - " + bd.getBeanClassName() + ":" + bd.getDescription() );
//        }
    }

}
