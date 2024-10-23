/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.configuration;

import org.dspace.saml2.DSpaceSamlAuthenticationFailureHandler;
import org.dspace.saml2.DSpaceSamlAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security configuration for SAML relying party endpoints.
 * <p>
 * This establishes and manages security for the following endpoints:
 * <ul>
 *   <li>/saml2/service-provider-metadata/{relyingPartyRegistrationId}</li>
 *   <li>/saml2/authenticate/{relyingPartyRegistrationId}</li>
 *   <li>/saml2/assertion-consumer/{relyingPartyRegistrationId}</li>
 * </ul>
 * </p>
 * <p>
 * This @Configuration class is automatically discovered by Spring Boot via a @ComponentScan
 * on the org.dspace.app.configuration package.
 * <p>
 *
 * @author Ray Lee
 */
@EnableWebSecurity
@Configuration
@ComponentScan(basePackages = "org.dspace.saml2")
public class SamlWebSecurityConfiguration {

    /**
     * Configure security on SAML relying party endpoints.
     *
     * @param http the HTTP security builder to configure
     * @return the configured security filter chain
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain samlSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/saml2/**")
            // Initiate SAML login at /saml2/authenticate/{registrationId}.
            .saml2Login(saml -> saml
                // Accept SAML identity assertions from the IdP at /saml2/assertion-consumer/{registrationId}.
                .loginProcessingUrl("/saml2/assertion-consumer/{registrationId}")
                .successHandler(new DSpaceSamlAuthenticationSuccessHandler())
                .failureHandler(new DSpaceSamlAuthenticationFailureHandler()))
            // Produce SAML relying party metadata at /saml2/service-provider-metadata/{registrationId}.
            .saml2Metadata(Customizer.withDefaults())
            .build();
    }
}
