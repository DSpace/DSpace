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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.AccessStatus;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultAccessStatusHelperTest  extends AbstractUnitTest {

    private static final Logger log = LogManager.getLogger(DefaultAccessStatusHelperTest.class);

    private Collection collection;
    private Community owningCommunity;
    private Item itemWithoutBundle;
    private Item itemWithoutBitstream;
    private Item itemWithBitstream;
    private Item itemWithEmbargo;
    private Item itemWithDateRestriction;
    private Item itemWithGroupRestriction;
    private Item itemWithoutPolicy;
    private Item itemWithoutPrimaryBitstream;
    private Item itemWithPrimaryAndMultipleBitstreams;
    private Item itemWithoutPrimaryAndMultipleBitstreams;
    private DefaultAccessStatusHelper helper;
    private LocalDate threshold;

    protected CommunityService communityService =
            ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService =
            ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService =
            ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService =
            ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService =
            ContentServiceFactory.getInstance().getInstallItemService();
    protected BundleService bundleService =
            ContentServiceFactory.getInstance().getBundleService();
    protected BitstreamService bitstreamService =
            ContentServiceFactory.getInstance().getBitstreamService();
    protected ResourcePolicyService resourcePolicyService =
            AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    protected GroupService groupService =
            EPersonServiceFactory.getInstance().getGroupService();
    protected EPersonService ePersonService =
            EPersonServiceFactory.getInstance().getEPersonService();

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();
            owningCommunity = communityService.create(null, context);
            collection = collectionService.create(context, owningCommunity);
            itemWithoutBundle = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithoutBitstream = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithBitstream = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithEmbargo = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithDateRestriction = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithGroupRestriction = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithoutPolicy = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithoutPrimaryBitstream = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithPrimaryAndMultipleBitstreams = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            itemWithoutPrimaryAndMultipleBitstreams = installItemService.installItem(context,
                    workspaceItemService.create(context, collection, true));
            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
        helper = new DefaultAccessStatusHelper();
        threshold = LocalDate.of(10000, 1, 1);
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy() {
        context.turnOffAuthorisationSystem();
        try {
            itemService.delete(context, itemWithoutBundle);
            itemService.delete(context, itemWithoutBitstream);
            itemService.delete(context, itemWithBitstream);
            itemService.delete(context, itemWithEmbargo);
            itemService.delete(context, itemWithDateRestriction);
            itemService.delete(context, itemWithGroupRestriction);
            itemService.delete(context, itemWithoutPolicy);
            itemService.delete(context, itemWithoutPrimaryBitstream);
            itemService.delete(context, itemWithPrimaryAndMultipleBitstreams);
            itemService.delete(context, itemWithoutPrimaryAndMultipleBitstreams);
        } catch (Exception e) {
            // ignore
        }
        try {
            collectionService.delete(context, collection);
        } catch (Exception e) {
            // ignore
        }
        try {
            communityService.delete(context, owningCommunity);
        } catch (Exception e) {
            // ignore
        }
        context.restoreAuthSystemState();
        itemWithoutBundle = null;
        itemWithoutBitstream = null;
        itemWithBitstream = null;
        itemWithEmbargo = null;
        itemWithDateRestriction = null;
        itemWithGroupRestriction = null;
        itemWithoutPolicy = null;
        itemWithoutPrimaryBitstream = null;
        itemWithPrimaryAndMultipleBitstreams = null;
        itemWithoutPrimaryAndMultipleBitstreams = null;
        collection = null;
        owningCommunity = null;
        helper = null;
        threshold = null;
        communityService = null;
        collectionService = null;
        itemService = null;
        workspaceItemService = null;
        installItemService = null;
        bundleService = null;
        bitstreamService = null;
        resourcePolicyService = null;
        groupService = null;
        try {
            super.destroy();
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Test for a null item
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithNullItem() throws Exception {
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                null, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithNullItem 0", status, equalTo(DefaultAccessStatusHelper.UNKNOWN));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertNull("testWithNullItem 1", availabilityDate);
    }

    /**
     * Test for an item with no bundle
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithoutBundle() throws Exception {
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutBundle, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithoutBundle 0", status, equalTo(DefaultAccessStatusHelper.METADATA_ONLY));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertNull("testWithoutBundle 1", availabilityDate);
    }

    /**
     * Test for an item with no bitstream
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithoutBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        bundleService.create(context, itemWithoutBitstream, Constants.CONTENT_BUNDLE_NAME);
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithoutBitstream 0", status, equalTo(DefaultAccessStatusHelper.METADATA_ONLY));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertNull("testWithoutBitstream 1", availabilityDate);
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                null, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithoutBitstream 3", bitstreamStatus, equalTo(DefaultAccessStatusHelper.UNKNOWN));
        LocalDate bitstreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertNull("testWithoutBitstream 4", bitstreamAvailabilityDate);
    }

    /**
     * Test for an item with a basic bitstream (open access)
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithBitstream, Constants.CONTENT_BUNDLE_NAME);
        Bitstream bitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bitstream.setName(context, "primary");
        bundle.setPrimaryBitstreamID(bitstream);
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithBitstream 0", status, equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertNull("testWithBitstream 1", availabilityDate);
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithBitstream 3", bitstreamStatus, equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate bitstreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertNull("testWithBitstream 4", bitstreamAvailabilityDate);
    }

    /**
     * Test for an item with an embargo
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithEmbargo() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithEmbargo, Constants.CONTENT_BUNDLE_NAME);
        Bitstream bitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bitstream.setName(context, "primary");
        bundle.setPrimaryBitstreamID(bitstream);
        List<ResourcePolicy> policies = new ArrayList<>();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        ResourcePolicy policy = resourcePolicyService.create(context, null, group);
        policy.setRpName("Embargo");
        policy.setAction(Constants.READ);
        LocalDate startDate = LocalDate.of(9999, 12, 31);
        policy.setStartDate(startDate);
        policies.add(policy);
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.addPolicies(context, policies, bitstream);
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithEmbargo 0", status, equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithEmbargo 1", availabilityDate, equalTo(startDate));
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithEmbargo 3", bitstreamStatus, equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate bitstreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertThat("testWithEmbargo 4", bitstreamAvailabilityDate, equalTo(startDate));
    }

    /**
     * Test for an item with an anonymous date restriction
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithDateRestriction() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithDateRestriction, Constants.CONTENT_BUNDLE_NAME);
        Bitstream bitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bitstream.setName(context, "primary");
        bundle.setPrimaryBitstreamID(bitstream);
        List<ResourcePolicy> policies = new ArrayList<>();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        ResourcePolicy policy = resourcePolicyService.create(context, null, group);
        policy.setRpName("Restriction");
        policy.setAction(Constants.READ);
        LocalDate startDate = LocalDate.of(10000, 1, 1);
        policy.setStartDate(startDate);
        policies.add(policy);
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.addPolicies(context, policies, bitstream);
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithDateRestriction, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithDateRestriction 0", status, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithDateRestriction 1", availabilityDate, equalTo(startDate));
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithDateRestriction 3", bitstreamStatus, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate bistreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertThat("testWithDateRestriction 4", bistreamAvailabilityDate, equalTo(startDate));
    }

    /**
     * Test for an item with a group restriction
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithGroupRestriction() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithGroupRestriction, Constants.CONTENT_BUNDLE_NAME);
        Bitstream bitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bitstream.setName(context, "primary");
        bundle.setPrimaryBitstreamID(bitstream);
        List<ResourcePolicy> policies = new ArrayList<>();
        Group group = groupService.findByName(context, Group.ADMIN);
        ResourcePolicy policy = resourcePolicyService.create(context, null, group);
        policy.setRpName("Restriction");
        policy.setAction(Constants.READ);
        policies.add(policy);
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.addPolicies(context, policies, bitstream);
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithGroupRestriction, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithGroupRestriction 0", status, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithGroupRestriction 1", availabilityDate, equalTo(threshold));
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithGroupRestriction 3", bitstreamStatus, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate bitstreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertThat("testWithGroupRestriction 4", bitstreamAvailabilityDate, equalTo(threshold));
    }

    /**
     * Test for an item with no policy
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithoutPolicy() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithoutPolicy, Constants.CONTENT_BUNDLE_NAME);
        Bitstream bitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bitstream.setName(context, "primary");
        bundle.setPrimaryBitstreamID(bitstream);
        authorizeService.removeAllPolicies(context, bitstream);
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutPolicy, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithoutPolicy 0", status, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithoutPolicy 1", availabilityDate, equalTo(threshold));
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithoutPolicy 3", bitstreamStatus, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate bitstreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertThat("testWithoutPolicy 4", bitstreamAvailabilityDate, equalTo(threshold));
    }

    /**
     * Test for an item with no primary bitstream
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithoutPrimaryBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithoutPrimaryBitstream, Constants.CONTENT_BUNDLE_NAME);
        Bitstream bitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bitstream.setName(context, "first");
        context.restoreAuthSystemState();
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutPrimaryBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithoutPrimaryBitstream 0", status, equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertNull("testWithoutPrimaryBitstream 1", availabilityDate);
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithoutPrimaryBitstream 3", bitstreamStatus, equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate bitstreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertNull("testWithoutPrimaryBitstream 4", bitstreamAvailabilityDate);
    }

    /**
     * Test for an item with an open access bitstream
     * and another primary bitstream on embargo
     * @throws java.lang.Exception passed through.
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
     * @throws java.lang.Exception passed through.
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

    /**
     * Test for an item with an embargo for both configurations (current, anonymous) and as a guest
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithEmbargoForCurrentOrAnonymousAsGuest() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithEmbargo, Constants.CONTENT_BUNDLE_NAME);
        Bitstream bitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bitstream.setName(context, "primary");
        bundle.setPrimaryBitstreamID(bitstream);
        List<ResourcePolicy> policies = new ArrayList<>();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        ResourcePolicy policy = resourcePolicyService.create(context, null, group);
        policy.setRpName("Embargo");
        policy.setAction(Constants.READ);
        LocalDate startDate = LocalDate.of(9999, 12, 31);
        policy.setStartDate(startDate);
        policies.add(policy);
        EPerson admin = ePersonService.create(context);
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        ResourcePolicy adminPolicy = resourcePolicyService.create(context, admin, adminGroup);
        adminPolicy.setRpName("Open Access For Admin");
        adminPolicy.setAction(Constants.READ);
        policies.add(adminPolicy);
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.addPolicies(context, policies, bitstream);
        context.restoreAuthSystemState();
        // Configuration: current
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsGuest 1", status,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsGuest 2", availabilityDate, equalTo(startDate));
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsGuest 3", bitstreamStatus,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate bistreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsGuest 4", bistreamAvailabilityDate, equalTo(startDate));
        // Configuration: anonymous
        // getAccessStatusFromItem
        accessStatus = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS);
        status = accessStatus.getStatus();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsGuest 5", status,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsGuest 6", availabilityDate, equalTo(startDate));
        // getAccessStatusFromBitstream
        accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS);
        bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsGuest 7", bitstreamStatus,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        bistreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsGuest 8", bistreamAvailabilityDate, equalTo(startDate));
    }

    /**
     * Test for an item with an embargo for both configurations (current, anonymous) and as an admin
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithEmbargoForCurrentOrAnonymousAsAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle bundle = bundleService.create(context, itemWithEmbargo, Constants.CONTENT_BUNDLE_NAME);
        Bitstream bitstream = bitstreamService.create(context, bundle,
                new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)));
        bitstream.setName(context, "primary");
        bundle.setPrimaryBitstreamID(bitstream);
        List<ResourcePolicy> policies = new ArrayList<>();
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        ResourcePolicy policy = resourcePolicyService.create(context, null, group);
        policy.setRpName("Embargo");
        policy.setAction(Constants.READ);
        LocalDate startDate = LocalDate.of(9999, 12, 31);
        policy.setStartDate(startDate);
        policies.add(policy);
        EPerson admin = ePersonService.create(context);
        Group adminGroup = groupService.findByName(context, Group.ADMIN);
        ResourcePolicy adminPolicy = resourcePolicyService.create(context, admin, adminGroup);
        adminPolicy.setRpName("Open Access For Admin");
        adminPolicy.setAction(Constants.READ);
        policies.add(adminPolicy);
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.addPolicies(context, policies, bitstream);
        context.restoreAuthSystemState();
        EPerson currentUser = context.getCurrentUser();
        context.setCurrentUser(admin);
        // Configuration: current
        // getAccessStatusFromItem
        AccessStatus accessStatus = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getStatus();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsAdmin 1", status,
                equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate availabilityDate = accessStatus.getAvailabilityDate();
        assertNull("testWithEmbargoForCurrentOrAnonymousAsAdmin 2", availabilityDate);
        // getAccessStatusFromBitstream
        AccessStatus accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsAdmin 3", bitstreamStatus,
                equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate bitstreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertNull("testWithEmbargoForCurrentOrAnonymousAsAdmin 4", bitstreamAvailabilityDate);
        // Configuration: anonymous
        accessStatus = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS);
        status = accessStatus.getStatus();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsAdmin 5", status,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        availabilityDate = accessStatus.getAvailabilityDate();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsAdmin 6", availabilityDate, equalTo(startDate));
        // getAccessStatusFromBitstream
        accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS);
        bitstreamStatus = accessStatusBitstream.getStatus();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsAdmin 7", bitstreamStatus,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        bitstreamAvailabilityDate = accessStatusBitstream.getAvailabilityDate();
        assertThat("testWithEmbargoForCurrentOrAnonymousAsAdmin 8", bitstreamAvailabilityDate, equalTo(startDate));
        context.setCurrentUser(currentUser);
    }
}
