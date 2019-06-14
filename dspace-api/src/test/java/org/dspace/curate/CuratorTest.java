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
        instance.setReporter(System.out); // Send any report to standard out.
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
}
