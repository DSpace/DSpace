/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app;

import org.dspace.app.rest.WebApplication;
import org.dspace.app.rest.utils.DSpaceConfigurationInitializer;
import org.dspace.app.rest.utils.DSpaceKernelInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Define the Spring Boot Application settings itself to be run using an
 * embedded application server.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings({ "checkstyle:hideutilityclassconstructor" })
@SpringBootApplication(scanBasePackageClasses = WebApplication.class)
public class ServerBootApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ServerBootApplication.class)
            .initializers(new DSpaceKernelInitializer(), new DSpaceConfigurationInitializer())
            .run(args);
    }

}
