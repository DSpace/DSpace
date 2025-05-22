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
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for configuration utilities.
 *
 * Because our command-line tools call System.exit(), we can't expect any code
 * (such as assertions) following the call to main() to be executed.  Instead we
 * set up expectations in advance and attach them to an exit() trapper.
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
    @BeforeAll
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
    @AfterAll
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
    @ExpectSystemExitWithStatus(0)
    public void testMainAllSingle() throws Exception {
        String[] argv;
        argv = new String[] {
            "--property", SINGLE_PROPERTY
        };
        String[] output = tapSystemOut(() -> {
            Configuration.main(argv);
        }).split("\n");
        assertThat(output, arrayWithSize(1));
        assertThat(output[0], equalTo(SINGLE_VALUE));
    }

    /**
     * Test fetching all values of an array property.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    public void testMainAllArray() throws Exception {
        String[] argv;
        argv = new String[] {
            "--property", ARRAY_PROPERTY
        };
        String[] output = tapSystemOut(() -> {
            Configuration.main(argv);
        }).split("\n");
        assertThat(output, arrayWithSize(ARRAY_VALUE.length));
        assertThat(output, arrayContainingInAnyOrder(ARRAY_VALUE));
    }

    /**
     * Test fetching all values of a single-valued property containing property
     * placeholders.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    public void testMainAllSubstitution() throws Exception {
        String[] argv;
        argv = new String[] {
            "--property", PLACEHOLDER_PROPERTY
        };
        String[] output = tapSystemOut(() -> {
            Configuration.main(argv);
        }).split("\n");
        assertThat(output, arrayWithSize(1));
        assertThat(output[0], equalTo(SUBSTITUTED_VALUE));
    }

    /**
     * Test fetching all values of a single-valued property containing property
     * placeholders, suppressing property substitution.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    public void testMainAllRaw() throws Exception {
        // Can it handle a raw property (with substitution placeholders)?
        String[] argv;
        argv = new String[] {
            "--property", PLACEHOLDER_PROPERTY,
            "--raw"
        };
        String[] output = tapSystemOut(() -> {
            Configuration.main(argv);
        }).split("\n");
        assertThat(output, arrayWithSize(1));
        assertThat(output[0], equalTo(PLACEHOLDER_VALUE));
    }

    /**
     * Test fetching all values of an undefined property.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    public void testMainAllUndefined() throws Exception {
        // Can it handle an undefined property?
        String[] argv;
        argv = new String[] {
            "--property", MISSING_PROPERTY
        };
        String[] output = tapSystemOut(() -> {
            Configuration.main(argv);
        }).split("\n");
        assertThat(output, arrayWithSize(0));
    }

    /**
     * Test fetching only the first value of an array property.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    public void testMainFirstArray() throws Exception {
        String[] argv = new String[] {
            "--property", ARRAY_PROPERTY,
            "--first"
        };
        String[] output = tapSystemOut(() -> {
            Configuration.main(argv);
        }).split("\n");
        assertThat(output, arrayWithSize(1));
        assertThat(output[0], equalTo(ARRAY_VALUE[0]));
    }

    /**
     * Test fetching a single-valued property using {@code --first}
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    public void testMainFirstSingle() throws Exception {
        String[] argv = new String[] {
            "--property", SINGLE_PROPERTY,
            "--first"
        };
        String[] output = tapSystemOut(() -> {
            Configuration.main(argv);
        }).split("\n");
        assertThat(output, arrayWithSize(1));
        // Other hamcrest asserts are actual, expected but JUnit 5 is expected, actual, message
        assertEquals(SINGLE_VALUE, output[0], "--first should return only value");
    }
}
