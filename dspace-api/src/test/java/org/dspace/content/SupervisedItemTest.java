/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.SupervisedItemService;
import org.dspace.content.service.WorkspaceItemService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.AbstractUnitTest;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.SupervisorService;
import org.junit.*;
import static org.hamcrest.CoreMatchers.*;
import org.apache.log4j.Logger;
import static org.junit.Assert.*;

/**
 *
 * @author pvillega
 */
public class SupervisedItemTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(SupervisedItemTest.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected SupervisedItemService supervisedItemService = ContentServiceFactory.getInstance().getSupervisedItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected SupervisorService supervisorService = EPersonServiceFactory.getInstance().getSupervisorService();

    protected UUID communityId;
    protected UUID groupId;
    protected int workspaceItemId;


    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
        try
        {
            //we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            Community owningCommunity = communityService.create(null, context);
            Collection collection = collectionService.create(context, owningCommunity);
            WorkspaceItem si = workspaceItemService.create(context, collection, false);
            Group gr = groupService.create(context);
            EPerson currentUser = context.getCurrentUser();
            groupService.addMember(context, gr, currentUser);
            groupService.update(context, gr);

            //set a supervisor as editor
            supervisorService.add(context, gr, si, 1);

            communityId = owningCommunity.getID();
            workspaceItemId = si.getID();
            groupId = gr.getID();

            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
            context.complete();
            context = new Context();
            context.setCurrentUser(currentUser);
        } catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init");
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
    public void destroy()
    {
        try {
            context.turnOffAuthorisationSystem();
            communityService.delete(context, communityService.find(context, communityId));
            context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException | IOException ex) {
            log.error("SQL Error in destroy", ex);
            fail("SQL Error in destroy: " + ex.getMessage());
        }
        super.destroy();
    }

    /**
     * Test of getAll method, of class SupervisedItem.
     */
    @Test
    public void testGetAll() throws Exception
    {
        List<WorkspaceItem> found = supervisedItemService.getAll(context);
        assertThat("testGetAll 0", found, notNullValue());
        assertTrue("testGetAll 1", found.size() >= 1);

        boolean added = false;
        for(WorkspaceItem sia: found)
        {
            if(sia.getID() == workspaceItemId)
            {
                added = true;
            }
        }
        assertTrue("testGetAll 2",added);
    }

    /**
     * Test of getSupervisorGroups method, of class SupervisedItem.
     */
    @Test
    public void testGetSupervisorGroups_Context_int() throws Exception
    {
        List<Group> found = workspaceItemService.find(context, workspaceItemId).getSupervisorGroups();
        assertThat("testGetSupervisorGroups_Context_int 0", found, notNullValue());
        assertTrue("testGetSupervisorGroups_Context_int 1", found.size() == 1);
        assertThat("testGetSupervisorGroups_Context_int 2", found.get(0).getID(), equalTo(groupId));
    }

    /**
     * Test of getSupervisorGroups method, of class SupervisedItem.
     */
    @Test
    public void testGetSupervisorGroups_0args() throws Exception 
    {
        List<Group> found = workspaceItemService.find(context, workspaceItemId).getSupervisorGroups();
        assertThat("testGetSupervisorGroups_0args 0", found, notNullValue());
        assertTrue("testGetSupervisorGroups_0args 1", found.size() == 1);

        boolean added = false;
        for(Group g: found)
        {
            if(g.getID().equals(groupId))
            {
                added = true;
            }
        }
        assertTrue("testGetSupervisorGroups_0args 2",added);
    }

    /**
     * Test of findbyEPerson method, of class SupervisedItem.
     */
    @Test
    public void testFindbyEPerson() throws Exception
    {
        context.turnOffAuthorisationSystem();
        List<WorkspaceItem> found = supervisedItemService.findbyEPerson(context, ePersonService.create(context));
        assertThat("testFindbyEPerson 0", found, notNullValue());
        assertTrue("testFindbyEPerson 1", found.size() == 0);

        found = supervisedItemService.findbyEPerson(context, context.getCurrentUser());
        assertThat("testFindbyEPerson 2", found, notNullValue());        
        assertTrue("testFindbyEPerson 3", found.size() >= 1);

        boolean added = false;
        for(WorkspaceItem sia: found)
        {
            if(sia.getID() == workspaceItemId)
            {
                added = true;
            }
        }
        assertTrue("testFindbyEPerson 4",added);

        context.restoreAuthSystemState();
    }

}