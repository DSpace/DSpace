/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.config;

import org.dspace.kernel.DSpaceKernel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Starts the DSpace kernel to load all configurations and services
 */
@Configuration
public class DSpaceConfigLoader {

    @Bean
    public DSpaceKernel dspaceKernel() throws Exception {
        DSpaceKernel kernel = DSpaceKernel.init(null);
        kernel.start();
        return kernel;
    }
}
