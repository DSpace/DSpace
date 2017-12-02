/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Atmire NV (info at atmire dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class EPersonRestAuthenticationProviderTest {

    @InjectMocks
    private EPersonRestAuthenticationProvider ePersonRestAuthenticationProvider;

    @Mock
    private EPerson ePerson;

    @Mock
    private Context context;

    @Mock
    private AuthorizeService authorizeService;

    @Test
    public void testGetGrantedAuthoritiesAdmin() throws Exception {
        when(authorizeService.isAdmin(context, ePerson)).thenReturn(true);

        List<GrantedAuthority> authorities = ePersonRestAuthenticationProvider.getGrantedAuthorities(context, ePerson);

        assertThat(authorities.stream().map(a -> a.getAuthority()).collect(Collectors.toList()), containsInAnyOrder(
                WebSecurityConfiguration.ADMIN_GRANT, WebSecurityConfiguration.EPERSON_GRANT));

    }

    @Test
    public void testGetGrantedAuthoritiesEPerson() throws Exception {
        when(authorizeService.isAdmin(context, ePerson)).thenReturn(false);

        List<GrantedAuthority> authorities = ePersonRestAuthenticationProvider.getGrantedAuthorities(context, ePerson);

        assertThat(authorities.stream().map(a -> a.getAuthority()).collect(Collectors.toList()), containsInAnyOrder(
                WebSecurityConfiguration.EPERSON_GRANT));

    }

}