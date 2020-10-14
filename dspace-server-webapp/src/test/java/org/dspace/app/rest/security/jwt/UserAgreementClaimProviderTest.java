/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import static org.dspace.content.Item.ANY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jwt.JWTClaimsSet;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link UserAgreementClaimProvider}.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class UserAgreementClaimProviderTest {

    @InjectMocks
    private UserAgreementClaimProvider userAgreementClaimProvider;

    @Mock
    private EPersonService ePersonService;

    @Mock
    private Context context;

    @Mock
    private HttpServletRequest request;

    @Mock
    private EPerson ePerson;

    private UUID ePersonUUID = UUID.randomUUID();

    @Before
    public void setup() throws SQLException {
        when(context.getCurrentUser()).thenReturn(ePerson);
        when(ePerson.getID()).thenReturn(ePersonUUID);
        when(ePersonService.find(context, ePersonUUID)).thenReturn(ePerson);
    }

    @Test
    public void testGetValueWhenEPersonThatHasAgreementMetadata() throws SQLException {
        when(ePersonService.getMetadataFirstValue(ePerson, "dspace", "agreements", "end-user", ANY)).thenReturn("true");
        Object value = userAgreementClaimProvider.getValue(context, request);
        assertEquals("true", value);

        verify(ePersonService).find(context, ePersonUUID);
        verify(ePersonService).getMetadataFirstValue(ePerson, "dspace", "agreements", "end-user", ANY);
        verify(ePersonService).getMetadataFirstValue(ePerson, "dspace", "agreements", "ignore", ANY);
        verifyNoMoreInteractions(ePersonService);
    }

    @Test
    public void testGetValueWhenEPersonThatHasNotAgreementMetadata() throws SQLException {
        when(ePersonService.getMetadataFirstValue(ePerson, "dspace", "agreements", "end-user", ANY)).thenReturn(null);
        Object value = userAgreementClaimProvider.getValue(context, request);
        assertEquals("false", value);

        verify(ePersonService).find(context, ePersonUUID);
        verify(ePersonService).getMetadataFirstValue(ePerson, "dspace", "agreements", "end-user", ANY);
        verify(ePersonService).getMetadataFirstValue(ePerson, "dspace", "agreements", "ignore", ANY);
        verifyNoMoreInteractions(ePersonService);
    }

    @Test
    public void testGetValueWithIgnoreSetToTrue() throws SQLException {
        when(ePersonService.getMetadataFirstValue(ePerson, "dspace", "agreements", "end-user", ANY)).thenReturn(null);
        when(ePersonService.getMetadataFirstValue(ePerson, "dspace", "agreements", "ignore", ANY)).thenReturn("true");
        Object value = userAgreementClaimProvider.getValue(context, request);
        assertEquals("true", value);

        verify(ePersonService).find(context, ePersonUUID);
        verify(ePersonService).getMetadataFirstValue(ePerson, "dspace", "agreements", "end-user", ANY);
        verify(ePersonService).getMetadataFirstValue(ePerson, "dspace", "agreements", "ignore", ANY);
        verifyNoMoreInteractions(ePersonService);
    }

    @Test
    public void testParseClaimWithTrueValue() throws SQLException {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .claim(UserAgreementClaimProvider.USER_AGREEMENT_ACCEPTED, "true")
            .build();

        userAgreementClaimProvider.parseClaim(context, request, jwtClaimsSet);
        verify(request).setAttribute(UserAgreementClaimProvider.USER_AGREEMENT_ACCEPTED, true);
    }

    @Test
    public void testParseClaimWithTrueFalse() throws SQLException {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .claim(UserAgreementClaimProvider.USER_AGREEMENT_ACCEPTED, "false")
            .build();

        userAgreementClaimProvider.parseClaim(context, request, jwtClaimsSet);
        verify(request).setAttribute(UserAgreementClaimProvider.USER_AGREEMENT_ACCEPTED, false);
    }
}
