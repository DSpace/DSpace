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
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Define the Spring Boot Application settings itself. This class takes the place
 * of a web.xml file, and configures all Filters/Listeners as methods (see below).
 * <p>
 * NOTE: Requires a Servlet 3.0 container, e.g. Tomcat 7.0 or above.
 * <p>
 * NOTE: This extends SpringBootServletInitializer in order to allow us to build
 * a deployable WAR file with Spring Boot. See:
 * http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-create-a-deployable-war-file
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SpringBootApplication(scanBasePackageClasses = WebApplication.class)
public class ServerApplication extends SpringBootServletInitializer {

    /**
     * Override the default SpringBootServletInitializer.configure() method,
     * passing it this Application class.
     * <p>
     * This is necessary to allow us to build a deployable WAR, rather than
     * always relying on embedded Tomcat.
     * <p>
     * See: http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-create-a-deployable-war-file
     *
     * @param application
     * @return
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // Pass this Application class, and our initializers for DSpace Kernel and Configuration
        // NOTE: Kernel must be initialized before Configuration
        return application.sources(ServerApplication.class)
                          .initializers(new DSpaceKernelInitializer(), new DSpaceConfigurationInitializer());
    }
}
