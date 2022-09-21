/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.Handle;
import org.dspace.handle.Handle_;
import org.dspace.handle.dao.HandleClarinDAO;
import org.dspace.handle.factory.HandleClarinServiceFactory;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.utils.DSpace;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the Handle Clarin DAO
 */
public class HandleClarinDAOImplTest extends AbstractUnitTest {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(HandleDAOImplTest.class);

    /**
     * Item instances for the tests
     */
    private Item item1;
    private Item item3;
    private Item item4;

    /**
     * Created external handle from the item4
     */
    private Handle externalHandle;

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();
    protected HandleClarinService handleClarinService =
            HandleClarinServiceFactory.getInstance().getHandleClarinService();

    private HandleClarinDAO handleClarinDAO =
            new DSpace().getServiceManager().getServicesByType(HandleClarinDAO.class).get(0);

    private Community owningCommunity;

    private static final String HANDLE_PREFIX = "123456789";
    private static final String CUSTOM_PREFIX = "hdl:custom-prefix";
    private static final String SUFFIX_1 = "101";
    private static final String SUFFIX_3 = "303";
    private static final String SUFFIX_4 = "404";
    private static final String SUFFIX_EXTERNAL = "123456";
    private static final String EXTERNAL_URL = "external URL";

    private static final String HANDLE_SORTING_COLUMN_DEF = Handle_.HANDLE + ":" + HANDLE_PREFIX + "/" + SUFFIX_3;
    private static final String INTERNAL_HANDLE_SORTING_COLUMN_DEF = Handle_.URL + ":internal";
    private static final String EXTERNAL_HANDLE_SORTING_COLUMN_DEF = Handle_.URL + ":external";
    private static final String RESOURCE_TYPE_HANDLE_ITEM_SORTING_COLUMN_DEF = "resourceTypeId:" + Constants.ITEM;
    private static final String RESOURCE_TYPE_HANDLE_COLLECTION_SORTING_COLUMN_DEF =
            "resourceTypeId:" + Constants.COLLECTION;


    @Before
    @Override
    public void init() {
        super.init();
        try {
            // we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            Collection collection = collectionService.create(context, owningCommunity);

            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            item1 = installItemService.installItem(context, workspaceItem, HANDLE_PREFIX + "/" + SUFFIX_1);
            item1.setSubmitter(context.getCurrentUser());
            itemService.update(context, item1);

            workspaceItem = workspaceItemService.create(context, collection, false);
            item3 = installItemService.installItem(context, workspaceItem, HANDLE_PREFIX + "/" + SUFFIX_3);
            item3.setSubmitter(context.getCurrentUser());
            itemService.update(context, item3);

            workspaceItem = workspaceItemService.create(context, collection, false);
            item4 = installItemService.installItem(context, workspaceItem,
                    CUSTOM_PREFIX + "/" + SUFFIX_4);
            item4.setSubmitter(context.getCurrentUser());
            itemService.update(context, item4);

            // create external handle
            externalHandle = handleClarinService.createExternalHandle(context,
                    HANDLE_PREFIX + "/" + SUFFIX_EXTERNAL, EXTERNAL_URL);
            // save created handle
            handleClarinService.save(context, externalHandle);

            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (IOException ex) {
            log.error("Failed to assign handle", ex);
            fail("Failed to assign handle: " + ex.getMessage());
        }
    }

    @After
    @Override
    public void destroy() {
        try {
            context.turnOffAuthorisationSystem();
            // Context might have been committed in the test method, so best to reload to entity so we're sure that it
            // is attached.
            externalHandle = context.reloadEntity(externalHandle);
            handleClarinService.delete(context, externalHandle);

            owningCommunity = context.reloadEntity(owningCommunity);
            ContentServiceFactory.getInstance().getCommunityService().delete(context, owningCommunity);
            owningCommunity = null;
        } catch (Exception e) {
            throw new AssertionError("Error occurred in destroy()", e);
        }
        item1 = null;
        item3 = null;
        item4 = null;
        externalHandle = null;
        super.destroy();
    }

    @Test
    public void findAllHandles() throws Exception {
        context.turnOffAuthorisationSystem();

        List<Handle> receivedHandles = handleClarinDAO.findAll(context, null, 10, 0);

        assertEquals(receivedHandles.size(), 7);
        assertEquals(receivedHandles.get(3).getHandle(), HANDLE_PREFIX + "/" + SUFFIX_1);
        assertEquals(receivedHandles.get(4).getHandle(), HANDLE_PREFIX + "/" + SUFFIX_3);
        assertEquals(receivedHandles.get(5).getHandle(), CUSTOM_PREFIX + "/" + SUFFIX_4);
        assertEquals(receivedHandles.get(6).getHandle(), HANDLE_PREFIX + "/" + SUFFIX_EXTERNAL);
        context.restoreAuthSystemState();
    }

    @Test
    public void findHandlesByHandle() throws Exception {
        context.turnOffAuthorisationSystem();

        List<Handle> receivedHandles = handleClarinDAO.findAll(context, HANDLE_SORTING_COLUMN_DEF, 10, 0);

        assertEquals(receivedHandles.size(), 1);
        assertEquals(receivedHandles.get(0).getHandle(), HANDLE_PREFIX + "/" + SUFFIX_3);
        context.restoreAuthSystemState();
    }

    @Test
    public void findExternalHandles() throws Exception {
        context.turnOffAuthorisationSystem();
        assertNotNull(this.externalHandle);

        List<Handle> receivedHandles = handleClarinDAO.findAll(context, EXTERNAL_HANDLE_SORTING_COLUMN_DEF, 10, 0);

        assertEquals(receivedHandles.size(), 1);
        assertEquals(receivedHandles.get(0).getHandle(), HANDLE_PREFIX + "/" + SUFFIX_EXTERNAL);
        assertEquals(receivedHandles.get(0).getUrl(), this.externalHandle.getUrl());
        context.restoreAuthSystemState();
    }

    @Test
    public void findInternalHandles() throws Exception {
        context.turnOffAuthorisationSystem();
        assertNotNull(this.externalHandle);

        List<Handle> receivedHandles = handleClarinDAO.findAll(context, INTERNAL_HANDLE_SORTING_COLUMN_DEF, 10, 0);

        assertEquals(receivedHandles.size(), 6);
        context.restoreAuthSystemState();
    }

    @Test
    public void findItemsHandlesByResourceType() throws Exception {
        context.turnOffAuthorisationSystem();
        assertNotNull(this.externalHandle);

        List<Handle> receivedHandles =
                handleClarinDAO.findAll(context, RESOURCE_TYPE_HANDLE_ITEM_SORTING_COLUMN_DEF, 10, 0);

        assertEquals(receivedHandles.size(), 3);
        context.restoreAuthSystemState();
    }

    @Test
    public void findCollectionHandlesByResourceType() throws Exception {
        context.turnOffAuthorisationSystem();
        assertNotNull(this.externalHandle);

        List<Handle> receivedHandles =
                handleClarinDAO.findAll(context, RESOURCE_TYPE_HANDLE_COLLECTION_SORTING_COLUMN_DEF, 10, 0);

        assertEquals(receivedHandles.size(), 1);
        context.restoreAuthSystemState();
    }


}
