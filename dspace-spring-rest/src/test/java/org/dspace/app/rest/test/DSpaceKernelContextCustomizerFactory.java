package org.dspace.app.rest.test;

import java.util.List;

import org.dspace.kernel.DSpaceKernelManager;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * Context customizer factory to set the parent context of our Spring Boot application in TEST mode
 */
public class DSpaceKernelContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) -> {
            context.setParent(DSpaceKernelManager.getDefaultKernel().getServiceManager().getApplicationContext());
        };
    }

}
