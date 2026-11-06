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

public class DefaultAccessStatusHelperTest extends AbstractAccessStatusHelperTest {

    @Before
    @Override
    public void init() {
        super.init();
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        configurationService.setProperty(
            "plugin.single.org.dspace.access.status.AccessStatusHelper",
            "org.dspace.access.status.DefaultAccessStatusHelper"
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
        assertThat("testWithNoPrimaryAndMultipleBitstreams 0", status,
            equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertNull("testWithNoPrimaryAndMultipleBitstreams 1", availabilityDate);
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

}
