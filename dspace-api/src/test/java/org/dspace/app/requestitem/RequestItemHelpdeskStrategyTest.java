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
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author mwood
 */
public class RequestItemHelpdeskStrategyTest
        extends AbstractUnitTest {
    private static final String HELPDESK_ADDRESS = "helpdesk@example.com";
    private static final String AUTHOR_ADDRESS = "john.doe@example.com";

    private static ConfigurationService configurationService;
    private static EPersonService epersonService;
    private static EPerson johnDoe;

    private Item item;

    @BeforeClass
    public static void setUpClass()
            throws SQLException {
        AbstractBuilder.init(); // AbstractUnitTest doesn't do this for us.

        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        epersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();
        johnDoe = EPersonBuilder.createEPerson(ctx)
                .withEmail(AUTHOR_ADDRESS)
                .withNameInMetadata("John", "Doe")
                .build();
        ctx.restoreAuthSystemState();
        ctx.complete();
    }

    @AfterClass
    public static void tearDownClass() {
        AbstractBuilder.destroy(); // AbstractUnitTest doesn't do this for us.
    }

    @Before
    public void setUp() {
        context = new Context();
        context.setCurrentUser(johnDoe);
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        item = ItemBuilder.createItem(context, collection)
                .build();
        context.restoreAuthSystemState();
        context.setCurrentUser(null);
    }

    /**
     * Test of getRequestItemAuthor method, of class RequestItemHelpdeskStrategy.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetRequestItemAuthor()
            throws Exception {
        RequestItemHelpdeskStrategy instance = new RequestItemHelpdeskStrategy();
        instance.configurationService = configurationService;
        instance.ePersonService = epersonService;

        // Check with help desk enabled.
        configurationService.setProperty(RequestItemHelpdeskStrategy.P_HELPDESK_OVERRIDE, "true");
        configurationService.setProperty(RequestItemHelpdeskStrategy.P_MAIL_HELPDESK, HELPDESK_ADDRESS);
        List<RequestItemAuthor> authors = instance.getRequestItemAuthor(context, item);
        assertEquals("Wrong author address", HELPDESK_ADDRESS, authors.get(0).getEmail());

        // Check with help desk disabled.
        configurationService.setProperty(RequestItemHelpdeskStrategy.P_HELPDESK_OVERRIDE, "false");
        authors = instance.getRequestItemAuthor(context, item);
        assertEquals("Wrong author address", AUTHOR_ADDRESS, authors.get(0).getEmail());
    }

    /**
     * Test of getHelpDeskPerson method, of class RequestItemHelpdeskStrategy.
     * @throws java.lang.Exception passed through.
     */
    @Ignore
    @Test
    public void testGetHelpDeskPerson() throws Exception {
    }
}
