/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SWORD webapp configuration. Replaces the old web.xml
 * <p>
 * This @Configuration class is automatically discovered by Spring via a @ComponentScan.
 * <p>
 * All SWORD web configurations (beans) can be enabled or disabled by setting "sword-server.enabled"
 * to true or false, respectively (in your DSpace configuration). Default is "false".
 * <p>
 * All @Value annotated configurations below can also be overridden in your DSpace configuration.
 *
 * @author Tim Donohue
 */
@Configuration
public class SWORDWebConfig {
    // Path where SWORD should be deployed (when enabled). Defaults to "sword"
    @Value("${sword-server.path:sword}")
    private String swordPath;

    // SWORD Server class. Defaults to "org.dspace.sword.DSpaceSWORDServer"
    @Value("${sword-server.class:org.dspace.sword.DSpaceSWORDServer}")
    private String serverClass;

    // SWORD Authentication Method. Defaults to "Basic"
    @Value("${sword-server.authentication-method:Basic}")
    private String authenticationMethod;

    /**
     * Initialize all required Context Parameters (i.e. <context-param> in web.xml), based on configurations above.
     * <p>
     * This bean is only loaded when sword-server.enabled = true
     * @return ServletContextInitializer which includes all required params
     */
    @Bean
    @ConditionalOnProperty("sword-server.enabled")
    public ServletContextInitializer swordv1ContextInitializer() {
        return servletContext -> {
            servletContext.setInitParameter("sword-server-class", serverClass);
            servletContext.setInitParameter("authentication-method", authenticationMethod);
        };
    }

    // Servlet Beans. All of the below bean definitions map servlets to respond to specific URL patterns
    // These are the combined equivalent of <servlet> and <servlet-mapping> in web.xml
    // All beans are only loaded when sword-server.enabled = true

    @Bean
    @ConditionalOnProperty("sword-server.enabled")
    public ServletRegistrationBean swordv1ServiceDocumentBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean( new org.purl.sword.server.ServiceDocumentServlet(),
                                                                    "/" + swordPath + "/servicedocument/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty("sword-server.enabled")
    public ServletRegistrationBean swordv1DepositBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean( new org.purl.sword.server.DepositServlet(),
                                                                    "/" + swordPath + "/deposit/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty("sword-server.enabled")
    public ServletRegistrationBean swordv1MediaLinkBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean( new org.purl.sword.server.AtomDocumentServlet(),
                                                                    "/" + swordPath + "/media-link/*");
        bean.setLoadOnStartup(1);
        return bean;
    }
}
