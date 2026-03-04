/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import static org.junit.Assert.assertEquals;
import java.sql.SQLException;
import java.util.List;
import org.dspace.AbstractUnitTest;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RequestPrimaryBitstreamMetadataStrategyTest extends AbstractUnitTest {

    private static final String AUTHOR_EMAIL = "john.doe@example.com";
    private static final String AUTHOR_FIRSTNAME = "John";
    private static final String AUTHOR_LASTNAME = "Doe";
    private static final String AUTHOR_NAME = AUTHOR_LASTNAME + ", " + AUTHOR_FIRSTNAME;
    private static final String BITSTREAM_REQUEST_EMAIL_FIELD = "person.email";
    private static final String BITSTREAM_REQUEST_NAME_FIELD = "dc.contributor.author";

    private static EPerson johnDoe;

    private Item item;

    private Bundle bundle;

    @BeforeClass
    public static void setUpClass() throws SQLException {
        AbstractBuilder.init(); // AbstractUnitTest doesn't do this for us.

        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();
        johnDoe = EPersonBuilder.createEPerson(ctx).withEmail(AUTHOR_EMAIL)
                .withNameInMetadata(AUTHOR_FIRSTNAME, AUTHOR_LASTNAME).build();
        ctx.restoreAuthSystemState();
        ctx.complete();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // AbstractUnitTest doesn't do this for us.
        AbstractBuilder.cleanupObjects();
        AbstractBuilder.destroy();
    }

    @Before
    public void setUp() throws Exception {
        context = new Context();
        context.setCurrentUser(johnDoe);
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        item = ItemBuilder.createItem(context, collection).build();
        bundle = BundleBuilder.createBundle(context, item).withName(Constants.DEFAULT_BUNDLE_NAME).build();
        BitstreamBuilder.createBitstream(context, bundle, null)
                .withMetadata("person", "email", null, null, AUTHOR_EMAIL)
                .withMetadata("dc", "contributor", "author", null, AUTHOR_NAME).build();
        context.restoreAuthSystemState();
        context.setCurrentUser(null);
    }

    /**
     * Test of getRequestItemAuthor method, of class
     * RequestPrimaryBitstreamMetadataStrategy.
     * 
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetRequestBistreamAuthor() throws Exception {
        RequestPrimaryBitstreamMetadataStrategy instance = new RequestPrimaryBitstreamMetadataStrategy();
        instance.setEmailMetadata(BITSTREAM_REQUEST_EMAIL_FIELD);
        instance.setFullNameMetadata(BITSTREAM_REQUEST_NAME_FIELD);

        List<RequestItemAuthor> author = instance.getRequestItemAuthor(context, item);
        assertEquals("Wrong bitstream author address", AUTHOR_EMAIL, author.get(0).getEmail());
    }

}
