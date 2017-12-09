/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.File;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.Filter;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.filter.DSpaceRequestContextFilter;
import org.dspace.app.rest.model.hateoas.DSpaceRelProvider;
import org.dspace.app.rest.utils.ApplicationConfig;
import org.dspace.app.util.DSpaceContextListener;
import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.utils.servlet.DSpaceWebappServletFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.RelProvider;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

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
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Tim Donohue
 */
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    private ApplicationConfig configuration;

    /**
     * Override the default SpringBootServletInitializer.configure() method,
     * passing it this Application class.
     * <p>
     * This is necessary to allow us to build a deployable WAR, rather than
     * always relying on embedded Tomcat.
     * <p>
     * <p>
     * See: http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-create-a-deployable-war-file
     *
     * @param application
     * @return
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class)
                .initializers(
                        new DSpaceKernelInitializer(),
                        new FlywayDatabaseMigrationInitializer());
    }

    @Bean
    public ServletContextInitializer contextInitializer() {
        return servletContext -> servletContext.setInitParameter("dspace.dir", configuration.getDspaceHome());
    }

    /**
     * Register the "DSpaceContextListener" so that it is loaded
     * for this Application.
     *
     * @return DSpaceContextListener
     */
    @Bean
    @Order(2)
    protected DSpaceContextListener dspaceContextListener() {
        // This listener initializes the DSpace Context object
        // (and loads all DSpace configs)
        return new DSpaceContextListener();
    }

    /**
     * Register the DSpaceWebappServletFilter, which initializes the
     * DSpace RequestService / SessionService
     *
     * @return DSpaceWebappServletFilter
     */
    @Bean
    @Order(1)
    protected Filter dspaceWebappServletFilter() {
        return new DSpaceWebappServletFilter();
    }

    /**
     * Register the DSpaceRequestContextFilter, a Filter which checks for open
     * Context objects *after* a request has been fully processed, and closes them
     *
     * @return DSpaceRequestContextFilter
     */
    @Bean
    @Order(2)
    protected Filter dspaceRequestContextFilter() {
        return new DSpaceRequestContextFilter();
    }

    @Bean
    public RequestContextListener requestContextListener(){
        return new RequestContextListener();
    }

    @Bean
    protected RelProvider dspaceRelProvider() {
        return new DSpaceRelProvider();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] corsAllowedOrigins = configuration.getCorsAllowedOrigins();
                if (corsAllowedOrigins != null) {
                    registry.addMapping("/api/**").allowedOrigins(corsAllowedOrigins);
                }
            }
        };
    }

    /** Utility class that will destroy the DSpace Kernel on Spring Boot shutdown */
    private class DSpaceKernelDestroyer implements ApplicationListener<ContextClosedEvent> {
        private DSpaceKernel kernel;

        public DSpaceKernelDestroyer(DSpaceKernel kernel) {
            this.kernel = kernel;
        }

        public void onApplicationEvent(final ContextClosedEvent event) {
            if (this.kernel != null) {
                this.kernel.destroy();
                this.kernel = null;
            }
        }
    }

    /** Utility class that will initialize the DSpace Kernel on Spring Boot startup */
    private class DSpaceKernelInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private transient DSpaceKernel dspaceKernel;

        public void initialize(final ConfigurableApplicationContext applicationContext) {

            String dspaceHome = applicationContext.getEnvironment().getProperty("dspace.dir");

            this.dspaceKernel = DSpaceKernelManager.getDefaultKernel();
            if (this.dspaceKernel == null) {
                DSpaceKernelImpl kernelImpl = null;
                try {
                    kernelImpl = DSpaceKernelInit.getKernel(null);
                    if (!kernelImpl.isRunning()) {
                        kernelImpl.start(getProvidedHome(dspaceHome)); // init the kernel
                    }
                    this.dspaceKernel = kernelImpl;

                } catch (Exception e) {
                    // failed to start so destroy it and log and throw an exception
                    try {
                        if (kernelImpl != null) {
                            kernelImpl.destroy();
                        }
                        this.dspaceKernel = null;
                    } catch (Exception e1) {
                        // nothing
                    }
                    String message = "Failure during ServletContext initialisation: " + e.getMessage();
                    log.error(message + ":" + e.getMessage(), e);
                    throw new RuntimeException(message, e);
                }
            }

            if (applicationContext.getParent() == null) {
                //Set the DSpace Kernel Application context as a parent of the Spring Boot context so that
                //we can auto-wire all DSpace Kernel services
                applicationContext.setParent(dspaceKernel.getServiceManager().getApplicationContext());

                //Add a listener for Spring Boot application shutdown so that we can nicely cleanup the DSpace kernel.
                applicationContext.addApplicationListener(new DSpaceKernelDestroyer(dspaceKernel));
            }
        }

        /**
         * Find DSpace's "home" directory.
         * Initially look for JNDI Resource called "java:/comp/env/dspace.dir".
         * If not found, look for "dspace.dir" initial context parameter.
         */
        private String getProvidedHome(String dspaceHome) {
            String providedHome = null;
            try {
                Context ctx = new InitialContext();
                providedHome = (String) ctx.lookup("java:/comp/env/" + DSpaceConfigurationService.DSPACE_HOME);
            } catch (Exception e) {
                // do nothing
            }

            if (providedHome == null) {
                if (dspaceHome != null && !dspaceHome.equals("") &&
                        !dspaceHome.equals("${" + DSpaceConfigurationService.DSPACE_HOME + "}")) {
                    File test = new File(dspaceHome);
                    if (test.exists() && new File(test, DSpaceConfigurationService.DSPACE_CONFIG_PATH).exists()) {
                        providedHome = dspaceHome;
                    }
                }
            }
            return providedHome;
        }
    }

    private class FlywayDatabaseMigrationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            //If we fail to update the database, throw an exception
            if(!org.dspace.core.Context.updateDatabase()) {
                throw new RuntimeException("Unable to initialize the database");
            }
        }
    }
}