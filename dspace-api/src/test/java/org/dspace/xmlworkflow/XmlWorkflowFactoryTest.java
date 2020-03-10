/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Workflow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that check that the spring bean {@link org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactoryImpl}
 * in workflow.xml gets created correctly
 *
 * @author Maria Verdonck (Atmire) on 19/12/2019
 */
public class XmlWorkflowFactoryTest extends AbstractUnitTest {

    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private XmlWorkflowFactory xmlWorkflowFactory
            = new DSpace().getServiceManager().getServiceByName("xmlWorkflowFactory",
            XmlWorkflowFactoryImpl.class);
    private Community owningCommunity;
    private Collection mappedCollection;
    private Collection nonMappedCollection;

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(XmlWorkflowFactoryTest.class);

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
            //we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            this.mappedCollection =
                    this.collectionService.create(context, owningCommunity, "123456789/workflow-test-1");
            this.nonMappedCollection = this.collectionService.create(context, owningCommunity, "123456789/999");
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
        } catch (SQLException e) {
            log.error("SQL Error in init", e);
            fail("SQL Error in init: " + e.getMessage());
        } catch (AuthorizeException e) {
            log.error("Authorization Error in init", e);
            fail("Authorization Error in init: " + e.getMessage());
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
        context.turnOffAuthorisationSystem();

        try {
            this.collectionService.delete(context, this.nonMappedCollection);
            this.collectionService.delete(context, this.mappedCollection);
            this.communityService.delete(context, this.owningCommunity);
        } catch (Exception e) {
            log.error("Error in destroy", e);
        }

        context.restoreAuthSystemState();
        this.owningCommunity = null;
        this.nonMappedCollection = null;
        this.mappedCollection = null;
        try {
            super.destroy();
        } catch (Exception e) {
            log.error("Error in destroy", e);
        }
    }

    @Test
    public void workflowMapping_NonMappedCollection() throws WorkflowConfigurationException {
        Workflow workflow = xmlWorkflowFactory.getWorkflow(this.nonMappedCollection);
        assertEquals(workflow.getID(), "defaultWorkflow");
    }

    @Test
    public void workflowMapping_MappedCollection() throws WorkflowConfigurationException {
        Workflow workflow = xmlWorkflowFactory.getWorkflow(this.mappedCollection);
        assertEquals(workflow.getID(), "selectSingleReviewer");
    }
}
