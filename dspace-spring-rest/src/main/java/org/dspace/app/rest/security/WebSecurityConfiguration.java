/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@EnableWebSecurity
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private EPersonRestAuthenticationProvider ePersonRestAuthenticationProvider;

    @Autowired
    private CustomLogoutHandler customLogoutHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().cacheControl();
        http
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .exceptionHandling().and()
                .anonymous().and()
                .servletApi().and()
                .csrf().disable()

                .logout().addLogoutHandler(customLogoutHandler).logoutRequestMatcher(new AntPathRequestMatcher("/api/logout")).logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()).permitAll()
                .and()
                .authorizeRequests()

                .antMatchers( "/api/login").permitAll()
                .antMatchers(HttpMethod.GET, "/api/status").permitAll()

                .and()

                .addFilterBefore(new StatelessLoginFilter("/api/login", authenticationManager()), UsernamePasswordAuthenticationFilter.class)

                // Custom Token based authentication based on the header previously given to the client
                .addFilterBefore(new StatelessAuthenticationFilter(authenticationManager()),  UsernamePasswordAuthenticationFilter.class);


    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //Add the in-memory user with and ADMIN role. This user can be used to create the initial users.
        auth.authenticationProvider(ePersonRestAuthenticationProvider);
    }

}