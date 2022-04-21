/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.cache;

import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Exposes the Spring application's Sherpa cache evict service to the submission sherpaPolicyStep.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@Component
public class SherpaCacheEvictBeanLocator implements ApplicationContextAware {

    private static ApplicationContext context;

    private static final String SHERPA_CACHE_EVICT_SERVICE = "sherpaCacheEvictService";

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static SherpaCacheEvictService getSherpaCacheEvictService() {
        return Objects.nonNull(context) ? (SherpaCacheEvictService) context.getBean(SHERPA_CACHE_EVICT_SERVICE) : null;
    }

}