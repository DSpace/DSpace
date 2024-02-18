/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RelationshipTypeDAOImplIT extends AbstractIntegrationTest {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RelationshipTypeDAOImplIT.class);

    private Relationship relationship;

    private Item itemOne;

    private Item itemTwo;

    private Collection collection;

    private Community owningCommunity;

    private RelationshipType relationshipType;

    private List<RelationshipType> relationshipTypeList = new ArrayList<>();

    private EntityType entityTypeOne;

    private EntityType entityTypeTwo;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected RelationshipTypeService relationshipTypeService =
            ContentServiceFactory.getInstance().getRelationshipTypeService();
    protected RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    protected EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();

    /**
     * Initalize DSpace objects used for testing for each test
     */
    @Before
    @Override
    public void init() {
        super.init();
        try {
            context.turnOffAuthorisationSystem();
            owningCommunity = communityService.create(null, context);
            collection = collectionService.create(context, owningCommunity);
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            WorkspaceItem workspaceItemTwo = workspaceItemService.create(context, collection, false);
            itemOne = installItemService.installItem(context, workspaceItem);
            itemTwo = installItemService.installItem(context, workspaceItemTwo);
            itemService.addMetadata(context, itemOne, "dspace", "entity", "type", Item.ANY, "Publication");
            itemService.addMetadata(context, itemTwo, "dspace", "entity", "type", Item.ANY, "Person");
            itemService.update(context, itemOne);
            itemService.update(context, itemTwo);
            entityTypeOne = entityTypeService.create(context, "Person");
            entityTypeTwo = entityTypeService.create(context, "Publication");
            relationshipType = relationshipTypeService.create(context, entityTypeTwo, entityTypeOne,
                    "isAuthorOfPublication", "isPublicationOfAuthor",0,10,0,10);
            relationship = relationshipService.create(context, itemOne, itemTwo, relationshipType, 0, 0);
            relationshipService.update(context, relationship);
            relationshipTypeList.add(relationshipType);
            context.restoreAuthSystemState();
        } catch (Exception e) {
            log.error(e);
            fail(e.getMessage());
        }
    }

    /**
     * Delete all initalized DSpace objects after each test
     */
    @After
    @Override
    public void destroy() {
        try {
            // Cleanup newly created objects
            context.turnOffAuthorisationSystem();
            relationshipService.delete(context, relationship);
            relationshipTypeService.delete(context, relationshipType);
            entityTypeService.delete(context, entityTypeTwo);
            entityTypeService.delete(context, entityTypeOne);
            itemService.delete(context, itemOne);
            itemService.delete(context, itemTwo);
        } catch (Exception e) {
            log.error(e);
            fail(e.getMessage());
        }
        super.destroy();

    }

    /**
     * Test findbyTypesAndLabels should return our defined RelationshipType given our test Entities entityTypeTwo and
     * entityTypeOne with the affiliated labels isAuthorOfPublication and isPublicationOfAuthor
     *
     * @throws Exception
     */
    @Test
    public void testFindByTypesAndLabels() throws Exception {
        assertEquals("TestFindbyTypesAndLabels 0", relationshipType, relationshipTypeService
                .findbyTypesAndTypeName(context, entityTypeTwo, entityTypeOne, "isAuthorOfPublication",
                        "isPublicationOfAuthor"));
    }

    /**
     * Test findByLeftOrRightLabel should return our defined relationshipTypeList given one of our affiliated labels
     *
     * @throws Exception
     */
    @Test
    public void testFindByLeftOrRightLabel() throws Exception {
        assertEquals("TestFindByLeftOrRightLabel 0", relationshipTypeList, relationshipTypeService.
                findByLeftwardOrRightwardTypeName(context, "isAuthorOfPublication", -1, -1));
    }

    /**
     * Test findByEntityType should return our defined relationshipsList given one our defined EntityTypes
     * entityTypeOne
     *
     * @throws Exception
     */
    @Test
    public void testFindByEntityType() throws Exception {
        assertEquals("TestFindByEntityType 0", relationshipTypeList, relationshipTypeService.findByEntityType(context,
                entityTypeOne));
    }
}
