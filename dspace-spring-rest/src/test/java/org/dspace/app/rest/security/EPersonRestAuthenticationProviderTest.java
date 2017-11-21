package org.dspace.app.rest.security;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

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
        assertEquals(WebSecurityConfiguration.ADMIN_GRANT ,authorities.get(0).getAuthority());
    }

    @Test
    public void testGetGrantedAuthoritiesEPerson() throws Exception {
        when(authorizeService.isAdmin(context, ePerson)).thenReturn(false);
        List<GrantedAuthority> authorities = ePersonRestAuthenticationProvider.getGrantedAuthorities(context, ePerson);
        assertEquals(WebSecurityConfiguration.EPERSON_GRANT ,authorities.get(0).getAuthority());
    }

}