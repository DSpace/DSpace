/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This class verifies that {@link BitstreamMetadataReadPermissionEvaluatorPlugin} resolves its
 * target through a typed {@link BitstreamService} lookup.
 *
 * <p>The plugin only ever handles Bitstreams, so it must not resolve the target with the untyped
 * {@link org.dspace.app.rest.utils.DSpaceObjectUtils#findDSpaceObject(Context, UUID)} sweep: that
 * sweep calls {@code find} on every {@code DSpaceObjectService} in turn, and loading a Bitstream's
 * UUID as a Collection or Community makes Hibernate log a spurious {@code HHH000327} /
 * {@code ObjectNotFoundException} for every Collection or Community logo (see #12839).</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class BitstreamMetadataReadPermissionEvaluatorPluginTest {

    private static final String METADATA_READ = "METADATA_READ";

    @InjectMocks
    private BitstreamMetadataReadPermissionEvaluatorPlugin plugin;

    @Mock
    private RequestService requestService;
    @Mock
    private AuthorizeService authorizeService;
    @Mock
    private BitstreamService bitstreamService;

    @Mock
    private Request request;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private Context context;
    @Mock
    private Bitstream bitstream;

    private MockedStatic<ContextUtil> contextUtil;
    private UUID uuid;

    @Before
    public void setUp() throws Exception {
        uuid = UUID.randomUUID();
        contextUtil = mockStatic(ContextUtil.class);
        when(requestService.getCurrentRequest()).thenReturn(request);
        when(request.getHttpServletRequest()).thenReturn(httpServletRequest);
        contextUtil.when(() -> ContextUtil.obtainContext(httpServletRequest)).thenReturn(context);
    }

    @After
    public void tearDown() throws Exception {
        contextUtil.close();
    }

    @Test
    public void resolvesTargetViaBitstreamService() throws Exception {
        when(bitstreamService.find(context, uuid)).thenReturn(bitstream);
        when(authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ)).thenReturn(true);

        assertTrue(plugin.hasPermission(null, uuid.toString(), "BITSTREAM", METADATA_READ));

        // The target must be looked up as a Bitstream and as nothing else, so that no wrong-type
        // load is ever issued for it (see #12839).
        verify(bitstreamService).find(context, uuid);
        verifyNoMoreInteractions(bitstreamService);
    }

    @Test
    public void returnsFalseWhenTargetIsNotABitstream() throws Exception {
        when(bitstreamService.find(context, uuid)).thenReturn(null);

        assertFalse(plugin.hasPermission(null, uuid.toString(), "BITSTREAM", METADATA_READ));

        verify(bitstreamService).find(context, uuid);
        verifyNoMoreInteractions(bitstreamService);
    }
}
