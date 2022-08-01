/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.consumer;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Exposes the Spring application's IIIF cache evict service to the DSpace event consumer.
 */
@Component
public class CacheEvictBeanLocator implements ApplicationContextAware {

    private static ApplicationContext context;

    private static final String MANIFESTS_CACHE_EVICT_SERVICE = "manifestsCacheEvictService";
    private static final String CANVAS_DIMENSIONS_EVICT_SERVICE = "canvasCacheEvictService";

    @Override
    public void setApplicationContext(ApplicationContext appContext)
        throws BeansException {
        context = appContext;
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    public static ManifestsCacheEvictService getManifestsCacheEvictService() {
        if (context != null) {
            return (ManifestsCacheEvictService) context.getBean(MANIFESTS_CACHE_EVICT_SERVICE);
        }
        return null;
    }

    public static CanvasCacheEvictService getCanvasCacheEvictService() {
        if (context != null) {
            return (CanvasCacheEvictService) context.getBean(CANVAS_DIMENSIONS_EVICT_SERVICE);
        }
        return null;
    }

}
