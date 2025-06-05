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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for configuration utilities.
 */
public class ConfigurationIT extends AbstractDSpaceTest {

    private static ConfigurationService cfg;

    private static final String SINGLE_PROPERTY = "test.single";
    private static final String SINGLE_VALUE = "value";

    private static final String ARRAY_PROPERTY = "test.array";
    private static final String[] ARRAY_VALUE = { "one", "two" };

    private static final String PLACEHOLDER_PROPERTY = "test.substituted";
    private static final String PLACEHOLDER_VALUE = "insert ${test.single} here";
    private static final String SUBSTITUTED_VALUE = "insert value here";

    private static final String MISSING_PROPERTY = "test.missing";

    private static final Logger log = LogManager.getLogger(ConfigurationIT.class);

    @BeforeAll
    public static void setupSuite() {
        cfg = kernelImpl.getConfigurationService();

        cfg.setProperty(SINGLE_PROPERTY, SINGLE_VALUE);
        cfg.setProperty(ARRAY_PROPERTY, ARRAY_VALUE);
        cfg.setProperty(PLACEHOLDER_PROPERTY, PLACEHOLDER_VALUE);
        cfg.setProperty(MISSING_PROPERTY, null);
    }

    @AfterAll
    public static void teardownSuite() {
        if (null != cfg) {
            cfg.setProperty(SINGLE_PROPERTY, null);
            cfg.setProperty(ARRAY_PROPERTY, null);
            cfg.setProperty(PLACEHOLDER_PROPERTY, null);
        }
    }

    @Test
    public void testMainAllSingle() throws Exception {
        String[] argv = new String[] {
                "--property", SINGLE_PROPERTY
        };
        String output = tapSystemOut(() -> {
            int exitCode = Configuration.execute(argv);
            assertEquals(0, exitCode);
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(1));
        assertThat(outputLines[0], equalTo(SINGLE_VALUE));
    }

    @Test
    public void testMainAllArray() throws Exception {
        String[] argv = new String[] {
                "--property", ARRAY_PROPERTY
        };
        String output = tapSystemOut(() -> {
            int exitCode = Configuration.execute(argv);
            assertEquals(0, exitCode);
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(ARRAY_VALUE.length));
        assertThat(outputLines, arrayContainingInAnyOrder(ARRAY_VALUE));
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
        String output = tapSystemOut(() -> {
            int exitCode = Configuration.execute(argv);
            assertEquals(0, exitCode);
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(1));
        assertThat(outputLines[0], equalTo(SUBSTITUTED_VALUE));
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
        String output = tapSystemOut(() -> {
            int exitCode = Configuration.execute(argv);
            assertEquals(0, exitCode);
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(1));
        assertThat(outputLines[0], equalTo(PLACEHOLDER_VALUE));
    }

    /**
     * Test fetching all values of an undefined property.
     */
    @Test
    public void testMainAllUndefined() throws Exception {
        String[] argv = new String[] {
            "--property", MISSING_PROPERTY
        };
        String output = tapSystemOut(() -> {
            int exitCode = Configuration.execute(argv);
            assertEquals(0, exitCode);
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(0));
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
        String output = tapSystemOut(() -> {
            int exitCode = Configuration.execute(argv);
            assertEquals(0, exitCode);
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(1));
        assertThat(outputLines[0], equalTo(ARRAY_VALUE[0]));
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
        String output = tapSystemOut(() -> {
            int exitCode = Configuration.execute(argv);
            assertEquals(0, exitCode);
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(1));
        assertEquals(SINGLE_VALUE, outputLines[0], "--first should return only value");
    }

    private String[] parseOutputLines(String output) {
        String trimmedOutput = output.trim();
        if (trimmedOutput.isEmpty()) {
            return new String[0];
        }
        return trimmedOutput.split("\n");
    }
}
