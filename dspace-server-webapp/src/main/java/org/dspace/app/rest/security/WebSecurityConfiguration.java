/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
        // NOTE: REST API is hardcoded to respond on /api/. Other modules (OAI, SWORD, etc) use other root paths.
        http.antMatcher("/api/**")
            // Enable Spring Security authorization on these paths
            .authorizeRequests()
                // Allow POST by anyone on the login endpoint
                .antMatchers(HttpMethod.POST,"/api/authn/login").permitAll()
                // Everyone can call GET on the status endpoint (used to check your authentication status)
                .antMatchers(HttpMethod.GET, "/api/authn/status").permitAll()
            .and()
            // Tell Spring to not create Sessions
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            // Anonymous requests should have the "ANONYMOUS" security grant
            .anonymous().authorities(ANONYMOUS_GRANT).and()
            // Wire up the HttpServletRequest with the current SecurityContext values
            .servletApi().and()
            // Enable CORS for Spring Security (see CORS settings in Application and ApplicationConfig)
            .cors().and()
            // Enable CSRF protection with custom CookieCsrfTokenRepository (see below) designed for Angular apps
            // While we primarily use JWT in headers, CSRF protection is needed because we also support JWT via Cookies
            .csrf().csrfTokenRepository(this.getCsrfTokenRepository()).and()
            // Return 401 on authorization failures with a correct WWWW-Authenticate header
            .exceptionHandling().authenticationEntryPoint(
                    new DSpace401AuthenticationEntryPoint(restAuthenticationService))
            .and()

            // Logout configuration
            .logout()
                // On logout, clear the "session" salt
                .addLogoutHandler(customLogoutHandler)
                // Configure the logout entry point
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/authn/logout"))
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
            .addFilterBefore(new ShibbolethAuthenticationFilter("/api/authn/shibboleth", authenticationManager(),
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
     * Override the defaults of CookieCsrfTokenRepository to always set the Cookie Path to "/"
     * <P>
     * We use the CookieCsrfTokenRepository designed for Angular apps
     * See https://docs.spring.io/spring-security/site/docs/current/reference/html5/#servlet-csrf
     * <P>
     * This CookieCsrfTokenRepository will write a cookie named XSRF-TOKEN and read it from
     * a header named X-XSRF-TOKEN *or* a URL parameter named "_csrf". Angular apps will respond to
     * XSRF-TOKEN automatically, see: https://angular.io/guide/http#security-xsrf-protection
     * <P>
     * However, currently Angular *requires* the CSR cookie path to always be "/" or it will ignore it.
     * See: https://stackoverflow.com/a/50511663
     * @return CookieCsrfTokenRepository with cookie path="/"
     */
    private CsrfTokenRepository getCsrfTokenRepository() {
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokenRepository.setCookiePath("/");
        return tokenRepository;
    }

}
