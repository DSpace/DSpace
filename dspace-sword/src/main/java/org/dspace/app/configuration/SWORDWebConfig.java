/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.configuration;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SWORDWebConfig {

    @Bean
    public ServletContextInitializer swordv1ContextInitializer() {
        return servletContext -> {
            servletContext.setInitParameter("sword-server-class", "org.dspace.sword.DSpaceSWORDServer");
            servletContext.setInitParameter("authentication-method", "Basic");
        };
    }

    @Bean
    public ServletRegistrationBean swordv1ServiceDocumentBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean( new org.purl.sword.server.ServiceDocumentServlet(),
                                                                    "/sword/servicedocument/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ServletRegistrationBean swordv1DepositBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean( new org.purl.sword.server.DepositServlet(),
                                                                    "/sword/deposit/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ServletRegistrationBean swordv1MediaLinkBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean( new org.purl.sword.server.AtomDocumentServlet(),
                                                                    "/sword/media-link/*");
        bean.setLoadOnStartup(1);
        return bean;
    }
}
