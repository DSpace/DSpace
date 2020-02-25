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

    @Test
    public void workflowMapping_NonMappedCollection() throws WorkflowConfigurationException {
        Collection collection = this.findOrCreateCollectionWithHandle("123456789/6");
        Workflow workflow = xmlWorkflowFactory.getWorkflow(collection);
        assertEquals("defaultWorkflow", workflow.getID());
    }

    @Test
    public void workflowMapping_MappedCollection() throws WorkflowConfigurationException {
        Collection collection = this.findOrCreateCollectionWithHandle("123456789/4");
        Workflow workflow = xmlWorkflowFactory.getWorkflow(collection);
        assertEquals("selectSingleReviewer", workflow.getID());
    }

    private Collection findOrCreateCollectionWithHandle(String handle) {
        try {
            context.turnOffAuthorisationSystem();
            for (Collection collection : this.collectionService.findAll(context)) {
                if (collection.getHandle().equalsIgnoreCase(handle)) {
                    return collection;
                }
            }
            Collection collection = this.collectionService.create(context, owningCommunity, handle);
            context.restoreAuthSystemState();
            return collection;
        } catch (SQLException e) {
            log.error("SQL Error in findOrCreateCollectionWithHandle", e);
            fail("SQL Error in findOrCreateCollectionWithHandle: " + e.getMessage());
        } catch (AuthorizeException e) {
            log.error("Authorization Error in findOrCreateCollectionWithHandle", e);
            fail("Authorization Error in findOrCreateCollectionWithHandle: " + e.getMessage());
        }
        return null;
    }
}
