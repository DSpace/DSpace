/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

/**
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class JWTTokenHandlerTest {

    @InjectMocks
    @Spy
    private LoginJWTTokenHandler loginJWTTokenHandler;

    @Mock
    protected ConfigurationService configurationService;

    @Mock
    protected Context context;

    @Mock
    protected EPerson ePerson;

    @Mock
    protected HttpServletRequest httpServletRequest;

    @Mock
    protected EPersonService ePersonService;

    @Mock
    protected EPersonClaimProvider ePersonClaimProvider;

    @Mock
    protected ClientInfoService clientInfoService;

    @Spy
    protected List<JWTClaimProvider> jwtClaimProviders = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        when(ePerson.getSessionSalt()).thenReturn("01234567890123456789012345678901");
        when(ePerson.getLastActive()).thenReturn(Instant.now());
        when(context.getCurrentUser()).thenReturn(ePerson);
        when(clientInfoService.getClientIp(any())).thenReturn("123.123.123.123");
        when(ePersonClaimProvider.getKey()).thenReturn("eid");
        when(ePersonClaimProvider.getValue(any(), Mockito.any(HttpServletRequest.class))).thenReturn("epersonID");
        jwtClaimProviders.add(ePersonClaimProvider);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testJWTNoEncryption() throws Exception {
        Instant previous = Instant.now().minus(10000000000L, ChronoUnit.MILLIS);
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previous);
        SignedJWT signedJWT = SignedJWT.parse(token);
        String personId = (String) signedJWT.getJWTClaimsSet().getClaim(EPersonClaimProvider.EPERSON_ID);
        assertEquals("epersonID", personId);
    }

    @Test(expected = ParseException.class)
    public void testJWTEncrypted() throws Exception {
        when(loginJWTTokenHandler.isEncryptionEnabled()).thenReturn(true);
        Instant previous = Instant.now().minus(10000000000L, ChronoUnit.MILLIS);
        StringKeyGenerator keyGenerator = KeyGenerators.string();
        when(configurationService.getProperty("jwt.login.encryption.secret")).thenReturn(keyGenerator.generateKey());
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previous);
        SignedJWT signedJWT = SignedJWT.parse(token);
    }

    //temporary set a negative expiration time so the token is invalid immediately
    @Test
    public void testExpiredToken() throws Exception {
        when(configurationService.getLongProperty("jwt.login.token.expiration", 1800000)).thenReturn(-99999999L);
        when(ePersonClaimProvider.getEPerson(any(Context.class), any(JWTClaimsSet.class))).thenReturn(ePerson);
        Instant previous = Instant.now().minus(10000000000L, ChronoUnit.MILLIS);
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previous);
        EPerson parsed = loginJWTTokenHandler.parseEPersonFromToken(token, httpServletRequest, context);
        assertEquals(null, parsed);

    }

    //Try if we can change the expiration date
    @Test
    public void testTokenTampering() throws Exception {
        when(loginJWTTokenHandler.getExpirationPeriod()).thenReturn(-99999999L);
        when(ePersonClaimProvider.getEPerson(any(Context.class), any(JWTClaimsSet.class))).thenReturn(ePerson);
        Instant previous = Instant.now().minus(10000000000L, ChronoUnit.MILLIS);
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previous);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().claim("eid", "epersonID").expirationTime(
            java.util.Date.from(Instant.now().plus(99999999, ChronoUnit.MILLIS))).build();
        String tamperedPayload = new String(Base64.getUrlEncoder().encode(jwtClaimsSet.toString().getBytes()));
        String[] splitToken = token.split("\\.");
        String tamperedToken = splitToken[0] + "." + tamperedPayload + "." + splitToken[2];
        EPerson parsed = loginJWTTokenHandler.parseEPersonFromToken(tamperedToken, httpServletRequest, context);
        assertEquals(null, parsed);
    }

    @Test
    public void testInvalidatedToken() throws Exception {
        Instant previous = Instant.now().minus(10000000000L, ChronoUnit.MILLIS);
        // create a new token
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previous);
        // immediately invalidate it
        loginJWTTokenHandler.invalidateToken(token, new MockHttpServletRequest(), context);
        // Check if it is still valid by trying to parse the EPerson from it (should return null)
        EPerson parsed = loginJWTTokenHandler.parseEPersonFromToken(token, httpServletRequest, context);
        assertEquals(null, parsed);
    }

}
