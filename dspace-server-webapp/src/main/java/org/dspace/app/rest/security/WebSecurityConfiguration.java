/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.dspace.app.rest.exception.DSpaceAccessDeniedHandler;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration for DSpace Server Webapp
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@EnableWebSecurity
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static final String ADMIN_GRANT = "ADMIN";
    public static final String AUTHENTICATED_GRANT = "AUTHENTICATED";
    public static final String ANONYMOUS_GRANT = "ANONYMOUS";

    @Autowired
    private EPersonRestAuthenticationProvider ePersonRestAuthenticationProvider;

    @Autowired
    private RestAuthenticationService restAuthenticationService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private CustomLogoutHandler customLogoutHandler;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private DSpaceAccessDeniedHandler accessDeniedHandler;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorBasePath;

    @Override
    public void configure(WebSecurity webSecurity) throws Exception {
        // Define URL patterns which Spring Security will ignore entirely.
        webSecurity
            .ignoring()
                // These /login request types are purposefully unsecured, as they all throw errors.
                .antMatchers(HttpMethod.GET, "/api/authn/login")
                .antMatchers(HttpMethod.PUT, "/api/authn/login")
                .antMatchers(HttpMethod.PATCH, "/api/authn/login")
                .antMatchers(HttpMethod.DELETE, "/api/authn/login");
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Configure authentication requirements for ${dspace.server.url}/api/ URL only
        // NOTE: REST API is hardcoded to respond on /api/. Other modules (OAI, SWORD, IIIF, etc) use other root paths.
        http.requestMatchers()
            .antMatchers("/api/**", "/iiif/**", actuatorBasePath + "/**")
            .and()
            // Enable Spring Security authorization on these paths
            .authorizeRequests()
                // Allow POST by anyone on the login endpoint
                .antMatchers(HttpMethod.POST,"/api/authn/login").permitAll()
                // Everyone can call GET on the status endpoint (used to check your authentication status)
                .antMatchers(HttpMethod.GET, "/api/authn/status").permitAll()
                .antMatchers(HttpMethod.GET, actuatorBasePath + "/info").hasAnyAuthority(ADMIN_GRANT)
            .and()
            // Tell Spring to not create Sessions
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            // Anonymous requests should have the "ANONYMOUS" security grant
            .anonymous().authorities(ANONYMOUS_GRANT).and()
            // Wire up the HttpServletRequest with the current SecurityContext values
            .servletApi().and()
            // Enable CORS for Spring Security (see CORS settings in Application and ApplicationConfig)
            .cors().and()
            // Enable CSRF protection with custom csrfTokenRepository and custom sessionAuthenticationStrategy
            // (both are defined below as methods).
            // While we primarily use JWT in headers, CSRF protection is needed because we also support JWT via Cookies
            .csrf()
                .csrfTokenRepository(this.csrfTokenRepository())
                .sessionAuthenticationStrategy(this.sessionAuthenticationStrategy())
            .and()
            .exceptionHandling()
                // Return 401 on authorization failures with a correct WWWW-Authenticate header
                .authenticationEntryPoint(new DSpace401AuthenticationEntryPoint(restAuthenticationService))
                // Custom handler for AccessDeniedExceptions, including CSRF exceptions
                .accessDeniedHandler(accessDeniedHandler)
            .and()

            // Logout configuration
            .logout()
                // On logout, clear the "session" salt
                .addLogoutHandler(customLogoutHandler)
                // Configure the logout entry point & require POST
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/authn/logout", HttpMethod.POST.name()))
                // When logout is successful, return OK (204) status
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                // Everyone can call this endpoint
                .permitAll()
            .and()

            // Add a filter before any request to handle DSpace IP-based authorization/authentication
            // (e.g. anonymous users may be added to special DSpace groups if they are in a given IP range)
            .addFilterBefore(new AnonymousAdditionalAuthorizationFilter(authenticationManager(), authenticationService),
                             StatelessAuthenticationFilter.class)
            // Add a filter before our login endpoints to do the authentication based on the data in the HTTP request
            .addFilterBefore(new StatelessLoginFilter("/api/authn/login", authenticationManager(),
                                                      restAuthenticationService),
                             LogoutFilter.class)
            // Add a filter before our shibboleth endpoints to do the authentication based on the data in the
            // HTTP request
            .addFilterBefore(new ShibbolethLoginFilter("/api/authn/shibboleth", authenticationManager(),
                                                       restAuthenticationService),
                             LogoutFilter.class)
            //Add a filter before our ORCID endpoints to do the authentication based on the data in the
            // HTTP request
            .addFilterBefore(new OrcidLoginFilter("/api/authn/orcid", authenticationManager(),
                                                       restAuthenticationService),
                             LogoutFilter.class)
            //Add a filter before our OIDC endpoints to do the authentication based on the data in the
            // HTTP request
            .addFilterBefore(new OidcLoginFilter("/api/authn/oidc", authenticationManager(),
                                                      restAuthenticationService),
                             LogoutFilter.class)
            // Add a custom Token based authentication filter based on the token previously given to the client
            // before each URL
            .addFilterBefore(new StatelessAuthenticationFilter(authenticationManager(), restAuthenticationService,
                                                               ePersonRestAuthenticationProvider, requestService),
                             StatelessLoginFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(ePersonRestAuthenticationProvider);
    }

    /**
     * Returns a custom DSpaceCsrfTokenRepository based on Spring Security's CookieCsrfTokenRepository, which is
     * designed for Angular Apps.
     * <P>
     * The DSpaceCsrfTokenRepository stores the token in server-side cookie (for later verification), but sends it to
     * the client as a DSPACE-XSRF-TOKEN header. The client is expected to return the token in either a header named
     * X-XSRF-TOKEN *or* a URL parameter named "_csrf", at which point it is validated against the server-side cookie.
     * <P>
     * This behavior is based on the defaults for Angular apps: https://angular.io/guide/http#security-xsrf-protection.
     * However, instead of sending an XSRF-TOKEN Cookie (as is usual for Angular apps), we send the DSPACE-XSRF-TOKEN
     * header...as this ensures the Angular app can receive the token even if it is on a different domain.
     *
     * @return CsrfTokenRepository as described above
     */
    @Lazy
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new DSpaceCsrfTokenRepository();
    }

    /**
     * Returns a custom DSpaceCsrfAuthenticationStrategy, which ensures that (after authenticating) the CSRF token
     * is only refreshed when it is used (or attempted to be used) by the client.
     */
    private SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new DSpaceCsrfAuthenticationStrategy(csrfTokenRepository());
    }

}
