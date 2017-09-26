/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.workflowbasic;

import org.apache.log4j.Logger;
import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;

import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * This is an integration test to ensure that the basic workflow system 
 * -including methods of the collection service dealing with it- works properly 
 * together with the authorization service.
 * @author Pascal-Nicolas Becker
 */
public class BasicWorkflowAuthorizationRolesIntegrationTest
extends AbstractIntegrationTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BasicWorkflowAuthorizationRolesIntegrationTest.class);
    
    protected ConfigurationService configurationService = new DSpace().getConfigurationService();;
    
    protected Community owningCommunity;
    protected Collection collection;
    protected Item item;
    protected WorkflowItem wfi;
    protected Group mgroup;
    protected EPerson member;
    protected WorkspaceItem wsi;
    protected enum ROLE {ADMIN,SUB,STEP1,STEP2,STEP3;}
    protected HashMap<ROLE,Group> roleGroups = new HashMap<>();
    protected HashMap<ROLE,EPerson> roleEPersons = new HashMap<>();
    protected final String aLanguage = Locale.US.getLanguage();

    public BasicWorkflowAuthorizationRolesIntegrationTest()
    {
        owningCommunity = null;
        collection = null;
        item = null;
        wsi = null;
        wfi = null;
        mgroup = null;
        member = null;
        roleGroups.clear();
        roleEPersons.clear();
    }
    
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
            configurationService.setProperty("workflow.notify.returned.tasks", false);
            context.turnOffAuthorisationSystem();
            
            long date = new Date().getTime();

            this.owningCommunity = Community.create(null, context);
            this.owningCommunity.update();

            this.collection = owningCommunity.createCollection();
            this.collection.update();

            this.member = EPerson.create(context);
            member.setEmail(String.format("wf-member-%d@example.org", date));
            member.setFirstName("Member");
            member.setLanguage(aLanguage);
            member.update();

            this.mgroup = Group.create(context);
            mgroup.addMember(member);
            mgroup.setName(String.format("Member Group %d", date));
            mgroup.update();

            for(ROLE role: ROLE.values()) {
                EPerson person = EPerson.create(context);
                person.setFirstName(String.format("%d", date));
                person.setLastName(String.format("Role %s", role.toString()));
                person.setEmail(String.format("basicwf-test-%s-%d@example.org",role.toString(),date));
                person.setLanguage(aLanguage);
                person.update();

                roleEPersons.put(role, person);

                Group pgroup = Group.create(context);
                roleGroups.put(role, pgroup);
                pgroup.setName(String.format("Group %s %d", role.toString(), date));
                pgroup.addMember(person);
                pgroup.update();

                log.info(String.format("Create ROLE %s GROUP %s PERSON %s", role.toString(), pgroup.getName(), person.getFullName()));
            }

            context.commit();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            Assert.fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            Assert.fail("SQL Error in init: " + ex.getMessage());
        }
        finally
        {
            // restore the authorization system as tests expect it to be in place
            context.restoreAuthSystemState();
        }
    }

    private void contextReload() {
        try {
            if (context.isValid()) {
                context.commit();                    
            } else {
                context.abort();
            }
            context.complete();
            context = new Context();
            if (item != null) {
                int itemid = item.getID();
                item = Item.find(context, itemid);
            }
            if (wfi != null) {
                int wfid = wfi.getID();
                wfi = WorkflowItem.find(context, wfid);
            }
            for(ROLE role: ROLE.values()) {
                EPerson eperson = roleEPersons.get(role);
                Group grp = roleGroups.get(role);
                eperson = EPerson.find(context, eperson.getID());
                Assert.assertNotNull(eperson);
                roleEPersons.put(role, eperson);
                roleGroups.put(role, Group.find(context, grp.getID()));
            }
        } catch (SQLException e) {
            log.error("Error reloading context", e);
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
        try {
            context.complete();
            super.destroy();
            context.clearCache();
        } catch(Exception e) {
            log.error("Error in destroy", e);
        }
    }
    

    /*
     * Prepare a workspace item for subsequent tests
     */
    private void setupItemAndStartWorkflow() throws SQLException, AuthorizeException, FileNotFoundException, IOException {
        context.setCurrentUser(roleEPersons.get(ROLE.SUB));
        wsi = WorkspaceItem.create(context, collection, false);
        item = wsi.getItem();
        Bundle bundle = item.createBundle("ORIGINAL");
        bundle.update();
        item.update();
        wsi.update();
        
        wfi = WorkflowManager.start(context, wsi);
        wfi.update();

        context.commit();
    }

    private void setWorkflowGroup(Context context, int step, Group group) throws SQLException, AuthorizeException {
        collection.setWorkflowGroup(step, group);
    }
    
    /*
     * Model the permission set up for a collection with basic workflow.
     * The last group to advance the item seems to need Add rights
     * SUB has add rights.
     */
    private void setStepPermissions(ROLE step1, ROLE step2, ROLE step3) throws SQLException, AuthorizeException {   
        
        AuthorizeManager.addPolicy(context, collection, Constants.ADMIN, roleGroups.get(ROLE.ADMIN));
        Group last = null;
        if (step1 != null) {
            Group group = roleGroups.get(step1);
            setWorkflowGroup(context, 1, group);
            last = group;
        }
        if (step2 != null) {
            Group group = roleGroups.get(step2);
            setWorkflowGroup(context, 2, group);
            last = group;
        }
        if (step3 != null) {
            Group group = roleGroups.get(step3);
            setWorkflowGroup(context, 3, group);
            last = group;
        }
        
        AuthorizeManager.addPolicy(context, collection, Constants.ADD, last);
        collection.update();
        context.commit();
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a workflow with only Step 1.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep1AllRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, null, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            
            attemptItemUnclaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP1);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim permissions are enforced for a workflow with only Step 1.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep1AllRolesClaim() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, null, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            
            attemptItemUnclaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP1, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that expected transitions succeed for a workflow with only Step 1.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep1ExpectedRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, null, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            
            attemptItemUnclaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP1, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a submitter in a workflow with only Step 1.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep1Submitter() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, null, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());
            
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            
            attemptItemUnclaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvanceFinal(ROLE.SUB, true);
            attemptItemAdvanceFinal(ROLE.STEP1, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a workflow with only Step 2.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep2AllRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, ROLE.STEP2, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());
            
            contextReload();
            attemptItemUnclaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP2);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim permissions are enforced for a workflow with only Step 2.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep2AllRolesClaim() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, ROLE.STEP2, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());
            
            contextReload();
            attemptItemUnclaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP2, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that expected permissions are valid for a workflow with only Step 2.
     * @throws java.sql.SQLException
     * @throws IOException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep2ExpectedRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, ROLE.STEP2, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());
            
            contextReload();
            attemptItemUnclaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP2, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a submitter in a workflow with only Step 2.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep2Submitter() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, ROLE.STEP2, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());
            
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());
            
            contextReload();
            attemptItemUnclaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.SUB, true);
            attemptItemAdvanceFinal(ROLE.STEP2, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }
    /**
     * Test that appropriate claim and advance permissions are enforced for a workflow with only Step 3.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep3AllRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, null, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));
        
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP3);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());
            
            contextReload();
            attemptItemUnclaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.STEP3);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim permissions are enforced for a workflow with only Step 3.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep3AllRolesClaim() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, null, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));
        
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP3);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());
            
            contextReload();
            attemptItemUnclaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.STEP3);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that expected permissions are valid for a workflow with only Step 3.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep3ExpectedRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, null, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));
        
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());
            
            contextReload();
            attemptItemUnclaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }
    
    /**
     * Test that appropriate claim and advance permissions are enforced for a submitter in a workflow with only Step 3.
     * @throws java.sql.SQLException
     * @throws IOException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithStep3Submitter() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, null, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));
        
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());
            
            contextReload();
            attemptItemUnclaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.SUB, true);
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }
    /**
     * Test that appropriate claim and advance permissions are enforced for a 2 step workflow (1 & 2).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps12AllRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, ROLE.STEP2, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
             
            contextReload();
            attemptItemAdvance(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP2);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that expected permissions are valid for a 2 step workflow (1 & 2).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps12ExpectedRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, ROLE.STEP2, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());
            
            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
             
            contextReload();
            attemptItemAdvance(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP2, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a submitter in a 2 step workflow (1 & 2).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps12Submitter() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, ROLE.STEP2, null);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertNull(collection.getWorkflowGroup(3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());
            
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
             
            contextReload();
            attemptItemAdvance(ROLE.SUB, true);
            attemptItemAdvance(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.SUB, true);
            attemptItemAdvanceFinal(ROLE.STEP2, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }


    /**
     * Test that appropriate claim and advance permissions are enforced for a 2 step workflow (1 & 3).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps13AllRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, null, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            attemptItemClaim(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
 
            contextReload();
            attemptItemClaim(ROLE.STEP3);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that expected permissions are valid for a 2 step workflow (1 & 3).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps13ExpectedRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, null, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
 
            contextReload();
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    
    /**
     * Test that appropriate claim and advance permissions are enforced for a submitter in a 2 step workflow (1 & 3).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps13Submitter() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, null, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertNull(collection.getWorkflowGroup(2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.SUB, true);
            attemptItemAdvance(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
 
            contextReload();
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.SUB, true);
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a 2 step workflow (2 & 3).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps23AllRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, ROLE.STEP2, ROLE.STEP3);
            setupItemAndStartWorkflow();
       } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            attemptItemClaim(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.STEP3);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that expected permissions are valid for a 2 step workflow (2 & 3).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps23ExpectedRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, ROLE.STEP2, ROLE.STEP3);
            setupItemAndStartWorkflow();
       } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a submitter in a 2 step workflow (2 & 3).
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps23Submitter() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(null, ROLE.STEP2, ROLE.STEP3);
            setupItemAndStartWorkflow();
       } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertNull(collection.getWorkflowGroup(1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());

            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.SUB, true);
            attemptItemAdvance(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.SUB, true);
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a 3 step workflow.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps123AllRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, ROLE.STEP2, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            attemptItemClaim(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.STEP1);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvance(ROLE.STEP2);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP3);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that expected permissions are valid for a 3 step workflow.
     * @throws java.sql.SQLException
     * @throws IOException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps123ExpectedRoles() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, ROLE.STEP2, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvance(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }

    /**
     * Test that appropriate claim and advance permissions are enforced for a submitter in a 3 step workflow.
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testsetWorkflowWithSteps123Submitter() throws SQLException, AuthorizeException, FileNotFoundException, IOException
    {
        try {
            context.turnOffAuthorisationSystem();
            setStepPermissions(ROLE.STEP1, ROLE.STEP2, ROLE.STEP3);
            setupItemAndStartWorkflow();
        } finally {
            context.restoreAuthSystemState();
        }
        
        try {
            contextReload();
            Assert.assertEquals(collection.getWorkflowGroup(1), roleGroups.get(ROLE.STEP1));
            Assert.assertEquals(collection.getWorkflowGroup(2), roleGroups.get(ROLE.STEP2));
            Assert.assertEquals(collection.getWorkflowGroup(3), roleGroups.get(ROLE.STEP3));

            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1POOL, wfi.getState());

            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP1, wfi.getState());
            
            contextReload();
            attemptItemAdvance(ROLE.SUB, true);
            attemptItemAdvance(ROLE.STEP1, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2POOL, wfi.getState());
            
            contextReload();
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP2, wfi.getState());

            contextReload();
            attemptItemAdvance(ROLE.SUB, true);
            attemptItemAdvance(ROLE.STEP2, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3POOL, wfi.getState());

            contextReload();
            attemptItemClaim(ROLE.SUB, true);
            attemptItemClaim(ROLE.STEP3, false);
            Assert.assertEquals(WorkflowManager.WFSTATE_STEP3, wfi.getState());

            contextReload();
            attemptItemAdvanceFinal(ROLE.SUB, true);
            attemptItemAdvanceFinal(ROLE.STEP3, false);
            contextReload();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.error("Exception found in processing", e);
            contextReload();
            throw e;
        }
    }
/*
     * Each user in ROLE.values() will attempt to claim an item.
     * Only the user with roleValid should succeed.
     */
    public void attemptItemClaim(ROLE roleValid) throws SQLException, AuthorizeException, IOException {
        for(ROLE role: ROLE.values()) {
            if (role == ROLE.ADMIN || role == roleValid) {
                continue;
            }
            attemptItemClaim(role, true);
        }
        if (roleValid != null) {
            attemptItemClaim(roleValid, false);
        }
    }

    /*
     * Each user in ROLE.values() will attempt to advance an item.
     * Only the user with roleValid should succeed.
     */
    public void attemptItemAdvance(ROLE roleValid) throws SQLException, AuthorizeException, IOException {
        for(ROLE role: ROLE.values()) {
            if (role == ROLE.ADMIN || role == roleValid) {
                continue;
            }
            attemptItemAdvance(role, true);
        }
        if (roleValid != null) {
            attemptItemAdvance(roleValid, false);
        }
    }

    /*
     * Each user in ROLE.values() will attempt to advance an item to archived state.
     * Only the user with roleValid should succeed.
     */
    public void attemptItemAdvanceFinal(ROLE roleValid) throws SQLException, AuthorizeException, IOException {
        for(ROLE role: ROLE.values()) {
            if (role == ROLE.ADMIN || role == roleValid) {
                continue;
            }
            attemptItemAdvanceFinal(role, true);
        }
        if (roleValid != null) {
            attemptItemAdvanceFinal(roleValid, false);
        }
    }
    
   /*
    * User role will attempt to claim an item.  
    * expectFail indicates whether or not the action should succeed.
    */
    public void attemptItemClaim(ROLE role, boolean expectAuthFail)
            throws SQLException, AuthorizeException, IOException
    {
        EPerson eperson = roleEPersons.get(role);
        context.setCurrentUser(eperson);

        int state = wfi.getState();
        if (expectAuthFail) {
            try {
                WorkflowManager.claim(context, wfi, context.getCurrentUser());
                //If exception is not thrown, owner will be set and state will change
            } catch(AuthorizeException e) {
                context.abort();
            } catch(Exception e) {
                //handle hibernate exception triggered by database rule
                context.abort();
            }

            contextReload();

            eperson = roleEPersons.get(role);
            if (state != wfi.getState()) {
                log.error(String.format("USER[%-20s] is able to claim task (state %d -> %d) (unexpected)",
                        eperson.getFullName(), state, wfi.getState()), new Exception());
            }

            Assert.assertEquals(state, wfi.getState());        
            Assert.assertNull(wfi.getOwner());
        } else {
            WorkflowManager.claim(context, wfi, context.getCurrentUser());
            contextReload();
            eperson = roleEPersons.get(role);
            if (state == wfi.getState()) {
                log.error(String.format("USER[%-20s] is unable to claim task (state %d -> %d) (unexpected)",
                        eperson.getFullName(), state, wfi.getState()), new Exception());
            }

            Assert.assertNotEquals(state, wfi.getState());
            Assert.assertNotNull(wfi.getOwner());
        }
        Assert.assertFalse(wfi.getItem().isArchived());
    }

    /*
     * User role will attempt to claim an item.  
     * expectFail indicates whether or not the action should succeed.
     */
     public void attemptItemUnclaim(ROLE role, boolean expectAuthFail) throws SQLException, AuthorizeException, IOException {
         EPerson eperson = roleEPersons.get(role);
         context.setCurrentUser(eperson);

         int state = wfi.getState();
         if (expectAuthFail) {
             try {
                 WorkflowManager.unclaim(context, wfi, eperson);
                 //If exception is not thrown, owner will be set and state will change
             } catch(AuthorizeException e) {
                 context.abort();
             } catch(Exception e) {
                 //handle hibernate exception triggered by database rule
                 context.abort();
             }

             contextReload();

             eperson = roleEPersons.get(role);
             if (state != wfi.getState()) {
                 log.error(String.format("USER[%-20s] is able to unclaim task (state %d -> %d) (unexpected)", eperson.getFullName(), state, wfi.getState()), new Exception());
             }

             Assert.assertEquals(state, wfi.getState());        
             Assert.assertNotNull(wfi.getOwner());
         } else {
             WorkflowManager.unclaim(context, wfi, eperson);
             contextReload();
             eperson = roleEPersons.get(role);
             if (state == wfi.getState()) {
                 log.error(String.format("USER[%-20s] is unable to unclaim task (state %d -> %d) (unexpected)", eperson.getFullName(), state, wfi.getState()), new Exception());
             }

             Assert.assertNotEquals(state, wfi.getState());        
             Assert.assertNull(wfi.getOwner());
         }
         Assert.assertFalse(wfi.getItem().isArchived());
     }
    /*
     * User role will attempt to advance an item.  
     * expectFail indicates whether or not the action should succeed.
     */
     public void attemptItemAdvance(ROLE role, boolean expectAuthFail) throws SQLException, AuthorizeException, IOException {
        EPerson eperson = roleEPersons.get(role);
        context.setCurrentUser(eperson);
        int state = wfi.getState();
        EPerson owner = wfi.getOwner();
        if (expectAuthFail) {
            try {
                WorkflowManager.advance(context, wfi, eperson);
                //If exception is not thrown, owner will be unset and state will change
            } catch(AuthorizeException e) {
                context.abort();
            } catch(Exception e) {
                //handle hibernate exception triggered by database rule
                context.abort();
            }
            contextReload();
            owner = EPerson.find(context, owner.getID());
            eperson = roleEPersons.get(role);
            if (state != wfi.getState()) {
                log.error(String.format("USER[%-20s] is able to advance task (state %d -> %d) (unexpected)", eperson.getFullName(), state, wfi.getState()), new Exception());
            }
            Assert.assertEquals(state, wfi.getState());
            Assert.assertEquals(owner, wfi.getOwner());
        } else {
            WorkflowManager.advance(context, wfi, eperson);
            contextReload();
            eperson = roleEPersons.get(role);
            if (state == wfi.getState()) {
                log.error(String.format("USER[%-20s] is unable to advance task (state %d -> %d) (unexpected)", eperson.getFullName(), state, wfi.getState()), new Exception());
            }
            Assert.assertNotEquals(state, wfi.getState());
            Assert.assertNull(wfi.getOwner());
        }
        Assert.assertFalse(wfi.getItem().isArchived());
    }

     /*
      * User role will attempt to advance an item.  
      * expectFail indicates whether or not the action should succeed.
      */
      public void attemptItemAdvanceFinal(ROLE role, boolean expectAuthFail) throws SQLException, AuthorizeException, IOException {
         EPerson eperson = roleEPersons.get(role);
         context.setCurrentUser(eperson);
         int state = wfi.getState();
         EPerson owner = wfi.getOwner();
         if (expectAuthFail) {
             try {
                 WorkflowManager.advance(context, wfi, eperson);
                 //If exception is not thrown, owner will be unset and state will change
             } catch(AuthorizeException e) {
                 context.abort();
             } catch(Exception e) {
                 //handle hibernate exception triggered by database rule
                 context.abort();
             }
             contextReload();
             owner = EPerson.find(context, owner.getID());
             eperson = roleEPersons.get(role);
             if (wfi == null) {
                 log.error(String.format("USER[%-20s] is able to advance task to completion from state %d (unexpected)", eperson.getFullName(), state), new Exception());
             }
             Assert.assertNotNull(wfi);
             Assert.assertEquals(state, wfi.getState());
             Assert.assertEquals(owner, wfi.getOwner());
             Assert.assertFalse(wfi.getItem().isArchived());
         } else {
             WorkflowManager.advance(context, wfi, eperson);
             contextReload();
             eperson = roleEPersons.get(role);
             if (wfi != null) {
                 log.error(String.format("USER[%-20s] is unable to advance task to completion from state %d (unexpected)", eperson.getFullName(), state), new Exception());
             }
             Assert.assertNull(wfi);
             Assert.assertTrue(item.isArchived());
         }
     }

}