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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                null, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithNullItem 0", status, equalTo(DefaultAccessStatusHelper.UNKNOWN));
        String embargoDate = helper.getEmbargoFromItem(context, null, threshold);
        assertNull("testWithNullItem 1", embargoDate);
    }

    /**
     * Test for an item with no bundle
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithoutBundle() throws Exception {
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutBundle, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithoutBundle 0", status, equalTo(DefaultAccessStatusHelper.METADATA_ONLY));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithoutBundle, threshold);
        assertNull("testWithoutBundle 1", embargoDate);
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithoutBitstream 0", status, equalTo(DefaultAccessStatusHelper.METADATA_ONLY));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithoutBitstream, threshold);
        assertNull("testWithoutBitstream 1", embargoDate);
        Pair<String, LocalDate> accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                null, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getLeft();
        assertThat("testWithoutBitstream 2", bitstreamStatus, equalTo(DefaultAccessStatusHelper.UNKNOWN));
        LocalDate availabilityDate = accessStatusBitstream.getRight();
        assertNull("testWithoutBitstream 3", availabilityDate);
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithBitstream 0", status, equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithBitstream, threshold);
        assertNull("testWithBitstream 1", embargoDate);
        Pair<String, LocalDate> accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getLeft();
        assertThat("testWithBitstream 2", bitstreamStatus, equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate availabilityDate = accessStatusBitstream.getRight();
        assertNull("testWithBitstream 3", availabilityDate);
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithEmbargo 0", status, equalTo(DefaultAccessStatusHelper.EMBARGO));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithEmbargo, threshold);
        assertThat("testWithEmbargo 1", embargoDate, equalTo(startDate.toString()));
        Pair<String, LocalDate> accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getLeft();
        assertThat("testWithEmbargo 2", bitstreamStatus, equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate availabilityDate = accessStatusBitstream.getRight();
        assertThat("testWithEmbargo 3", availabilityDate, equalTo(startDate));
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithDateRestriction, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithDateRestriction 0", status, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithDateRestriction, threshold);
        assertNull("testWithDateRestriction 1", embargoDate);
        Pair<String, LocalDate> accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getLeft();
        assertThat("testWithDateRestriction 2", bitstreamStatus, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate availabilityDate = accessStatusBitstream.getRight();
        assertThat("testWithDateRestriction 3", availabilityDate, equalTo(startDate));
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithGroupRestriction, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithGroupRestriction 0", status, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithGroupRestriction, threshold);
        assertNull("testWithGroupRestriction 1", embargoDate);
        Pair<String, LocalDate> accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getLeft();
        assertThat("testWithGroupRestriction 2", bitstreamStatus, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate availabilityDate = accessStatusBitstream.getRight();
        assertThat("testWithGroupRestriction 3", availabilityDate, equalTo(threshold));
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutPolicy, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithoutPolicy 0", status, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithoutPolicy, threshold);
        assertNull("testWithoutPolicy 1", embargoDate);
        Pair<String, LocalDate> accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getLeft();
        assertThat("testWithoutPolicy 2", bitstreamStatus, equalTo(DefaultAccessStatusHelper.RESTRICTED));
        LocalDate availabilityDate = accessStatusBitstream.getRight();
        assertThat("testWithoutPolicy 3", availabilityDate, equalTo(threshold));
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutPrimaryBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithoutPrimaryBitstream 0", status, equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithoutPrimaryBitstream, threshold);
        assertNull("testWithoutPrimaryBitstream 1", embargoDate);
        Pair<String, LocalDate> accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getLeft();
        assertThat("testWithoutPrimaryBitstream 2", bitstreamStatus, equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate availabilityDate = accessStatusBitstream.getRight();
        assertNull("testWithoutPrimaryBitstream 3", availabilityDate);
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithPrimaryAndMultipleBitstreams, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithPrimaryAndMultipleBitstreams 0", status, equalTo(DefaultAccessStatusHelper.EMBARGO));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithPrimaryAndMultipleBitstreams, threshold);
        assertThat("testWithPrimaryAndMultipleBitstreams 1", embargoDate, equalTo(startDate.toString()));
        Pair<String, LocalDate> accessStatusPrimaryBitstream = helper.getAccessStatusFromBitstream(context,
                primaryBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String primaryBitstreamStatus = accessStatusPrimaryBitstream.getLeft();
        assertThat("testWithPrimaryAndMultipleBitstreams 2", primaryBitstreamStatus,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate primaryAvailabilityDate = accessStatusPrimaryBitstream.getRight();
        assertThat("testWithPrimaryAndMultipleBitstreams 3", primaryAvailabilityDate, equalTo(startDate));
        Pair<String, LocalDate> accessStatusOtherBitstream = helper.getAccessStatusFromBitstream(context,
                otherBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String otherBitstreamStatus = accessStatusOtherBitstream.getLeft();
        assertThat("testWithPrimaryAndMultipleBitstreams 4", otherBitstreamStatus,
                equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate otherAvailabilityDate = accessStatusOtherBitstream.getRight();
        assertNull("testWithPrimaryAndMultipleBitstreams 5", otherAvailabilityDate);
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithoutPrimaryAndMultipleBitstreams, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 0", status,
                equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithEmbargo, threshold);
        assertNull("testWithNoPrimaryAndMultipleBitstreams 1", embargoDate);
        Pair<String, LocalDate> accessStatusFirstBitstream = helper.getAccessStatusFromBitstream(context,
                firstBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String firstBitstreamStatus = accessStatusFirstBitstream.getLeft();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 2", firstBitstreamStatus,
                equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        LocalDate firstAvailabilityDate = accessStatusFirstBitstream.getRight();
        assertNull("testWithNoPrimaryAndMultipleBitstreams 3", firstAvailabilityDate);
        Pair<String, LocalDate> accessStatusOtherBitstream = helper.getAccessStatusFromBitstream(context,
                anotherBitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String otherBitstreamStatus = accessStatusOtherBitstream.getLeft();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 4", otherBitstreamStatus,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate otherAvailabilityDate = accessStatusOtherBitstream.getRight();
        assertThat("testWithNoPrimaryAndMultipleBitstreams 5", otherAvailabilityDate, equalTo(startDate));
    }

    /**
     * Test for an item with an embargo
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithEmbargoForAnonymousOrAdminUser() throws Exception {
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
        Pair<String, LocalDate> accessStatus = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String status = accessStatus.getLeft();
        assertThat("testWithEmbargoForAnonymousOrAdminUser 0", status, equalTo(DefaultAccessStatusHelper.EMBARGO));
        String embargoDate = helper.getEmbargoFromItem(context, itemWithEmbargo, threshold);
        assertThat("testWithEmbargoForAnonymousOrAdminUser 1", embargoDate, equalTo(startDate.toString()));
        Pair<String, LocalDate> accessStatusBitstream = helper.getAccessStatusFromBitstream(context,
                bitstream, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String bitstreamStatus = accessStatusBitstream.getLeft();
        assertThat("testWithEmbargoForAnonymousOrAdminUser 2", bitstreamStatus,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        LocalDate availabilityDate = accessStatusBitstream.getRight();
        assertThat("testWithEmbargoForAnonymousOrAdminUser 3", availabilityDate, equalTo(startDate));
        EPerson currentUser = context.getCurrentUser();
        context.setCurrentUser(admin);
        Pair<String, LocalDate> accessStatusAdmin = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_CURRENT_USER);
        String statusAdmin = accessStatusAdmin.getLeft();
        assertThat("testWithEmbargoForAnonymousOrAdminUser 4", statusAdmin,
                equalTo(DefaultAccessStatusHelper.OPEN_ACCESS));
        Pair<String, LocalDate> accessStatusAnonymous = helper.getAccessStatusFromItem(context,
                itemWithEmbargo, threshold, DefaultAccessStatusHelper.STATUS_FOR_ANONYMOUS);
        String statusAnonymous = accessStatusAnonymous.getLeft();
        assertThat("testWithEmbargoForAnonymousOrAdminUser 5", statusAnonymous,
                equalTo(DefaultAccessStatusHelper.EMBARGO));
        context.setCurrentUser(currentUser);
    }
}
