/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author mwood
 */
public class SubscriptionDsoMetadataForEmailComposeTest
        extends AbstractUnitTest {
    @Before
    public void setup() {
        AbstractBuilder.init();
    }

    /**
     * Test of canDisseminate method, of class SubscriptionDsoMetadataForEmailCompose.
     *
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    @Test
    public void testCanDisseminate()
            throws SQLException, AuthorizeException {
        System.out.println("canDisseminate");

        DSpaceObject dso;
        boolean expResult;
        boolean result;

        SubscriptionDsoMetadataForEmailCompose instance
                = new SubscriptionDsoMetadataForEmailCompose();

        dso = null;
        expResult = false;
        result = instance.canDisseminate(context, dso);
        assertEquals("Null is not disseminable", expResult, result);

        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, com).build();
        dso = ItemBuilder.createItem(context, col).build();
        context.restoreAuthSystemState();
        expResult = true;
        result = instance.canDisseminate(context, dso);
        assertEquals("Item should be disseminable", expResult, result);

        dso = com;
        expResult = false;
        result = instance.canDisseminate(context, dso);
        assertEquals("Community is not disseminable", expResult, result);
    }

    /**
     * Test of disseminate method, of class SubscriptionDsoMetadataForEmailCompose.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     */
    @Test
    public void testDisseminate()
            throws SQLException, AuthorizeException {
        System.out.println("disseminate");

        final String title = "Test Item";

        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, com).build();
        DSpaceObject dso = ItemBuilder.createItem(context, col)
                .withTitle(title)
                .build();
        context.restoreAuthSystemState();

        SubscriptionDsoMetadataForEmailCompose instance
                = new SubscriptionDsoMetadataForEmailCompose();
        instance.handleService = HandleServiceFactory.getInstance().getHandleService();
        instance.itemService = ContentServiceFactory.getInstance().getItemService();
        instance.setMetadata(List.of("dc.title"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        instance.disseminate(context, dso, out);
        String outString = out.toString(StandardCharsets.UTF_8);

        assertThat("Title not found in output", outString, containsString(title));

        String handle = instance.handleService.getCanonicalPrefix() + dso.getHandle();
        assertThat("Handle not found in output", outString,
                containsString(handle));
    }

    /**
     * Test of getMIMEType method, of class SubscriptionDsoMetadataForEmailCompose.
     */
    @Test
    public void testGetMIMEType() {
        System.out.println("getMIMEType");

        SubscriptionDsoMetadataForEmailCompose instance
                = new SubscriptionDsoMetadataForEmailCompose();
        String expResult = "text/plain";
        String result = instance.getMIMEType();
        assertEquals(expResult, result);
    }
}
