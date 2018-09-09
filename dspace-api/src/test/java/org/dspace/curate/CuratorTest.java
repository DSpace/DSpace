/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.dspace.AbstractUnitTest;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mhwood
 */
public class CuratorTest
        extends AbstractUnitTest {
    private static final SiteService SITE_SERVICE = ContentServiceFactory.getInstance().getSiteService();

    static final String RUN_PARAMETER_NAME = "runParameter";
    static final String RUN_PARAMETER_VALUE = "a parameter";
    static final String TASK_PROPERTY_NAME = "taskProperty";
    static final String TASK_PROPERTY_VALUE = "a property";

    /** Value of a known runtime parameter, if any. */
    static String runParameter;

    /** Value of a known task property, if any. */
    static String taskProperty;

    public CuratorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of addParameter method, of class Curator.
     */
/*
    @Test
    public void testAddParameter() {
        System.out.println("addParameter");
        String name = "";
        String value = "";
        Curator instance = new Curator();
        instance.addParameter(name, value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of addParameters method, of class Curator.
     */
/*
    @Test
    public void testAddParameters() {
        System.out.println("addParameters");
        Map<String, String> parameters = null;
        Curator instance = new Curator();
        instance.addParameters(parameters);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getRunParameter method, of class Curator.
     */
/*
    @Test
    public void testGetRunParameter() {
        System.out.println("getRunParameter");
        String name = "";
        Curator instance = new Curator();
        String expResult = "";
        String result = instance.getRunParameter(name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of addTask method, of class Curator.
     */
/*
    @Test
    public void testAddTask() {
        System.out.println("addTask");
        String taskName = "";
        Curator instance = new Curator();
        Curator expResult = null;
        Curator result = instance.addTask(taskName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of hasTask method, of class Curator.
     */
/*
    @Test
    public void testHasTask() {
        System.out.println("hasTask");
        String taskName = "";
        Curator instance = new Curator();
        boolean expResult = false;
        boolean result = instance.hasTask(taskName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of removeTask method, of class Curator.
     */
/*
    @Test
    public void testRemoveTask() {
        System.out.println("removeTask");
        String taskName = "";
        Curator instance = new Curator();
        Curator expResult = null;
        Curator result = instance.removeTask(taskName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setInvoked method, of class Curator.
     */
/*
    @Test
    public void testSetInvoked() {
        System.out.println("setInvoked");
        Curator.Invoked mode = null;
        Curator instance = new Curator();
        Curator expResult = null;
        Curator result = instance.setInvoked(mode);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setReporter method, of class Curator.
     */
/*
    @Test
    public void testSetReporter() {
        System.out.println("setReporter");
        String reporter = "";
        Curator instance = new Curator();
        Curator expResult = null;
        Curator result = instance.setReporter(reporter);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setTransactionScope method, of class Curator.
     */
/*
    @Test
    public void testSetTransactionScope() {
        System.out.println("setTransactionScope");
        Curator.TxScope scope = null;
        Curator instance = new Curator();
        Curator expResult = null;
        Curator result = instance.setTransactionScope(scope);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of curate method, of class Curator.
     */
/*
    @Test
    public void testCurate_Context_String() throws Exception {
        System.out.println("curate");
        String id = "";
        Curator instance = new Curator();
        instance.curate(context, id);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of curate method, of class Curator.
     * Currently this just tests task properties and run parameters.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testCurate_DSpaceObject()
            throws Exception {
        System.out.println("curate");

        final String TASK_NAME = "dummyTask";

        // Configure the task to be run.
        ConfigurationService cfg = kernelImpl.getConfigurationService();
        cfg.setProperty("plugin.named.org.dspace.curate.CurationTask",
                DummyTask.class.getName() + " = " + TASK_NAME);
        cfg.setProperty(TASK_NAME + '.' + TASK_PROPERTY_NAME, TASK_PROPERTY_VALUE);

        // Get and configure a Curator.
        Curator instance = new Curator();
        instance.setReporter("-"); // Send any report to standard out. FIXME when DS-3989 is merged
        instance.addTask(TASK_NAME);

        // Configure the run.
        Map<String, String> parameters = new HashMap<>();
        parameters.put(RUN_PARAMETER_NAME, RUN_PARAMETER_VALUE);
        instance.addParameters(parameters);

        // Curate the site.
        DSpaceObject dso = SITE_SERVICE.findSite(context);
        instance.curate(context, dso);

        // Check the result.
        System.out.format("Task %s result was '%s'%n",
                TASK_NAME, instance.getResult(TASK_NAME));
        System.out.format("Task %s status was %d%n",
                TASK_NAME, instance.getStatus(TASK_NAME));
        assertEquals("Unexpected task status",
                Curator.CURATE_SUCCESS, instance.getStatus(TASK_NAME));
        assertEquals("Wrong run parameter", RUN_PARAMETER_VALUE, runParameter);
        assertEquals("Wrong task property", TASK_PROPERTY_VALUE, taskProperty);
    }

    /**
     * Test of curate method, of class Curator.
     */
/*
    @Test
    public void testCurate_Context_DSpaceObject() throws Exception {
        System.out.println("curate");
        DSpaceObject dso = null;
        Curator instance = new Curator();
        instance.curate(context, dso);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of queue method, of class Curator.
     */
/*
    @Test
    public void testQueue() throws Exception {
        System.out.println("queue");
        Context c = null;
        String id = "";
        String queueId = "";
        Curator instance = new Curator();
        instance.queue(c, id, queueId);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of clear method, of class Curator.
     */
/*
    @Test
    public void testClear() {
        System.out.println("clear");
        Curator instance = new Curator();
        instance.clear();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of report method, of class Curator.
     */
/*
    @Test
    public void testReport() {
        System.out.println("report");
        String message = "";
        Curator instance = new Curator();
        instance.report(message);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getStatus method, of class Curator.
     */
/*
    @Test
    public void testGetStatus() {
        System.out.println("getStatus");
        String taskName = "";
        Curator instance = new Curator();
        int expResult = 0;
        int result = instance.getStatus(taskName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of getResult method, of class Curator.
     */
/*
    @Test
    public void testGetResult() {
        System.out.println("getResult");
        String taskName = "";
        Curator instance = new Curator();
        String expResult = "";
        String result = instance.getResult(taskName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of setResult method, of class Curator.
     */
/*
    @Test
    public void testSetResult() {
        System.out.println("setResult");
        String taskName = "";
        String result_2 = "";
        Curator instance = new Curator();
        instance.setResult(taskName, result_2);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of curationContext method, of class Curator.
     */
/*
    @Test
    public void testCurationContext() throws Exception {
        System.out.println("curationContext");
        Context expResult = null;
        Context result = Curator.curationContext();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    /**
     * Test of isContainer method, of class Curator.
     */
/*
    @Test
    public void testIsContainer() {
        System.out.println("isContainer");
        DSpaceObject dso = null;
        boolean expResult = false;
        boolean result = Curator.isContainer(dso);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/
}
