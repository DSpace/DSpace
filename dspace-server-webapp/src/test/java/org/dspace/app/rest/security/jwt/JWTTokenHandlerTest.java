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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
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
 * Unit Tests for LoginJWTTokenHandler and JWTTokenHandler.
 * <P>
 * NOTE: This only tests basic features of our JSON Web Token (JWT). See also AuthenticationRestControllerIT,
 * which includes integration tests for JWT features that are easier to test with real data.
 *
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
    protected EPersonService ePersonService;

    @Mock
    protected EPersonClaimProvider ePersonClaimProvider;

    @Spy
    protected List<JWTClaimProvider> jwtClaimProviders = new ArrayList<>();

    protected Date previousLoginDate;

    @Before
    public void setUp() throws Exception {
        when(ePerson.getSessionSalt()).thenReturn("01234567890123456789012345678901");
        when(ePerson.getLastActive()).thenReturn(new Date());
        when(context.getCurrentUser()).thenReturn(ePerson);
        when(ePersonClaimProvider.getKey()).thenReturn("eid");
        when(ePersonClaimProvider.getValue(any(), Mockito.any(HttpServletRequest.class))).thenReturn("epersonID");
        jwtClaimProviders.add(ePersonClaimProvider);

        // Set last login date to be a long time ago
        previousLoginDate = new Date(System.currentTimeMillis() - 10000000000L);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testJWTNoEncryption() throws Exception {
        // Verify that a token created
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previousLoginDate, new ArrayList<>());
        SignedJWT signedJWT = SignedJWT.parse(token);
        String personId = (String) signedJWT.getJWTClaimsSet().getClaim(EPersonClaimProvider.EPERSON_ID);
        assertEquals("epersonID", personId);
    }

    @Test(expected = ParseException.class)
    public void testJWTEncrypted() throws Exception {
        when(loginJWTTokenHandler.isEncryptionEnabled()).thenReturn(true);
        StringKeyGenerator keyGenerator = KeyGenerators.string();
        when(configurationService.getProperty("jwt.login.encryption.secret")).thenReturn(keyGenerator.generateKey());
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previousLoginDate, new ArrayList<>());
        // This should throw an exception as you cannot parse an encrypted JWT
        SignedJWT.parse(token);
    }

    // This test simply "sets up" the tests below. It proves that normally parseEPersonFromToken() will
    // return an ePerson object (except in scenarios tested below)
    @Test
    public void testParseEPersonFromToken() throws Exception {
        when(ePersonClaimProvider.getEPerson(any(Context.class), any(JWTClaimsSet.class))).thenReturn(ePerson);
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previousLoginDate, new ArrayList<>());
        EPerson parsed = loginJWTTokenHandler.parseEPersonFromToken(token, new MockHttpServletRequest(), context);
        assertEquals(ePerson, parsed);
    }

    //temporary set a negative expiration time so the token is invalid immediately
    @Test
    public void testExpiredToken() throws Exception {
        when(loginJWTTokenHandler.getExpirationPeriod()).thenReturn(-99999999L);
        when(ePersonClaimProvider.getEPerson(any(Context.class), any(JWTClaimsSet.class))).thenReturn(ePerson);
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previousLoginDate, new ArrayList<>());
        EPerson parsed = loginJWTTokenHandler.parseEPersonFromToken(token, new MockHttpServletRequest(), context);
        assertEquals(null, parsed);
    }

    //Try if we can change the expiration date
    @Test
    public void testTokenTampering() throws Exception {
        when(loginJWTTokenHandler.getExpirationPeriod()).thenReturn(-99999999L);
        when(ePersonClaimProvider.getEPerson(any(Context.class), any(JWTClaimsSet.class))).thenReturn(ePerson);
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previousLoginDate, new ArrayList<>());
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().claim("eid", "epersonID").expirationTime(
            new Date(System.currentTimeMillis() + 99999999)).build();
        String tamperedPayload = new String(Base64.getUrlEncoder().encode(jwtClaimsSet.toString().getBytes()));
        String[] splitToken = token.split("\\.");
        String tamperedToken = splitToken[0] + "." + tamperedPayload + "." + splitToken[2];
        EPerson parsed = loginJWTTokenHandler.parseEPersonFromToken(tamperedToken, new MockHttpServletRequest(),
                                                                    context);
        assertEquals(null, parsed);
    }

    @Test
    public void testInvalidatedToken() throws Exception {
        // create a new token
        String token = loginJWTTokenHandler
            .createTokenForEPerson(context, new MockHttpServletRequest(), previousLoginDate, new ArrayList<>());
        // immediately invalidate it
        loginJWTTokenHandler.invalidateToken(token, new MockHttpServletRequest(), context);
        // Check if it is still valid by trying to parse the EPerson from it (should return null)
        EPerson parsed = loginJWTTokenHandler.parseEPersonFromToken(token, new MockHttpServletRequest(), context);
        assertEquals(null, parsed);
    }

}
