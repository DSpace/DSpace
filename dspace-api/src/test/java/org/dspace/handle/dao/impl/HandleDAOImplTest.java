/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.dao.impl;

import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.dao.HandleDAO;
import org.dspace.utils.DSpace;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test class for the Handle DAO
 */
public class HandleDAOImplTest extends AbstractUnitTest {

    /** log4j category */
    private static final Logger log = Logger.getLogger(HandleDAOImplTest.class);

    /**
     * Item instances for the tests
     */
    private Item item1;
    private Item item2;
    private Item item3;
    private Item item4;

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();

    private HandleDAO handleDAO = new DSpace().getServiceManager().getServicesByType(HandleDAO.class).get(0);

    private Community owningCommunity;

    private static final String HANDLE_PREFIX = "123456789";
    private static final String SUFFIX_1 = "11";
    private static final String SUFFIX_2 = "11.2";
    private static final String SUFFIX_3 = "33";
    private static final String SUFFIX_4 = "44";

    @Before
    @Override
    public void init()
    {
        super.init();
        try
        {
            //we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            Collection collection = collectionService.create(context, owningCommunity);

            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            item1 = installItemService.installItem(context, workspaceItem, HANDLE_PREFIX + "/" + SUFFIX_1);
            item1.setSubmitter(context.getCurrentUser());
            itemService.update(context, item1);

            item2 = versioningService.createNewVersion(context, item1).getItem();

            workspaceItem = workspaceItemService.create(context, collection, false);
            item3 = installItemService.installItem(context, workspaceItem, HANDLE_PREFIX + "/" + SUFFIX_3);
            item3.setSubmitter(context.getCurrentUser());
            itemService.update(context, item3);

            workspaceItem = workspaceItemService.create(context, collection, false);
            item4 = installItemService.installItem(context, workspaceItem, "hdl:custom-prefix" + "/" + SUFFIX_4);
            item4.setSubmitter(context.getCurrentUser());
            itemService.update(context, item4);

            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (IOException ex) {
            log.error("Failed to assign handle", ex);
            fail("Failed to assign handle: " + ex.getMessage());
        }
    }

    @After
    @Override
    public void destroy()
    {
        try {
            context.turnOffAuthorisationSystem();

            //Context might have been committed in the test method, so best to reload to entity so we're sure that it is attached.
            owningCommunity = context.reloadEntity(owningCommunity);
            ContentServiceFactory.getInstance().getCommunityService().delete(context, owningCommunity);
            owningCommunity = null;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (AuthorizeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        item1 = null;
        item2 = null;
        item3 = null;
        item4 = null;
        super.destroy();
    }

    @Test
    public void updateHandlesWithNewPrefix() throws Exception {
        context.turnOffAuthorisationSystem();

        String newPrefix = "987654321";
        handleDAO.updateHandlesWithNewPrefix(context, newPrefix, HANDLE_PREFIX);
        context.commit();

        assertEquals(newPrefix + "/" + SUFFIX_1, itemService.find(context, item1.getID()).getHandle());
        assertEquals(newPrefix + "/" + SUFFIX_2, itemService.find(context, item2.getID()).getHandle());
        assertEquals(newPrefix + "/" + SUFFIX_3, itemService.find(context, item3.getID()).getHandle());

        //Ensure that records not matching the old prefix are not touched
        assertEquals("hdl:custom-prefix/" + SUFFIX_4, itemService.find(context, item4.getID()).getHandle());

        context.restoreAuthSystemState();
    }

}