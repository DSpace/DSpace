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
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RDF webapp configuration. Replaces the old web.xml
 * <p>
 * This @Configuration class is automatically discovered by Spring via a @ComponentScan.
 * <p>
 * All RDF web configurations (beans) can be enabled or disabled by setting "rdf.enabled"
 * to true or false, respectively (in your DSpace configuration). Default is "false".
 * <p>
 * All @Value annotated configurations below can also be overridden in your DSpace configuration.
 *
 * @author Tim Donohue
 */
@Configuration
public class RDFWebConfig {
    // Path where RDF should be deployed (when enabled). Defaults to "rdf"
    @Value("${rdf.path:rdf}")
    private String rdfPath;

    // Servlet Beans. All of the below bean definitions map servlets to respond to specific URL patterns
    // These are the combined equivalent of <servlet> and <servlet-mapping> in web.xml
    // All beans are only loaded when rdf.enabled = true

    @Bean
    @ConditionalOnProperty("rdf.enabled")
    public ServletRegistrationBean rdfSerializationBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean( new org.dspace.rdf.providing.DataProviderServlet(),
                                                                    "/" + rdfPath + "/handle/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty("rdf.enabled")
    public ServletRegistrationBean rdfLocalURIRedirectionBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean(
            new org.dspace.rdf.providing.LocalURIRedirectionServlet(), "/" + rdfPath + "/resource/*");
        bean.setLoadOnStartup(1);
        return bean;
    }
}

