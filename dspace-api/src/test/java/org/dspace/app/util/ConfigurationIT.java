/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import com.ginsberg.junit.exit.SystemExitPreventedException;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
    private static final String[] ARRAY_VALUE = {"one", "two"};

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
        argv = new String[]{
                "--property", SINGLE_PROPERTY
        };

        String output = tapSystemOut(() -> {
            try {
                Configuration.main(argv);
            } catch (SystemExitPreventedException ignored) {
                // Ignore the exception raised by the agent
            }
        });
        System.out.println("### output2");
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(1));
        assertEquals(SINGLE_VALUE, outputLines[0], "--first should return only value");
        System.out.println("### output: " + Arrays.toString(outputLines));

    }

    @Test
    @ExpectSystemExitWithStatus(0)
    public void testMainAllArray() throws Exception {
        String[] argv = new String[] {
                "--property", ARRAY_PROPERTY
        };
        String output = tapSystemOut(() -> {
            try {
                Configuration.main(argv);
            } catch (SystemExitPreventedException ignored) {
                // Ignore the exception raised by the agent
            }
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
    @ExpectSystemExitWithStatus(0)
    public void testMainAllSubstitution() throws Exception {
        String[] argv = new String[] {
                "--property", PLACEHOLDER_PROPERTY
        };
        String output = tapSystemOut(() -> {
            try {
                Configuration.main(argv);
            } catch (SystemExitPreventedException ignored) {
                // Ignore the exception raised by the agent
            }
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
    @ExpectSystemExitWithStatus(0)
    public void testMainAllRaw() throws Exception {
        String[] argv = new String[] {
                "--property", PLACEHOLDER_PROPERTY,
                "--raw"
        };
        String output = tapSystemOut(() -> {
            try {
                Configuration.main(argv);
            } catch (SystemExitPreventedException ignored) {
                // Ignore the exception raised by the agent
            }
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(1));
        assertThat(outputLines[0], equalTo(PLACEHOLDER_VALUE));
    }

    /**
     * Test fetching all values of an undefined property.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    public void testMainAllUndefined() throws Exception {
        String[] argv = new String[] {
                "--property", MISSING_PROPERTY
        };
        String output = tapSystemOut(() -> {
            try {
                Configuration.main(argv);
            } catch (SystemExitPreventedException ignored) {
                // Ignore the exception raised by the agent
            }
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(0));
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
        String output = tapSystemOut(() -> {
            try {
                Configuration.main(argv);
            } catch (SystemExitPreventedException ignored) {
                // Ignore the exception raised by the agent
            }
        });
        String[] outputLines = parseOutputLines(output);
        assertThat(outputLines, arrayWithSize(1));
        assertThat(outputLines[0], equalTo(ARRAY_VALUE[0]));
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
        String output = tapSystemOut(() -> {
            try {
                Configuration.main(argv);
            } catch (SystemExitPreventedException ignored) {
                // Ignore the exception raised by the agent
            }
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