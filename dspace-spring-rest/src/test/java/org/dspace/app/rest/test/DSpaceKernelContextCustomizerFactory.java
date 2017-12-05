/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import java.util.List;

import org.dspace.kernel.DSpaceKernelManager;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * Context customizer factory to set the parent context of our Spring Boot application in TEST mode
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
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
