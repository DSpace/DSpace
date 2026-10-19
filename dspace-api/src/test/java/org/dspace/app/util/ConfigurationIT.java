/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.ArrayMatching.arrayContainingInAnyOrder;
import static org.junit.Assert.assertEquals;

import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for configuration utilities.
 *
 * This test uses Configuration.runConfiguration() which returns an exit code
 * instead of calling System.exit(). This allows testing without SecurityManager
 * which was removed in Java 21.
 *
 * @author mhwood
 */
public class ConfigurationIT
        extends AbstractDSpaceTest {

    private static ConfigurationService cfg;

    private static final String SINGLE_PROPERTY = "test.single";
    private static final String SINGLE_VALUE = "value";

    private static final String ARRAY_PROPERTY = "test.array";
    private static final String[] ARRAY_VALUE = { "one", "two" };

    private static final String PLACEHOLDER_PROPERTY = "test.substituted";
    private static final String PLACEHOLDER_VALUE = "insert ${test.single} here"; // Keep aligned with SINGLE_NAME
    private static final String SUBSTITUTED_VALUE = "insert value here"; // Keep aligned with SINGLE_VALUE

    private static final String MISSING_PROPERTY = "test.missing";

    /**
     * Create some expected properties before all tests.
     */
    @BeforeClass
    public static void setupSuite() {
        cfg = kernelImpl.getConfigurationService();

        cfg.setProperty(SINGLE_PROPERTY, SINGLE_VALUE);
        cfg.setProperty(ARRAY_PROPERTY, ARRAY_VALUE);
        cfg.setProperty(PLACEHOLDER_PROPERTY, PLACEHOLDER_VALUE);
        cfg.setProperty(MISSING_PROPERTY, null); // Ensure that this one is undefined
    }

    /**
     * After all tests, remove the properties that were created at entry.
     */
    @AfterClass
    public static void teardownSuite() {
        if (null != cfg) {
            cfg.setProperty(SINGLE_PROPERTY, null);
            cfg.setProperty(ARRAY_PROPERTY, null);
            cfg.setProperty(PLACEHOLDER_PROPERTY, null);
        }
    }

    /**
     * Test fetching all values of a single-valued property.
     */
    @Test
    public void testMainAllSingle() throws Exception {
        String[] argv = new String[] {
            "--property", SINGLE_PROPERTY
        };

        String stdout = tapSystemOut(() -> {
            int exitStatus = Configuration.runConfiguration(argv);
            assertEquals("Exit status should be 0", 0, exitStatus);
        });

        String[] output = stdout.trim().split("\n");
        assertThat(output, arrayWithSize(1));
        assertThat(output[0], equalTo(SINGLE_VALUE));
    }

    /**
     * Test fetching all values of an array property.
     */
    @Test
    public void testMainAllArray() throws Exception {
        String[] argv = new String[] {
            "--property", ARRAY_PROPERTY
        };

        String stdout = tapSystemOut(() -> {
            int exitStatus = Configuration.runConfiguration(argv);
            assertEquals("Exit status should be 0", 0, exitStatus);
        });

        String[] output = stdout.trim().split("\n");
        assertThat(output, arrayWithSize(ARRAY_VALUE.length));
        assertThat(output, arrayContainingInAnyOrder(ARRAY_VALUE));
    }

    /**
     * Test fetching all values of a single-valued property containing property
     * placeholders.
     */
    @Test
    public void testMainAllSubstitution() throws Exception {
        String[] argv = new String[] {
            "--property", PLACEHOLDER_PROPERTY
        };

        String stdout = tapSystemOut(() -> {
            int exitStatus = Configuration.runConfiguration(argv);
            assertEquals("Exit status should be 0", 0, exitStatus);
        });

        String[] output = stdout.trim().split("\n");
        assertThat(output, arrayWithSize(1));
        assertThat(output[0], equalTo(SUBSTITUTED_VALUE));
    }

    /**
     * Test fetching all values of a single-valued property containing property
     * placeholders, suppressing property substitution.
     */
    @Test
    public void testMainAllRaw() throws Exception {
        String[] argv = new String[] {
            "--property", PLACEHOLDER_PROPERTY,
            "--raw"
        };

        String stdout = tapSystemOut(() -> {
            int exitStatus = Configuration.runConfiguration(argv);
            assertEquals("Exit status should be 0", 0, exitStatus);
        });

        String[] output = stdout.trim().split("\n");
        assertThat(output, arrayWithSize(1));
        assertThat(output[0], equalTo(PLACEHOLDER_VALUE));
    }

    /**
     * Test fetching all values of an undefined property.
     */
    @Test
    public void testMainAllUndefined() throws Exception {
        String[] argv = new String[] {
            "--property", MISSING_PROPERTY
        };

        String stdout = tapSystemOut(() -> {
            int exitStatus = Configuration.runConfiguration(argv);
            assertEquals("Exit status should be 0", 0, exitStatus);
        });

        // Empty output for undefined property
        String[] output = stdout.trim().isEmpty() ? new String[0] : stdout.trim().split("\n");
        assertThat(output, arrayWithSize(0));
    }

    /**
     * Test fetching only the first value of an array property.
     */
    @Test
    public void testMainFirstArray() throws Exception {
        String[] argv = new String[] {
            "--property", ARRAY_PROPERTY,
            "--first"
        };

        String stdout = tapSystemOut(() -> {
            int exitStatus = Configuration.runConfiguration(argv);
            assertEquals("Exit status should be 0", 0, exitStatus);
        });

        String[] output = stdout.trim().split("\n");
        assertThat(output, arrayWithSize(1));
        assertEquals("--first should return first value", ARRAY_VALUE[0], output[0]);
    }

    /**
     * Test fetching a single-valued property using {@code --first}
     */
    @Test
    public void testMainFirstSingle() throws Exception {
        String[] argv = new String[] {
            "--property", SINGLE_PROPERTY,
            "--first"
        };

        String stdout = tapSystemOut(() -> {
            int exitStatus = Configuration.runConfiguration(argv);
            assertEquals("Exit status should be 0", 0, exitStatus);
        });

        String[] output = stdout.trim().split("\n");
        assertThat(output, arrayWithSize(1));
        assertEquals("--first should return only value", SINGLE_VALUE, output[0]);
    }
}
