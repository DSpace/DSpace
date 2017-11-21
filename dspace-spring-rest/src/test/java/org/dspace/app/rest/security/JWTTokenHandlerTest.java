package org.dspace.app.rest.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class JWTTokenHandlerTest {

    @InjectMocks
    @Spy
    JWTTokenHandler jwtTokenHandler;

    @Mock
    private Context context;

    @Mock
    private EPerson ePerson;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private EPersonService ePersonService;

    @Mock
    private EPersonClaimProvider ePersonClaimProvider;

    @Spy
    private List<JWTClaimProvider> jwtClaimProviders = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        when(ePerson.getEmail()).thenReturn("test@dspace.org");
        when(ePerson.getSessionSalt()).thenReturn("01234567890123456789012345678901");
        when(ePerson.getLastActive()).thenReturn(new Date());
        when(context.getCurrentUser()).thenReturn(ePerson);
        when(ePersonClaimProvider.getKey()).thenReturn( "eid");
        when(ePersonClaimProvider.getValue(any(), Mockito.any(HttpServletRequest.class))).thenReturn("epersonID");
        jwtClaimProviders.add(ePersonClaimProvider);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testJWTNoEncryption() throws Exception {
        Date previous = new Date(new Date().getTime() - 10000000000L);
        String token = jwtTokenHandler.createTokenForEPerson(context, new MockHttpServletRequest(), previous, new ArrayList<>());
        SignedJWT signedJWT = SignedJWT.parse(token);
        String personId = (String) signedJWT.getJWTClaimsSet().getClaim("eid");
        assertEquals("epersonID", personId);
    }

    //temporary set a negative expiration time so the token is invalid immediately
    @Test
    public void testExpiredToken() throws Exception {
        when(jwtTokenHandler.getExpirationTime()).thenReturn(-99999999L);
        when(ePersonClaimProvider.getEPerson(any(Context.class), any(JWTClaimsSet.class))).thenReturn(ePerson);
        Date previous = new Date(new Date().getTime() - 10000000000L);
        String token = jwtTokenHandler.createTokenForEPerson(context, new MockHttpServletRequest(), previous, new ArrayList<>());
        EPerson parsed = jwtTokenHandler.parseEPersonFromToken(token, httpServletRequest, context);
        assertEquals(null, parsed);

    }

    //Try if we can change the expiration date
    @Test
    public void testTokenTampering() throws Exception {
        when(jwtTokenHandler.getExpirationTime()).thenReturn(-99999999L);
        when(ePersonClaimProvider.getEPerson(any(Context.class), any(JWTClaimsSet.class))).thenReturn(ePerson);
        Date previous = new Date(new Date().getTime() - 10000000000L);
        String token = jwtTokenHandler.createTokenForEPerson(context, new MockHttpServletRequest(), previous, new ArrayList<>());
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().claim("eid", "epersonID").expirationTime(new Date(System.currentTimeMillis() + 99999999)).build();
        String tamperedPayload = new String(Base64.getUrlEncoder().encode(jwtClaimsSet.toString().getBytes()));
        String[] splitToken = token.split("\\.");
        String tamperedToken = splitToken[0] + "." + tamperedPayload + "." + splitToken[2];
        EPerson parsed = jwtTokenHandler.parseEPersonFromToken(tamperedToken, httpServletRequest, context);
        assertEquals(null, parsed);
    }










}