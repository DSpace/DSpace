/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.ctask.general.NoOpCurationTask;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.CurationTaskConfig;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.WorkflowUtils;
import org.dspace.xmlworkflow.XmlWorkflowFactoryImpl;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author mwood
 */
public class XmlWorkflowCuratorServiceImplIT
        extends AbstractIntegrationTestWithDatabase {
    private static final String GROUP_ONE = "one";
    private static final String TASK_NOOP = "noop";
    private static final String NULL_WORKFLOW = "nullWorkflow";

    // Configure the workflow task mapping.
    private static final String TASK_CONFIG_TEMPLATE
            = ("<?xml version='1.0' encoding='UTF-8'?>"
            + "<workflow-curation>"
            + "  <taskset-map>"
            + "    <mapping collection-handle='default' taskset='default'/>"
            + "  </taskset-map>"
            + "  <tasksets>"
            + "    <taskset name='default'>"
            + "      <flowstep name='%s'>"
            + "        <task name='%s'/>"
            + "      </flowstep>"
            + "    </taskset>"
            + "  </tasksets>"
            + "</workflow-curation>");

    private static CollectionService collectionService;

    private static ConfigurationService dspaceConfiguration;

    private static EPersonService epersonService;

    private static GroupService groupService;

    private static XmlWorkflowServiceFactory wsf;

    private static XmlWorkflowService workflowService;

    private static AutowireCapableBeanFactory beanFactory;

    @BeforeClass
    public static void setUpClass()
            throws SQLException {
        // Get some necessary service instances.
        dspaceConfiguration = DSpaceServicesFactory.getInstance().getConfigurationService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        EPersonServiceFactory esf = EPersonServiceFactory.getInstance();
        epersonService = esf.getEPersonService();
        groupService = esf.getGroupService();
        wsf = XmlWorkflowServiceFactory.getInstance();
        workflowService = wsf.getXmlWorkflowService();

        beanFactory = kernelImpl.getServiceManager()
                .getServiceByName(ApplicationContext.class.getName(), ApplicationContext.class)
                .getAutowireCapableBeanFactory();

        // Ensure that a known task exists with a known name.
        final String CURATION_TASK_MAP_NAME
                = "plugin.named.org.dspace.curate.CurationTask";
        dspaceConfiguration.setProperty(CURATION_TASK_MAP_NAME, null);
        dspaceConfiguration.addPropertyValue(CURATION_TASK_MAP_NAME,
                NoOpCurationTask.class.getCanonicalName() + " = " + TASK_NOOP);
    }

    /**
     * Test of doCuration method, of class XmlWorkflowCuratorServiceImpl.
     * @throws Exception passed through.
     */
    @Ignore
    @Test
    public void testDoCuration()
            throws Exception {
    }

    /**
     * Test of curate method, passing a WorkflowItem's ID.
     * @throws Exception passed through.
     */
    @Ignore
    @Test
    public void testCurate_by_id()
            throws Exception {
    }

    /**
     * Test of curate method, passing a WorkflowItem instance.
     * @throws Exception passed through.
     */
    @Test
    public void testCurate_by_instance()
            throws Exception {
        // Find our workflow.
        Workflow workflow = beanFactory.getBean(NULL_WORKFLOW, Workflow.class);

        // Find the workflow step that we will be testing.
        Step firstStep = workflow.getFirstStep();
        Role stepRole = firstStep.getRole();

        /*
         * Build some prerequisite database objects.
         */

        // We will be creating some model objects, so become administrator.
        context.turnOffAuthorisationSystem();

        // We need a Collection to which to attach the workflow.
        Community parent = CommunityBuilder.createCommunity(context)
                .build();
        Collection collection = CollectionBuilder.createCollection(context, parent)
                .build();

        // Create a group for the necessary role.
        Group roleGroup = GroupBuilder.createGroup(context)
                .withName(GROUP_ONE)
                .addMember(eperson)
                .build();

        // Define that role on the Collection.
        WorkflowUtils.createCollectionWorkflowRole(context, collection, stepRole.getId(), roleGroup);
        context.commit(); // XXX try
        context.reloadEntity(collection); // XXX try
        context.reloadEntity(roleGroup); // XXX try

        // Give the workflow factory a known configuration.
        Map<String, Workflow> workflowMapping
                = Collections.singletonMap(collection.getHandle(), workflow);

        XmlWorkflowFactoryImpl workflowFactory
                = (XmlWorkflowFactoryImpl) wsf.getWorkflowFactory();
        workflowFactory.setWorkflowMapping(workflowMapping);

        // Create a dummy WorkflowItem to curate.
        XmlWorkflowItem wfi = WorkflowItemBuilder.createWorkflowItem(context, collection)
                .withAuthor("JUnit")
                .withTitle("Test Item")
                .withSubject("Testing")
                .withIssueDate("2021-02-23")
                .withSubmitter(eperson)
                .build();
        context.commit();
        context.reloadEntity(wfi);
        context.reloadEntity(collection);

        /*
         * Database all set up.
         */
        context.restoreAuthSystemState();

        // Create a curation task configuration document and parse it.
        String taskConfigDocument
                = String.format(TASK_CONFIG_TEMPLATE, firstStep.getId(), TASK_NOOP);
        CurationTaskConfig curationTaskConfig
                = new CurationTaskConfig(
                        new ByteArrayInputStream(
                                taskConfigDocument.getBytes(StandardCharsets.UTF_8)));

        // Set up a Curator.
        Curator curator = new Curator();
        StringBuilder reporter = new StringBuilder();
        curator.setReporter(reporter);

        // Initialize an instance of the Unit Under Test.
        XmlWorkflowCuratorServiceImpl instance = new XmlWorkflowCuratorServiceImpl();
        instance.claimedTaskService = wsf.getClaimedTaskService();
        instance.collectionService = collectionService;
        instance.configurationService = dspaceConfiguration;
        instance.curationTaskConfig = curationTaskConfig;
        instance.ePersonService = epersonService;
        instance.groupService = groupService;
        instance.workflowFactory = workflowFactory;
        instance.workflowItemService = wsf.getXmlWorkflowItemService();
        instance.workflowService = workflowService;

        // Test!
        //XmlWorkflowItem wfi = claimedTask.getWorkflowItem();
        boolean curated = instance.curate(curator, context, wfi);

        // Check whether curation completed.
        assertTrue("Curation did not complete.", curated);

        // Check the run status.
        int status = curator.getStatus(TASK_NOOP);
        assertEquals("Task status was not SUCCESS:", Curator.CURATE_SUCCESS, status);

        // Check run's result.
        String result = curator.getResult(TASK_NOOP);
        // TODO assertSomething("", "expected", result);

        // Check report.
        String report = reporter.toString();
        // TODO assertSomething("", "expected", report);
    }
}
