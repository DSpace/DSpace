/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.processor;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.CharEncoding;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Test for the ExportEventProcessor class
 */
public class ExportEventProcessorIT extends AbstractIntegrationTestWithDatabase {

    @Mock
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private EntityType publication;
    private EntityType otherEntity;
    private final String excluded_type = "Excluded type";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        configurationService.setProperty("irus.statistics.tracker.urlversion", "Z39.88-2004");
        configurationService.setProperty("irus.statistics.tracker.enabled", true);
        configurationService.setProperty("irus.statistics.tracker.type-field", "dc.type");
        configurationService.setProperty("irus.statistics.tracker.type-value", "Excluded type");

        context.turnOffAuthorisationSystem();
        publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        otherEntity = EntityTypeBuilder.createEntityTypeBuilder(context, "Other").build();
        context.restoreAuthSystemState();


    }

    @Test
    /**
     * Test the getBaseParameters method
     */
    public void testGetBaseParameters() throws UnsupportedEncodingException {

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String encodedHandle = URLEncoder.encode(item.getHandle(), CharEncoding.UTF_8);
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        when(request.getRemoteAddr()).thenReturn("test-client-ip");
        when(request.getHeader("USER-AGENT")).thenReturn("test-user-agent");
        when(request.getHeader("referer")).thenReturn("test-referer");

        String result = exportEventProcessor.getBaseParameters(item);
        String expected = "url_ver=Z39.88-2004&req_id=test-client-ip&req_dat=test-user-agent&rft.artnum=" +
                "oai%3Alocalhost%3A" + encodedHandle + "&rfr_dat=test-referer&rfr_id=localhost&url_tim=";

        assertThat(result, startsWith(expected));


    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item is null
     */
    public void testShouldProcessItemWhenNull() throws SQLException {
        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, null);

        boolean result = exportEventProcessor.shouldProcessItem(null);
        assertThat(result, is(false));
    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item is not archived
     */
    public void testShouldProcessItemWhenNotArchived() throws SQLException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection).build();
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, workspaceItem.getItem());

        boolean result = exportEventProcessor.shouldProcessItem(workspaceItem.getItem());
        assertFalse(result);
    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item can be edit by the current user
     */
    public void testShouldProcessItemWhenCanEdit() throws SQLException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withEntityType(otherEntity.getLabel())
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        context.setCurrentUser(admin);
        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertFalse(result);

    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item type should be excluded
     */
    public void testShouldProcessItemWhenShouldNotProcessType() throws Exception {

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withEntityType(publication.getLabel())
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withType("Excluded type")
                               .build();

        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertFalse(result);

    }

    @Test
    /**
     * Test the ShouldProcessItem method where the item entity type should not be processed
     */
    public void testShouldProcessItemWhenShouldNotProcessEntity() throws SQLException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withEntityType(otherEntity.getLabel())
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertFalse(result);

    }

    @Test
    /**
     * Test the ShouldProcessItem method where all conditions are met
     */
    public void testShouldProcessItem() throws SQLException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withEntityType(publication.getLabel())
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessItem(item);
        assertTrue(result);

    }


    @Test
    /**
     * Test the ShouldProcessEntityType method where all conditions are met
     */
    public void testShouldProcessEntityType() throws SQLException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withEntityType(publication.getLabel())
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessEntityType(item);

        assertTrue(result);
    }

    @Test
    /**
     * Test the ShouldProcessEntityType method where the item entity type is not present in the configured list
     */
    public void testShouldProcessEntityTypeWhenNotInList() throws SQLException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withEntityType(otherEntity.getLabel())
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessEntityType(item);

        assertFalse(result);

    }

    @Test
    /**
     * Test the ShouldProcessEntityType method where no entityType is present
     */
    public void testShouldProcessEntityTypeWhenNotPresent() throws SQLException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessEntityType(item);

        assertTrue(result);
    }


    @Test
    /**
     * Test the shouldProcessItemType method where the item type is present in the list of excluded types
     */
    public void testShouldProcessItemTypeInExcludeTrackerTypeList() {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).withType(excluded_type).build();
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessItemType(item);
        assertFalse(result);

    }

    @Test
    /**
     * Test the shouldProcessItemType method where the item type is not present in the list of excluded types
     */
    public void testShouldProcessItemTypeNotInExcludeTrackerTypeList() {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).withType("Not excluded type").build();
        context.restoreAuthSystemState();

        ExportEventProcessor exportEventProcessor = new ItemEventProcessor(context, request, item);

        boolean result = exportEventProcessor.shouldProcessItemType(item);
        assertTrue(result);

    }

}
