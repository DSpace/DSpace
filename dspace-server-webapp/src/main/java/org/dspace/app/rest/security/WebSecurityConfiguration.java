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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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
public class WebSecurityConfiguration {

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

    /**
     * Create a Spring Security AuthenticationManager with our custom AuthenticationProvider
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        ProviderManager manager = new ProviderManager(ePersonRestAuthenticationProvider);
        return manager;

    }

    /**
     * Bean to customize security on specific endpoints
     * @param http HttpSecurity
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Get the current AuthenticationManager (defined above) to apply filters below
        AuthenticationManager authenticationManager = authenticationManager();

        // Configure authentication requirements for ${dspace.server.url}/api/ URL only
        // NOTE: REST API is hardcoded to respond on /api/. Other modules (OAI, SWORD, IIIF, etc) use other root paths.
        http.securityMatcher("/api/**", "/iiif/**", actuatorBasePath + "/**", "/signposting/**")
            .authorizeHttpRequests((requests) -> requests
                // Ensure /actuator/info endpoint is restricted to admins
                .requestMatchers(new AntPathRequestMatcher(actuatorBasePath + "/info", HttpMethod.GET.name()))
                    .hasAnyAuthority(ADMIN_GRANT)
                // All other requests should be permitted at this layer because we check permissions on each method
                // via @PreAuthorize annotations. As this code runs first, we must permitAll() here in order to pass
                // the request on to those annotations.
                .anyRequest().permitAll())
            // Tell Spring to not create Sessions
            .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Anonymous requests should have the "ANONYMOUS" security grant
            .anonymous((anonymous) -> anonymous.authorities(ANONYMOUS_GRANT))
            // Wire up the HttpServletRequest with the current SecurityContext values
            .servletApi(Customizer.withDefaults())
            // Enable CORS for Spring Security (see CORS settings in Application and ApplicationConfig)
            .cors(Customizer.withDefaults())
            // Enable CSRF protection with custom csrfTokenRepository and custom sessionAuthenticationStrategy
            // (both are defined below as methods).
            // While we primarily use JWT in headers, CSRF protection is needed because we also support JWT via Cookies
            .csrf((csrf) -> csrf
                .csrfTokenRepository(this.csrfTokenRepository())
                .sessionAuthenticationStrategy(this.dSpaceCsrfAuthenticationStrategy())
                // Disable SpringSecurity BREACH protection, as this is not working well with Cookie-based storage.
                // When enabled, BREACH protection causes the CSRF token to grow in size until UI errors occur.
                // See https://github.com/DSpace/DSpace/issues/9450
                // NOTE: DSpace doesn't need BREACH protection as it's only necessary when sending the token via a
                // request attribute (e.g. "_csrf") which the DSpace UI never does.
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .exceptionHandling((exceptionHandling) -> exceptionHandling
                // Return 401 on authorization failures with a correct WWWW-Authenticate header
                .authenticationEntryPoint(new DSpace401AuthenticationEntryPoint(restAuthenticationService))
                // Custom handler for AccessDeniedExceptions, including CSRF exceptions
                .accessDeniedHandler(accessDeniedHandler)
            )
            // Logout configuration
            .logout((logout) -> logout
                // On logout, clear the "session" salt
                .addLogoutHandler(customLogoutHandler)
                // Configure the logout entry point & require POST
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/authn/logout", HttpMethod.POST.name()))
                // When logout is successful, return OK (204) status
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            )
            // Add a filter before any request to handle DSpace IP-based authorization/authentication
            // (e.g. anonymous users may be added to special DSpace groups if they are in a given IP range)
            .addFilterBefore(new AnonymousAdditionalAuthorizationFilter(authenticationManager, authenticationService),
                             StatelessAuthenticationFilter.class)
            // Add a filter before our login endpoints to do the authentication based on the data in the HTTP request.
            // This login endpoint only responds to POST as it is used for PasswordAuthentication
            .addFilterBefore(new StatelessLoginFilter("/api/authn/login", HttpMethod.POST.name(),
                                                      authenticationManager, restAuthenticationService),
                             LogoutFilter.class)
            // Add a filter before our shibboleth endpoints to do the authentication based on the data in the HTTP
            // request. This endpoint only responds to GET as the actual authentication is performed by Shibboleth,
            // which then redirects to this endpoint to forward the authentication data to DSpace.
            .addFilterBefore(new ShibbolethLoginFilter("/api/authn/shibboleth", HttpMethod.GET.name(),
                                                       authenticationManager, restAuthenticationService),
                             LogoutFilter.class)
            // Add a filter before our ORCID endpoints to do the authentication based on the data in the HTTP request.
            // This endpoint only responds to GET as the actual authentication is performed by ORCID, which then
            // redirects to this endpoint to forward the authentication data to DSpace.
            .addFilterBefore(new OrcidLoginFilter("/api/authn/orcid", HttpMethod.GET.name(),
                                                  authenticationManager, restAuthenticationService),
                             LogoutFilter.class)
            // Add a filter before our OIDC endpoints to do the authentication based on the data in the HTTP request.
            // This endpoint only responds to GET as the actual authentication is performed by OIDC, which then
            // redirects to this endpoint to forward the authentication data to DSpace.
            .addFilterBefore(new OidcLoginFilter("/api/authn/oidc", HttpMethod.GET.name(),
                                                 authenticationManager, restAuthenticationService),
                             LogoutFilter.class)
            // Add a custom Token based authentication filter based on the token previously given to the client
            // before each URL
            .addFilterBefore(new StatelessAuthenticationFilter(authenticationManager, restAuthenticationService,
                                                               ePersonRestAuthenticationProvider, requestService),
                             StatelessLoginFilter.class);
        return http.build();
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
     *
     * This is defined as a bean so that it can also be used in other code to reset CSRF Tokens, see
     * JWTTokenRestAuthenticationServiceImpl
     */
    @Lazy
    @Bean
    public DSpaceCsrfAuthenticationStrategy dSpaceCsrfAuthenticationStrategy() {
        return new DSpaceCsrfAuthenticationStrategy(csrfTokenRepository());
    }

}
