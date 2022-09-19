/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.sql.SQLException;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.HandleBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * The test suite for testing the HandleClarinService.
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class HandleClarinServiceImplIT extends AbstractIntegrationTestWithDatabase {

    private final static String EXTERNAL_HANDLE_HANDLE = "123456789/LRT-ex123";
    private final static String EXTERNAL_HANDLE_DELIMITER_HANDLE = "123456789/LRT@-ex123";
    private final static String EXTERNAL_HANDLE_URL = "amazing URL";

    HandleClarinService handleClarinService = ContentServiceFactory.getInstance().getHandleClarinService();
    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private Handle communityHandle;
    private Handle collectionHandle;
    private Handle itemHandle;
    private Handle externalHandle;
    private Handle externalDelimiterHandle;

    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collection
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        communityHandle = parentCommunity.getHandles().get(0);

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        collectionHandle = col1.getHandles().get(0);

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();
        itemHandle = publicItem1.getHandles().get(0);

        externalHandle = HandleBuilder.createExternalHandle(context, EXTERNAL_HANDLE_HANDLE, EXTERNAL_HANDLE_URL)
                .build();

        externalDelimiterHandle = HandleBuilder.createExternalHandle(context, EXTERNAL_HANDLE_DELIMITER_HANDLE,
                        EXTERNAL_HANDLE_URL)
                .build();

        context.commit();
        context.restoreAuthSystemState();
    }

    @Test
    public void createdHandleShouldNotBeNull() throws Exception {
        Assert.assertNotNull(communityHandle);
        Assert.assertNotNull(collectionHandle);
        Assert.assertNotNull(itemHandle);
    }

    @Test
    public void testResolvingUrlOfInternalHandle() throws Exception {
        String expectedUrl = configurationService.getProperty("dspace.ui.url") + "/handle/" + itemHandle.getHandle();
        String receivedUrl = handleClarinService.resolveToURL(context, itemHandle.getHandle());
        Assert.assertEquals(expectedUrl, receivedUrl);
    }

    @Test
    public void testResolvingUrlOfExternalHandle() throws Exception {
        String expectedUrl = externalHandle.getUrl();
        String receivedUrl = handleClarinService.resolveToURL(context, externalHandle.getHandle());
        Assert.assertEquals(expectedUrl, receivedUrl);
    }

    @Test
    public void testResolvingUrlOfExternalDelimiterHandle() throws Exception {
        // should return null
        String receivedUrl = handleClarinService.resolveToURL(context, externalDelimiterHandle.getHandle());
        Assert.assertNull(receivedUrl);
    }

    @Test
    public void testResolvingHandleToItem() throws Exception {
        DSpaceObject item = handleClarinService.resolveToObject(context, itemHandle.getHandle());
        Assert.assertTrue(item instanceof Item);
    }

    @Test
    public void testResolvingHandleToCollection() throws Exception {
        DSpaceObject item = handleClarinService.resolveToObject(context, collectionHandle.getHandle());
        Assert.assertTrue(item instanceof Collection);
    }

    @Test
    public void testResolvingHandleToCommunity() throws Exception {
        DSpaceObject item = handleClarinService.resolveToObject(context, communityHandle.getHandle());
        Assert.assertTrue(item instanceof Community);
    }

    @Test
    public void testNotResolveExternalHandle() throws Exception {
        DSpaceObject item = handleClarinService.resolveToObject(context, externalHandle.getHandle());
        Assert.assertNull(item);
    }

    @Ignore("Unless the Handle table will be updated in the testing env.")
    @Test
    public void testIsDead() throws Exception {
        boolean isDead = handleClarinService.isDead(context, itemHandle.getHandle());
        Assert.assertTrue(isDead);
    }
}
