/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author mwood
 */
public class CurationTaskConfigTest {
    private static CurationTaskConfig instance;

    private static final String TEST1 = "test1";
    private static final String STEP1 = "step1";
    private static final String TASK1 = "task1";
    private static final String ACTION_REJECT = "reject";
    private static final String CONDITION_FAIL = "fail";
    private static final String ADMINISTRATOR = "me";
    private static final String COLLECTION_9_HANDLE = "123456789/9";
    private static final String BAD_HANDLE = "axolotl";
    private static final String CONFIGURATION
            = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<workflow-curation xmlns='https://dspace.org/workflow-curation'>"
            + "  <taskset-map>"
            + "    <mapping collection-handle='default' taskset='" + TEST1 + "'/>"
            + "    <mapping collection-handle='" + COLLECTION_9_HANDLE + "' taskset='default'/>"
            + "  </taskset-map>"
            + "  <tasksets>"
            + "    <taskset name='default'>"
            + "      <flowstep name='default1'>"
            + "        <task name='dontcare'/>"
            + "      </flowstep>"
            + "    </taskset>"
            + "    <taskset name='" + TEST1 + "'>"
            + "      <flowstep name='" + STEP1 + "'>"
            + "        <task name='" + TASK1 + "'>"
            + "          <workflow>" + ACTION_REJECT + "</workflow>"
            + "          <notify on='" + CONDITION_FAIL + "'>" + ADMINISTRATOR + "</notify>"
            + "        </task>"
            + "      </flowstep>"
            + "    </taskset>"
            + "  </tasksets>"
            + "</workflow-curation>";

    @BeforeAll
    public static void setUpClass()
            throws JAXBException, SAXException, IOException {
        instance = new CurationTaskConfig(
                new ByteArrayInputStream(
                        CONFIGURATION.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Test of findTaskSet method, of class CurationTaskConfig.
     */
    @Test
    public void testFindTaskSet() {
        TaskSet taskSet = instance.findTaskSet("123456789/2");
        assertNotNull(taskSet, "The 'default' Handle should match");
        assertEquals(TEST1, taskSet.name, "The wrong taskset was found.");

        FlowStep foundStep = null;
        for (FlowStep step : taskSet.steps) {
            if (STEP1.equals(step.step)) {
                foundStep = step;
                break;
            }
        }
        assertNotNull(foundStep, STEP1 + " should be in this taskset.");

        Task foundTask = null;
        for (Task task : foundStep.tasks) {
            if (TASK1.equals(task.name)) {
                foundTask = task;
                break;
            }
        }
        assertNotNull(foundTask, TASK1 + " should be in " + STEP1 + ".");

        assertTrue(foundTask.powers.contains(ACTION_REJECT),
                TASK1 + " should have 'reject' action");

        List<String> contacts = foundTask.contacts.get(CONDITION_FAIL);
        assertNotNull(contacts,
                TASK1 + " should have contacts for condition " + CONDITION_FAIL);
        assertTrue(contacts.contains(ADMINISTRATOR),
                TASK1 + " on condition " + CONDITION_FAIL + " should contact " + ADMINISTRATOR);
    }

    /**
     * Test of containsKey method, of class CurationTaskConfig.
     */
    @Test
    public void testContainsKey() {
        boolean isContained;

        isContained = instance.containsKey(COLLECTION_9_HANDLE);
        assertTrue(isContained,
                "Collection '" + COLLECTION_9_HANDLE + "' should be found.");

        isContained = instance.containsKey(BAD_HANDLE);
        assertFalse(isContained,
                "Collection '" + BAD_HANDLE + "' should not be found.");
    }
}
