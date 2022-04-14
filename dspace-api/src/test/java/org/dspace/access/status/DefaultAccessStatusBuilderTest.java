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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultAccessStatusBuilderTest  extends AbstractUnitTest {

    private static final Logger log = LogManager.getLogger(DefaultAccessStatusBuilderTest.class);

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
    private DefaultAccessStatusBuilder builder;
    private Date threshold;

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
            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
        builder = new DefaultAccessStatusBuilder();
        threshold = new LocalDate(10000, 1, 1).toDate();
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
        collection = null;
        owningCommunity = null;
        builder = null;
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
        String status = builder.getAccessStatusFromItem(context, null, threshold);
        assertThat("testWithNullItem 0", status, equalTo(DefaultAccessStatusBuilder.UNKNOWN));
    }

    /**
     * Test for an item with no bundle
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testWithoutBundle() throws Exception {
        String status = builder.getAccessStatusFromItem(context, itemWithoutBundle, threshold);
        assertThat("testWithoutBundle 0", status, equalTo(DefaultAccessStatusBuilder.METADATA_ONLY));
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
        String status = builder.getAccessStatusFromItem(context, itemWithoutBitstream, threshold);
        assertThat("testWithoutBitstream 0", status, equalTo(DefaultAccessStatusBuilder.METADATA_ONLY));
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
        String status = builder.getAccessStatusFromItem(context, itemWithBitstream, threshold);
        assertThat("testWithBitstream 0", status, equalTo(DefaultAccessStatusBuilder.OPEN_ACCESS));
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
        ResourcePolicy policy = resourcePolicyService.create(context);
        policy.setRpName("Embargo");
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        policy.setGroup(group);
        policy.setAction(Constants.READ);
        policy.setStartDate(new LocalDate(9999, 12, 31).toDate());
        policies.add(policy);
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.addPolicies(context, policies, bitstream);
        context.restoreAuthSystemState();
        String status = builder.getAccessStatusFromItem(context, itemWithEmbargo, threshold);
        assertThat("testWithEmbargo 0", status, equalTo(DefaultAccessStatusBuilder.EMBARGO));
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
        ResourcePolicy policy = resourcePolicyService.create(context);
        policy.setRpName("Restriction");
        Group group = groupService.findByName(context, Group.ANONYMOUS);
        policy.setGroup(group);
        policy.setAction(Constants.READ);
        policy.setStartDate(new LocalDate(10000, 1, 1).toDate());
        policies.add(policy);
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.addPolicies(context, policies, bitstream);
        context.restoreAuthSystemState();
        String status = builder.getAccessStatusFromItem(context, itemWithDateRestriction, threshold);
        assertThat("testWithDateRestriction 0", status, equalTo(DefaultAccessStatusBuilder.RESTRICTED));
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
        ResourcePolicy policy = resourcePolicyService.create(context);
        policy.setRpName("Restriction");
        Group group = groupService.findByName(context, Group.ADMIN);
        policy.setGroup(group);
        policy.setAction(Constants.READ);
        policies.add(policy);
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.addPolicies(context, policies, bitstream);
        context.restoreAuthSystemState();
        String status = builder.getAccessStatusFromItem(context, itemWithGroupRestriction, threshold);
        assertThat("testWithGroupRestriction 0", status, equalTo(DefaultAccessStatusBuilder.RESTRICTED));
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
        String status = builder.getAccessStatusFromItem(context, itemWithoutPolicy, threshold);
        assertThat("testWithoutPolicy 0", status, equalTo(DefaultAccessStatusBuilder.RESTRICTED));
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
        String status = builder.getAccessStatusFromItem(context, itemWithoutPrimaryBitstream, threshold);
        assertThat("testWithoutPrimaryBitstream 0", status, equalTo(DefaultAccessStatusBuilder.OPEN_ACCESS));
    }
}
