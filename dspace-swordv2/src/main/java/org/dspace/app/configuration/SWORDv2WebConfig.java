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
 * SWORDv2 webapp configuration. Replaces the old web.xml
 * <p>
 * This @Configuration class is automatically discovered by Spring via a @ComponentScan.
 * <p>
 * All SWORDv2 web configurations (beans) can be enabled or disabled by setting "swordv2-server.enabled"
 * to true or false, respectively (in your DSpace configuration). Default is "false".
 * <p>
 * All @Value annotated configurations below can also be overridden in your DSpace configuration.
 *
 * @author Tim Donohue
 */
@Configuration
public class SWORDv2WebConfig {
    // Path where SWORDv2 should be deployed (when enabled). Defaults to "swordv2"
    @Value("${swordv2-server.path:swordv2}")
    private String swordv2Path;

    // ServiceDocumentManager server implementation class name (default: org.dspace.sword2.ServiceDocumentManagerDSpace)
    @Value("${swordv2-server.service-document-impl:org.dspace.sword2.ServiceDocumentManagerDSpace}")
    private String serviceDocImpl;

    // CollectionListManager server implementation class name (default: org.dspace.sword2.CollectionListManagerDSpace)
    @Value("${swordv2-server.collection-list-impl:org.dspace.sword2.CollectionListManagerDSpace}")
    private String collectionListImpl;

    // CollectionDepositManager server implementation class name
    // (default: org.dspace.sword2.CollectionDepositManagerDSpace)
    @Value("${swordv2-server.collection-deposit-impl:org.dspace.sword2.CollectionDepositManagerDSpace}")
    private String collectionDepositImpl;

    // MediaResourceManager server implementation class name (default: org.dspace.sword2.MediaResourceManagerDSpace)
    @Value("${swordv2-server.media-resource-impl:org.dspace.sword2.MediaResourceManagerDSpace}")
    private String mediaResourceImpl;

    // ContainerManager server implementation class name (default: org.dspace.sword2.ContainerManagerDSpace)
    @Value("${swordv2-server.container-impl:org.dspace.sword2.ContainerManagerDSpace}")
    private String containerImpl;

    // StatementManager server implementation class name (default: org.dspace.sword2.StatementManagerDSpace)
    @Value("${swordv2-server.statement-impl:org.dspace.sword2.StatementManagerDSpace}")
    private String statementImpl;

    // SwordConfiguration server implementation class name (default: org.dspace.sword2.SwordConfigurationDSpace)
    @Value("${swordv2-server.config-impl:org.dspace.sword2.SwordConfigurationDSpace}")
    private String configImpl;

    // Authentication Method. Defaults to "Basic"
    @Value("${swordv2-server.auth-type:Basic}")
    private String authenticationMethod;

    /**
     * Initialize all required Context Parameters (i.e. <context-param> in web.xml), based on configurations above.
     * <p>
     * This bean is only loaded when swordv2-server.enabled = true
     * @return ServletContextInitializer which includes all required params
     */
    @Bean
    @ConditionalOnProperty("swordv2-server.enabled")
    public ServletContextInitializer swordv2ContextInitializer() {
        return servletContext -> {
            servletContext.setInitParameter("service-document-impl", serviceDocImpl);
            servletContext.setInitParameter("collection-list-impl", collectionListImpl);
            servletContext.setInitParameter("collection-deposit-impl", collectionDepositImpl);
            servletContext.setInitParameter("media-resource-impl", mediaResourceImpl);
            servletContext.setInitParameter("container-impl", containerImpl);
            servletContext.setInitParameter("statement-impl", statementImpl);
            servletContext.setInitParameter("config-impl", configImpl);
            servletContext.setInitParameter("authentication-method", authenticationMethod);
        };
    }

    // Servlet Beans. All of the below bean definitions map servlets to respond to specific URL patterns
    // These are the combined equivalent of <servlet> and <servlet-mapping> in web.xml
    // All beans are only loaded when swordv2-server.enabled = true

    @Bean
    @ConditionalOnProperty("swordv2-server.enabled")
    public ServletRegistrationBean swordv2ServiceDocumentBean() {
        ServletRegistrationBean bean =
            new ServletRegistrationBean(new org.swordapp.server.servlets.ServiceDocumentServletDefault(),
                                        "/" + swordv2Path + "/servicedocument/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty("swordv2-server.enabled")
    public ServletRegistrationBean swordv2CollectionBean() {
        ServletRegistrationBean bean =
            new ServletRegistrationBean( new org.swordapp.server.servlets.CollectionServletDefault(),
                                         "/" + swordv2Path + "/collection/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty("swordv2-server.enabled")
    public ServletRegistrationBean swordv2MediaResourceBean() {
        ServletRegistrationBean bean =
            new ServletRegistrationBean( new org.swordapp.server.servlets.MediaResourceServletDefault(),
                                         "/" + swordv2Path + "/edit-media/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty("swordv2-server.enabled")
    public ServletRegistrationBean swordv2ContainerBean() {
        ServletRegistrationBean bean =
            new ServletRegistrationBean( new org.swordapp.server.servlets.ContainerServletDefault(),
                                         "/" + swordv2Path + "/edit/*");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty("swordv2-server.enabled")
    public ServletRegistrationBean swordv2StatementBean() {
        ServletRegistrationBean bean =
            new ServletRegistrationBean( new org.swordapp.server.servlets.StatementServletDefault(),
                                         "/" + swordv2Path + "/statement/*");
        bean.setLoadOnStartup(1);
        return bean;
    }
}

