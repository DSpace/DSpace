/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.AccessStatus;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.core.Constants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class OrderedAccessStatusHelperTest extends AbstractAccessStatusHelperTest {

    private ConfigurationService configurationService;

    @Before
    @Override
    public void init() {
        super.init();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        configurationService.setProperty(
            "plugin.single.org.dspace.access.status.AccessStatusHelper",
            "org.dspace.access.status.OrderedAccessStatusHelper"
        );
        helper = (AccessStatusHelper) pluginService.getSinglePlugin(AccessStatusHelper.class);
    }

    /**
     * Test for an item with an open access bitstream
     * and another primary bitstream on embargo
     * @throws Exception passed through.
     */
    @Test
    public void testWithPrimaryAndMultipleBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithPrimaryAndMultipleBitstreams,
                Constants.CONTENT_BUNDLE_NAME);
        Bitstream otherBitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        Bitstream primaryBitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bundle.setPrimaryBitstreamID(primaryBitstream);
        List<ResourcePolicy> policies = new ArrayList<>();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        ResourcePolicy policy = resourcePolicyService.create(context, null, group);
        policy.setRpName("Embargo");
        policy.setAction(Constants.READ);
        LocalDate startDate = LocalDate.of(9999, 12, 31);
        policy.setStartDate(startDate);
        policies.add(policy);
        authorizeService.removeAllPolicies(context, primaryBitstream);
        authorizeService.addPolicies(context, policies, primaryBitstream);
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithPrimaryAndMultipleBitstreams, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithPrimaryAndMultipleBitstreams 0", status, equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithPrimaryAndMultipleBitstreams 1", availabilityDate, equalTo(startDate));
        // getAccessStatusFromBitstream -> primary
        AccessStatus accessStatusPrimaryBitstream = helper.getAccessStatusFromBitstream(context,
                primaryBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String primaryBitstreamStatus = accessStatusPrimaryBitstream.getStatus();
        assertThat("testWithPrimaryAndMultipleBitstreams 3", primaryBitstreamStatus,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate primaryAvailabilityDate = accessStatusPrimaryBitstream.getAvailabilityDate();
        assertThat("testWithPrimaryAndMultipleBitstreams 4", primaryAvailabilityDate, equalTo(startDate));
        // getAccessStatusFromBitstream -> other
        AccessStatus accessStatusOtherBitstream = helper.getAccessStatusFromBitstream(context,
                otherBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String otherBitstreamStatus = accessStatusOtherBitstream.getStatus();
        assertThat("testWithPrimaryAndMultipleBitstreams 5", otherBitstreamStatus,
                equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate otherAvailabilityDate = accessStatusOtherBitstream.getAvailabilityDate();
        assertNull("testWithPrimaryAndMultipleBitstreams 6", otherAvailabilityDate);
    }

    /**
     * Test for an item with an open access bitstream
     * and another bitstream on embargo
     * @throws Exception passed through.
     */
    @Test
    public void testWithNoPrimaryAndMultipleBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithoutPrimaryAndMultipleBitstreams,
                Constants.CONTENT_BUNDLE_NAME);
        Bitstream firstBitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        Bitstream anotherBitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        List<ResourcePolicy> policies = new ArrayList<>();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        ResourcePolicy policy = resourcePolicyService.create(context, null, group);
        policy.setRpName("Embargo");
        policy.setAction(Constants.READ);
        LocalDate startDate = LocalDate.of(9999, 12, 31);
        policy.setStartDate(startDate);
        policies.add(policy);
        authorizeService.removeAllPolicies(context, anotherBitstream);
        authorizeService.addPolicies(context, policies, anotherBitstream);
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutPrimaryAndMultipleBitstreams, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 1", status,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 2", availabilityDate, equalTo(startDate));
        // getAccessStatusFromBitstream -> first
        AccessStatus accessStatusFirstBitstream = helper.getAccessStatusFromBitstream(context,
                firstBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String firstBitstreamStatus = accessStatusFirstBitstream.getStatus();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 3", firstBitstreamStatus,
                equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate firstAvailabilityDate = accessStatusFirstBitstream.getAvailabilityDate();
        assertNull("testWithNoPrimaryAndMultipleBitstreams 4", firstAvailabilityDate);
        // getAccessStatusFromBitstream -> other
        AccessStatus accessStatusOtherBitstream = helper.getAccessStatusFromBitstream(context,
                anotherBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String otherBitstreamStatus = accessStatusOtherBitstream.getStatus();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 5", otherBitstreamStatus,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate otherAvailabilityDate = accessStatusOtherBitstream.getAvailabilityDate();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 6", otherAvailabilityDate, equalTo(startDate));
    }

    /**
     * Test that reversing the configured access.status.order flips the precedence: an item with one
     * open-access and one embargoed bitstream resolves to OPEN_ACCESS instead of the default EMBARGO.
     * @throws Exception passed through.
     */
    @Test
    public void testReversedAccessStatusOrder() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithoutPrimaryAndMultipleBitstreams,
                Constants.CONTENT_BUNDLE_NAME);
        bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        Bitstream embargoedBitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        List<ResourcePolicy> policies = new ArrayList<>();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        ResourcePolicy policy = resourcePolicyService.create(context, null, group);
        policy.setRpName("Embargo");
        policy.setAction(Constants.READ);
        LocalDate startDate = LocalDate.of(9999, 12, 31);
        policy.setStartDate(startDate);
        policies.add(policy);
        authorizeService.removeAllPolicies(context, embargoedBitstream);
        authorizeService.addPolicies(context, policies, embargoedBitstream);
        context.restoreAuthSystemState();

        // With the default order, EMBARGO wins over OPEN_ACCESS
        AccessStatus defaultStatus = helper.getAccessStatusFromItem(context,
                itemWithoutPrimaryAndMultipleBitstreams, threshold,
                DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        assertThat("testReversedAccessStatusOrder default", defaultStatus.getStatus(),
                equalTo(DefaultAccessStatusHelper.EMBARGO));

        try {
            // OPEN_ACCESS now has the highest precedence
            configurationService.setProperty(
                    OrderedAccessStatusHelper.ACCESS_STATUS_ORDER_PROPERTY,
                    String.join(",",
                            DefaultAccessStatusHelper.OPEN_ACCESS,
                            DefaultAccessStatusHelper.UNKNOWN,
                            DefaultAccessStatusHelper.RESTRICTED,
                            DefaultAccessStatusHelper.EMBARGO));

            AccessStatus reversedStatus = helper.getAccessStatusFromItem(context,
                    itemWithoutPrimaryAndMultipleBitstreams, threshold,
                    DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
            assertThat("testReversedAccessStatusOrder reversed", reversedStatus.getStatus(),
                    equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
            assertNull("testReversedAccessStatusOrder reversed availability",
                    reversedStatus.getAvailabilityDate());
        } finally {
            configurationService.setProperty(OrderedAccessStatusHelper.ACCESS_STATUS_ORDER_PROPERTY, null);
        }
    }

}
