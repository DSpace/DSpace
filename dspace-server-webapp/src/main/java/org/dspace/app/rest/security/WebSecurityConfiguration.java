/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration for DSpace Spring Rest
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

    @Override
    public void configure(WebSecurity webSecurity) throws Exception {
        webSecurity
            .ignoring()
                .antMatchers(HttpMethod.GET, "/api/authn/login")
                .antMatchers(HttpMethod.PUT, "/api/authn/login")
                .antMatchers(HttpMethod.PATCH, "/api/authn/login")
                .antMatchers(HttpMethod.DELETE, "/api/authn/login");
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().cacheControl();
        http
            //Tell Spring to not create Sessions
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            //Anonymous requests should have the "ANONYMOUS" security grant
            .anonymous().authorities(ANONYMOUS_GRANT).and()
            //Wire up the HttpServletRequest with the current SecurityContext values
            .servletApi().and().cors().and()
            //Disable CSRF as our API can be used by clients on an other domain, we are also protected against this,
            // since we pass the token in a header
            .csrf().disable()
            //Return 401 on authorization failures with a correct WWWW-Authenticate header
            .exceptionHandling().authenticationEntryPoint(
                    new DSpace401AuthenticationEntryPoint(restAuthenticationService))
            .and()

            //Logout configuration
            .logout()
                //On logout, clear the "session" salt
                .addLogoutHandler(customLogoutHandler)
                //Configure the logout entry point
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/authn/logout"))
                //When logout is successful, return OK (204) status
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                //Everyone can call this endpoint
                .permitAll()
            .and()

            //Configure the URL patterns with their authentication requirements
            //Enable Spring Security authorization on /api/ URLs only
            .antMatcher("/api/**").authorizeRequests()
                //Allow POST by anyone on the login endpoint
                .antMatchers(HttpMethod.POST,"/api/authn/login").permitAll()
                //TRACE, CONNECT, OPTIONS, HEAD
                //Everyone can call GET on the status endpoint
                .antMatchers(HttpMethod.GET, "/api/authn/status").permitAll()
            .and()

            //Add a filter before our login endpoints to do the authentication based on the data in the HTTP request
            .addFilterBefore(new StatelessLoginFilter("/api/authn/login", authenticationManager(),
                                                      restAuthenticationService),
                             LogoutFilter.class)

            //Add a filter before our shibboleth endpoints to do the authentication based on the data in the
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

}
