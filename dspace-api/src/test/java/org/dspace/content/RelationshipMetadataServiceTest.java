/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RelationshipMetadataServiceTest extends AbstractUnitTest {

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(RelationshipMetadataServiceTest.class);

    protected RelationshipMetadataService relationshipMetadataService = ContentServiceFactory
                                                                        .getInstance().getRelationshipMetadataService();
    protected RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    protected RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                                                                                     .getRelationshipTypeService();
    protected EntityService entityService = ContentServiceFactory.getInstance().getEntityService();
    protected EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    Item item;
    Item authorItem;

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
            Community community = communityService.create(null, context);

            Collection col = collectionService.create(context, community);
            WorkspaceItem is = workspaceItemService.create(context, col, false);
            WorkspaceItem authorIs = workspaceItemService.create(context, col, false);

            item = installItemService.installItem(context, is);
            authorItem = installItemService.installItem(context, authorIs);
            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }

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
        context.abort();
        super.destroy();
    }


    @Test
    public void testGetRelationshipMetadata() throws Exception {
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, item, "relationship", "type", null, null, "Publication");
        itemService.addMetadata(context, authorItem, "relationship", "type", null, null, "Author");
        itemService.addMetadata(context, authorItem, "person", "familyName", null, null, "familyName");
        itemService.addMetadata(context, authorItem, "person", "givenName", null, null, "firstName");
        EntityType publicationEntityType = entityTypeService.create(context, "Publication");
        EntityType authorEntityType = entityTypeService.create(context, "Author");
        RelationshipType isAuthorOfPublication = relationshipTypeService
            .create(context, publicationEntityType, authorEntityType, "isAuthorOfPublication", "isPublicationOfAuthor",
                    null, null, null, null);

        Relationship relationship = relationshipService.create(context, item, authorItem, isAuthorOfPublication, 0, 0);

        context.restoreAuthSystemState();

        List<MetadataValue> authorList = itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));

        List<MetadataValue> relationshipMetadataList = itemService
            .getMetadata(item, "relation", "isAuthorOfPublication", null, Item.ANY);
        assertThat(relationshipMetadataList.size(), equalTo(1));
        assertThat(relationshipMetadataList.get(0).getValue(), equalTo(String.valueOf(authorItem.getID())));

        List<RelationshipMetadataValue> list = relationshipMetadataService.getRelationshipMetadata(item, true);
        assertThat(list.size(), equalTo(2));
        assertThat(list.get(0).getValue(), equalTo("familyName, firstName"));
        assertThat(list.get(0).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(list.get(0).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(list.get(0).getMetadataField().getQualifier(), equalTo("author"));
        assertThat(list.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(list.get(1).getValue(), equalTo(String.valueOf(authorItem.getID())));
        assertThat(list.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("relation"));
        assertThat(list.get(1).getMetadataField().getElement(), equalTo("isAuthorOfPublication"));
        assertThat(list.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));

    }
}
