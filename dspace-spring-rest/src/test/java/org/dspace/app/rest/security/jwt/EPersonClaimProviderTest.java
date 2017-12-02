/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jwt.JWTClaimsSet;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Atmire NV (info at atmire dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class EPersonClaimProviderTest {

    @InjectMocks
    private EPersonClaimProvider ePersonClaimProvider;

    @Mock
    private EPerson ePerson;

    private Context context;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private EPersonService ePersonService;

    private JWTClaimsSet jwtClaimsSet;


    @Before
    public void setUp() throws Exception {
        context = Mockito.mock(Context.class);
        Mockito.doCallRealMethod().when(context).setCurrentUser(any(EPerson.class));
        Mockito.doCallRealMethod().when(context).getCurrentUser();
        when(ePerson.getID()).thenReturn(UUID.fromString("c3bae216-a481-496b-a524-7df5aabdb609"));
        jwtClaimsSet = new JWTClaimsSet.Builder()
                .claim(EPersonClaimProvider.EPERSON_ID, "c3bae216-a481-496b-a524-7df5aabdb609")
                .build();
        when(ePersonService.find(any(), any(UUID.class))).thenReturn(ePerson);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testParseClaim() throws Exception {
        ePersonClaimProvider.parseClaim(context, httpServletRequest, jwtClaimsSet);

        verify(context).setCurrentUser(ePerson);
    }

    @Test
    public void testGetEPerson() throws Exception {
        EPerson parsed = ePersonClaimProvider.getEPerson(context, jwtClaimsSet);
        assertEquals(parsed.getID(), UUID.fromString("c3bae216-a481-496b-a524-7df5aabdb609" ));
    }

}