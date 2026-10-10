/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.dspace.app.rest.authorization.impl.AuthorizationFeatureServiceImpl;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.junit.Before;
import org.junit.Test;

/**
 * Verify the shared authentication prerequisite without evaluating feature-specific policies.
 */
public class AuthorizationFeatureServiceTest {
    private final AuthorizationFeatureService service = new AuthorizationFeatureServiceImpl();
    private final Context context = mock(Context.class);
    private final AuthorizationFeature feature = mock(AuthorizationFeature.class);
    private final SiteRest site = new SiteRest();

    @Before
    public void setUp() {
        when(feature.getSupportedTypes()).thenReturn(new String[] {site.getUniqueType()});
    }

    @Test
    public void anonymousRequiredFeatureIsNotEvaluated() throws Exception {
        when(feature.requiresAuthentication()).thenReturn(true);
        assertFalse(service.isAuthorized(context, feature, site));
        verify(feature, never()).isAuthorized(context, site);
    }

    @Test
    public void authenticatedRequiredFeatureStillEvaluatesPermission() throws Exception {
        when(feature.requiresAuthentication()).thenReturn(true);
        when(context.getCurrentUser()).thenReturn(mock(EPerson.class));
        assertFalse(service.isAuthorized(context, feature, site));
        when(feature.isAuthorized(context, site)).thenReturn(true);
        assertTrue(service.isAuthorized(context, feature, site));
    }

    @Test
    public void anonymousOptionalFeatureRetainsItsDecision() throws Exception {
        when(feature.isAuthorized(context, site)).thenReturn(true);
        assertTrue(service.isAuthorized(context, feature, site));
        when(feature.isAuthorized(context, site)).thenReturn(false);
        assertFalse(service.isAuthorized(context, feature, site));
    }

    @Test
    public void unknownFeatureAndUnsupportedObjectsRemainDenied() throws Exception {
        assertFalse(service.isAuthorized(context, null, site));
        assertFalse(service.isAuthorized(context, feature, null));
        when(feature.getSupportedTypes()).thenReturn(new String[] {"core.item"});
        assertFalse(service.isAuthorized(context, feature, site));
        verify(feature, never()).isAuthorized(context, site);
    }

    @Test
    public void customFeaturesDefaultToNormalEvaluation() throws Exception {
        AuthorizationFeature customFeature = new AlwaysTrueFeature();
        assertFalse(customFeature.requiresAuthentication());
        assertTrue(service.isAuthorized(context, customFeature, site));
    }

    @Test
    public void guardUsesTheEffectiveContextUser() throws Exception {
        when(feature.requiresAuthentication()).thenReturn(true);
        when(feature.isAuthorized(context, site)).thenReturn(true);
        when(context.getCurrentUser()).thenReturn(mock(EPerson.class));
        assertTrue(service.isAuthorized(context, feature, site));
        when(context.getCurrentUser()).thenReturn(null);
        assertFalse(service.isAuthorized(context, feature, site));
    }
}
