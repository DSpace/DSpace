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
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration for DSpace Spring Rest
 *
 * @author Atmire NV (info at atmire dot com)
 */
@EnableWebSecurity
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static final String ADMIN_GRANT = "ADMIN";
    public static final String EPERSON_GRANT = "EPERSON";
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
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().cacheControl();
        http
                //Tell Spring to not create Sessions
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                //Return the login URL when having an access denied error
                .exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/api/authn/login")).and()
                //Anonymous requests should have the "ANONYMOUS" security grant
                .anonymous().authorities(ANONYMOUS_GRANT).and()
                //Wire up the HttpServletRequest with the current SecurityContext values
                .servletApi().and()
                //Disable CSRF as our API can be used by clients on an other domain, we are also protected against this, since we pass the token in a header
                .csrf().disable()

                //Logout configuration
                .logout()
                        //On logout, clear the "session" salt
                        .addLogoutHandler(customLogoutHandler)
                        //Configure the logout entry point
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/authn/logout"))
                        //When logout is successful, return OK (200) status
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                        //Everyone can call this endpoint
                        .permitAll()
                .and()

                //Configure the URL patterns with their authentication requirements
                .authorizeRequests()
                    //Allow GET and POST by anyone on the login endpoint
                    .antMatchers( "/api/authn/login").permitAll()
                    //Everyone can call GET on the status endpoint
                    .antMatchers(HttpMethod.GET, "/api/authn/status").permitAll()
                .and()

                //Add a filter before our login endpoints to do the authentication based on the data in the HTTP request
                .addFilterBefore(new StatelessLoginFilter("/api/authn/login", authenticationManager(), restAuthenticationService), LogoutFilter.class)
                //TODO see comment at org.dspace.app.rest.AuthenticationRestController.shibbolethLogin()
                .addFilterBefore(new StatelessLoginFilter("/shibboleth-login", authenticationManager(), restAuthenticationService), LogoutFilter.class)

                // Add a custom Token based authentication filter based on the token previously given to the client before each URL
                .addFilterBefore(new StatelessAuthenticationFilter(authenticationManager(), restAuthenticationService,
                        ePersonRestAuthenticationProvider, requestService), StatelessLoginFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(ePersonRestAuthenticationProvider);
    }

}