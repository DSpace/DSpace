/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.AbstractDSpaceTest;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ItemServiceImplTest extends AbstractUnitTest{
    /** log4j category */
    private static final Logger log = Logger.getLogger(ItemServiceImplTest.class);

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    Collection collection;
    Community owningCommunity;
    @Before
    public void init(){
        super.init();
        try{
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
        } catch(SQLException|AuthorizeException e){
            log.error(e,e);
        }
    }

    protected Item createItem() throws SQLException, IOException, AuthorizeException, IllegalAccessException {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = ePersonService.create(context);
        context.setCurrentUser(ePerson);
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        return installItemService.installItem(context, workspaceItem);
    }

    @Test
    public void testFindAllAuthorizedDoesntIncludePrivateItem() throws SQLException, IOException, AuthorizeException, IllegalAccessException, ParseException {
        Item item = createItem();
        item.setDiscoverable(false);
        itemService.update(context, item);
        context.restoreAuthSystemState();
        UUID id = item.getID();
        List<Group> anonGroups = groupService.search(context, Group.ANONYMOUS);
        for(Group group: anonGroups){
            ResourcePolicy resourcePolicy = authorizeService.createResourcePolicy(context, item, group, context.getCurrentUser(), Constants.READ, ResourcePolicy.TYPE_CUSTOM);
            String dateString = "2020-04-01";
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = formatter.parse(dateString);
            resourcePolicy.setStartDate(startDate);
        }
        context.restoreAuthSystemState();
        Iterator<Item> foundItems = itemService.findAllAuthorized(context, 20, 0);
        List<UUID> uuidList = new LinkedList<>();
        while(foundItems.hasNext()){
            Item nextItem = foundItems.next();
            uuidList.add(nextItem.getID());
        }
        assertFalse(uuidList.contains(id));
    }

    @Test
    public void testFindAllAuthorizedDoesIncludePublicItems() throws SQLException, IOException, AuthorizeException, IllegalAccessException, ParseException{
        Item item = createItem();
        context.restoreAuthSystemState();
        UUID id = item.getID();
        Iterator<Item> foundItems = itemService.findAllAuthorized(context, 20, 0);
        List<UUID> uuidList = new LinkedList<>();
        while(foundItems.hasNext()){
            Item nextItem = foundItems.next();
            uuidList.add(nextItem.getID());
        }
        assertTrue(uuidList.contains(id));
    }

    @Test
    public void testFindAllAuthorizedDoesIncludeItemsForWhichEmbargoDateHasPassed() throws SQLException, IOException, AuthorizeException, IllegalAccessException, ParseException{

        Item item = createItem();
        context.restoreAuthSystemState();
        UUID id = item.getID();
        List<Group> anonGroups = groupService.search(context, Group.ANONYMOUS);
        for(Group group: anonGroups){
            ResourcePolicy resourcePolicy = authorizeService.createResourcePolicy(context, item, group, context.getCurrentUser(), Constants.READ, "open-access");
            String dateString = "2000-04-01";
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = formatter.parse(dateString);
            resourcePolicy.setStartDate(startDate);
        }
        Iterator<Item> foundItems = itemService.findAllAuthorized(context, 20, 0);
        List<UUID> uuidList = new LinkedList<>();
        while(foundItems.hasNext()){
            Item nextItem = foundItems.next();
            uuidList.add(nextItem.getID());
        }
        assertTrue(uuidList.contains(id));
    }
}
