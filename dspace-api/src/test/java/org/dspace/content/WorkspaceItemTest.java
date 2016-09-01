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
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import mockit.NonStrictExpectations;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.AbstractUnitTest;
import org.apache.log4j.Logger;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 * Unit Tests for class WorkspaceItem
 * @author pvillega
 */
public class WorkspaceItemTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(WorkspaceItemTest.class);

    /**
     * WorkspaceItem instance for the tests
     */
    private WorkspaceItem wi;
    private Community owningCommunity;
    private Collection collection;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

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
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            this.wi = workspaceItemService.create(context, collection, true);
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
        } catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
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
    public void destroy()
    {
        wi = null;
        try {
            context.turnOffAuthorisationSystem();
            communityService.removeCollection(context, owningCommunity, collection);
        }
        catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("IO Error in init: " + ex.getMessage());
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
        } finally {
            context.restoreAuthSystemState();
        }
        context.restoreAuthSystemState();
        super.destroy();
    }

    /**
     * Test of find method, of class WorkspaceItem.
     */
    @Test
    public void testFind() throws Exception
    {
        int id = wi.getID();
        WorkspaceItem found = workspaceItemService.find(context, id);
        assertThat("testFind 0",found,notNullValue());
        assertThat("testFind 1",found.getID(), equalTo(id));
        assertThat("testFind 2",found, equalTo(wi));
        assertThat("testFind 3",found.getCollection(),equalTo(wi.getCollection()));
    }

    /**
     * Test of create method, of class WorkspaceItem.
     */
    @Test
    public void testCreateAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Collection ADD perms
                authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
        }};

        boolean template = false;
        WorkspaceItem created = null;

        template = false;
        created = workspaceItemService.create(context, collection, template);
        assertThat("testCreate 0",created,notNullValue());
        assertTrue("testCreate 1",created.getID() >= 0);
        assertThat("testCreate 2",created.getCollection(),equalTo(collection));

        template = true;
        created = workspaceItemService.create(context, collection, template);
        assertThat("testCreate 3",created,notNullValue());
        assertTrue("testCreate 4",created.getID() >= 0);
        assertThat("testCreate 5",created.getCollection(),equalTo(collection));
    }

    /**
     * Test of create method, of class WorkspaceItem.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Collection ADD perms
                authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = new AuthorizeException();
        }};

        boolean template = false;
        WorkspaceItem created = null;

        template = false;
        created = workspaceItemService.create(context, collection, template);
        fail("Exception expected");
    }

    /**
     * Test of findByEPerson method, of class WorkspaceItem.
     */
    @Test
    public void testFindByEPerson() throws Exception
    {
        EPerson ep = context.getCurrentUser();
        List<WorkspaceItem> found = workspaceItemService.findByEPerson(context, ep);
        assertThat("testFindByEPerson 0",found,notNullValue());
        assertTrue("testFindByEPerson 1",found.size() >= 1);
        boolean exists = false;
        for(WorkspaceItem w: found)
        {
            if(w.equals(wi))
            {
                exists = true;
            }
        }
        assertTrue("testFindByEPerson 2",exists);
    }

    /**
     * Test of findByCollection method, of class WorkspaceItem.
     */
    @Test
    public void testFindByCollection() throws Exception
    {
        Collection c = wi.getCollection();
        List<WorkspaceItem> found = workspaceItemService.findByCollection(context, c);
        assertThat("testFindByCollection 0",found,notNullValue());
        assertTrue("testFindByCollection 1",found.size() >= 1);
        assertThat("testFindByCollection 2",found.get(0).getID(), equalTo(wi.getID()));
        assertThat("testFindByCollection 3",found.get(0), equalTo(wi));
        assertThat("testFindByCollection 4",found.get(0).getCollection(),equalTo(wi.getCollection()));
    }

    /**
     * Test of findAll method, of class WorkspaceItem.
     */
    @Test
    public void testFindAll() throws Exception
    {
        List<WorkspaceItem> found = workspaceItemService.findAll(context);
        assertTrue("testFindAll 0",found.size() >= 1);
        boolean added = false;
        for(WorkspaceItem f: found)
        {
            assertThat("testFindAll 1",f,notNullValue());
            assertThat("testFindAll 2",f.getItem(),notNullValue());
            assertThat("testFindAll 3",f.getSubmitter(),notNullValue());
            if(f.equals(wi))
            {
                added = true;
            }
        }
        assertTrue("testFindAll 4",added);
    }

    /**
     * Test of getID method, of class WorkspaceItem.
     */
    @Test
    public void testGetID()
    {
        assertTrue("testGetID 0", wi.getID() >= 0);
    }

    /**
     * Test of getStageReached method, of class WorkspaceItem.
     */
    @Test
    public void testGetStageReached() 
    {
        assertTrue("testGetStageReached 0", wi.getStageReached() == -1);
    }

    /**
     * Test of setStageReached method, of class WorkspaceItem.
     */
    @Test
    public void testSetStageReached()
    {
        wi.setStageReached(4);
        assertTrue("testSetStageReached 0", wi.getStageReached() == 4);
    }

    /**
     * Test of getPageReached method, of class WorkspaceItem.
     */
    @Test
    public void testGetPageReached()
    {
        assertTrue("testGetPageReached 0", wi.getPageReached() == -1);
    }

    /**
     * Test of setPageReached method, of class WorkspaceItem.
     */
    @Test
    public void testSetPageReached() 
    {
        wi.setPageReached(4);
        assertTrue("testSetPageReached 0", wi.getPageReached() == 4);
    }

    /**
     * Test of update method, of class WorkspaceItem.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
		// no need to mockup the authorization as we are the same user that have
		// created the wi
        boolean pBefore = wi.isPublishedBefore();
        wi.setPublishedBefore(!pBefore);
        workspaceItemService.update(context, wi);
        context.commit();
        // force to read the data from the database
        context.uncacheEntity(wi);
        // read all our test attributes objects from the fresh session 
        // to avoid duplicate object in session issue
        wi = workspaceItemService.find(context, wi.getID());
        collection = wi.getCollection();
        owningCommunity = collection.getCommunities().get(0);
        assertTrue("testUpdate", pBefore != wi.isPublishedBefore());
    }
    
    /**
     * Test of update method, of class WorkspaceItem with no WRITE auth.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
    	new NonStrictExpectations(authorizeService.getClass())
        {{
             // Remove Item WRITE perms
        	authorizeService.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false; 
                    authorizeService.authorizeAction((Context) any, (Item) any,
                     Constants.WRITE); result = new AuthorizeException();
        }};
        boolean pBefore = wi.isPublishedBefore();
        wi.setPublishedBefore(!pBefore);
        workspaceItemService.update(context, wi);
    	fail("Exception expected");
    }

    /**
     * Test of deleteAll method, of class WorkspaceItem.
     */
    @Test
    public void testDeleteAllAuth() throws Exception
    {
        int id = wi.getID();
        //we are the user that created it (same context) so we can delete
        workspaceItemService.deleteAll(context, wi);
        WorkspaceItem found = workspaceItemService.find(context, id);
        assertThat("testDeleteAllAuth 0",found,nullValue());
    }

    /**
     * Test of deleteAll method, of class WorkspaceItem.
     */
    @Test
    public void testDeleteAllNoAuth() throws Exception
    {
        //we create a new user in context so we can't delete
        EPerson old = context.getCurrentUser();
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(ePersonService.create(context));
        context.restoreAuthSystemState();
        try
        {
            workspaceItemService.deleteAll(context, wi);
            fail("Exception expected");
        }
        catch(AuthorizeException ex)
        {
            context.setCurrentUser(old);
        }
    }

    /**
     * Test of deleteWrapper method, of class WorkspaceItem.
     */
    @Test
    public void testDeleteWrapperAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item WRITE perms
                authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = null;
        }};

        UUID itemid = wi.getItem().getID();
        int id = wi.getID();
        workspaceItemService.deleteWrapper(context, wi);
        Item found = itemService.find(context, itemid);
        assertThat("testDeleteWrapperAuth 0",found,notNullValue());
        WorkspaceItem wfound = workspaceItemService.find(context, id);
        assertThat("testDeleteWrapperAuth 1",wfound,nullValue());
    }

    /**
     * Test of deleteWrapper method, of class WorkspaceItem.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteWrapperNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
                authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = new AuthorizeException();
        }};

        workspaceItemService.deleteWrapper(context, wi);
        fail("Exception expected");
    }

    /**
     * Test of getItem method, of class WorkspaceItem.
     */
    @Test
    public void testGetItem()
    {
        assertThat("testGetItem 0", wi.getItem(), notNullValue());
    }

    /**
     * Test of getCollection method, of class WorkspaceItem.
     */
    @Test
    public void testGetCollection() 
    {
        assertThat("testGetCollection 0", wi.getCollection(), notNullValue());
    }

    /**
     * Test of getSubmitter method, of class WorkspaceItem.
     */
    @Test
    public void testGetSubmitter() throws Exception
    {
        assertThat("testGetSubmitter 0", wi.getSubmitter(), notNullValue());
        assertThat("testGetSubmitter 1", wi.getSubmitter(), equalTo(context.getCurrentUser()));
    }

    /**
     * Test of hasMultipleFiles method, of class WorkspaceItem.
     */
    @Test
    public void testHasMultipleFiles()
    {
        assertFalse("testHasMultipleFiles 0", wi.hasMultipleFiles());
    }

    /**
     * Test of setMultipleFiles method, of class WorkspaceItem.
     */
    @Test
    public void testSetMultipleFiles()
    {
        wi.setMultipleFiles(true);
        assertTrue("testSetMultipleFiles 0", wi.hasMultipleFiles());
    }

    /**
     * Test of hasMultipleTitles method, of class WorkspaceItem.
     */
    @Test
    public void testHasMultipleTitles() 
    {
        assertFalse("testHasMultipleTitles 0", wi.hasMultipleTitles());
    }

    /**
     * Test of setMultipleTitles method, of class WorkspaceItem.
     */
    @Test
    public void testSetMultipleTitles() 
    {
        wi.setMultipleTitles(true);
        assertTrue("testSetMultipleTitles 0", wi.hasMultipleTitles());
    }

    /**
     * Test of isPublishedBefore method, of class WorkspaceItem.
     */
    @Test
    public void testIsPublishedBefore() 
    {
        assertFalse("testIsPublishedBefore 0", wi.isPublishedBefore());
    }

    /**
     * Test of setPublishedBefore method, of class WorkspaceItem.
     */
    @Test
    public void testSetPublishedBefore() 
    {
        wi.setPublishedBefore(true);
        assertTrue("testSetPublishedBefore 0", wi.isPublishedBefore());
    }

}
