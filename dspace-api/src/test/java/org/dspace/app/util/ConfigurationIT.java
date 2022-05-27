/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.junit.Assert.assertEquals;

import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;

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

    /** Capture standard output. */
    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule();

    /** Capture standard error. */
    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule();

    /** Capture System.exit() value. */
    @Rule
    public final ExpectedSystemExit expectedSystemExit = ExpectedSystemExit.none();

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
    public void testMainAllSingle() {
        String[] argv;
        argv = new String[] {
            "--property", SINGLE_PROPERTY
        };
        expectedSystemExit.expectSystemExitWithStatus(0);
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String[] output = systemOutRule.getLogWithNormalizedLineSeparator()
                        .split("\n");
                assertThat(output, arrayWithSize(1));
            }
        });
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String[] output = systemOutRule.getLogWithNormalizedLineSeparator()
                        .split("\n");
                assertThat(output[0], equalTo(SINGLE_VALUE));
            }
        });
        systemOutRule.enableLog();
        Configuration.main(argv);
    }

    /**
     * Test fetching all values of an array property.
     */
    @Test
    public void testMainAllArray() {
        String[] argv;
        argv = new String[] {
            "--property", ARRAY_PROPERTY
        };
        expectedSystemExit.expectSystemExitWithStatus(0);
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String[] output = systemOutRule.getLogWithNormalizedLineSeparator()
                        .split("\n");
                assertThat(output, arrayWithSize(ARRAY_VALUE.length));
            }
        });
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String[] output = systemOutRule.getLogWithNormalizedLineSeparator()
                        .split("\n");
                assertThat(output, arrayContainingInAnyOrder(ARRAY_VALUE));
            }
        });
        systemOutRule.enableLog();
        Configuration.main(argv);
    }

    /**
     * Test fetching all values of a single-valued property containing property
     * placeholders.
     */
    @Test
    public void testMainAllSubstitution() {
        String[] argv;
        argv = new String[] {
            "--property", PLACEHOLDER_PROPERTY
        };
        expectedSystemExit.expectSystemExitWithStatus(0);
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String[] output = systemOutRule.getLogWithNormalizedLineSeparator()
                        .split("\n");
                assertThat(output, arrayWithSize(1));
            }
        });
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String[] output = systemOutRule.getLogWithNormalizedLineSeparator()
                        .split("\n");
                assertThat(output[0], equalTo(SUBSTITUTED_VALUE));
            }
        });
        systemOutRule.enableLog();
        Configuration.main(argv);
    }

    /**
     * Test fetching all values of a single-valued property containing property
     * placeholders, suppressing property substitution.
     */
    @Test
    public void testMainAllRaw() {
        // Can it handle a raw property (with substitution placeholders)?
        String[] argv;
        argv = new String[] {
            "--property", PLACEHOLDER_PROPERTY,
            "--raw"
        };
        expectedSystemExit.expectSystemExitWithStatus(0);
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String[] output = systemOutRule.getLogWithNormalizedLineSeparator()
                        .split("\n");
                assertThat(output, arrayWithSize(1));
            }
        });
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String[] output = systemOutRule.getLogWithNormalizedLineSeparator()
                        .split("\n");
                assertThat(output[0], equalTo(PLACEHOLDER_VALUE));
            }
        });
        systemOutRule.enableLog();
        Configuration.main(argv);
    }

    /**
     * Test fetching all values of an undefined property.
     */
    @Test
    public void testMainAllUndefined() {
        // Can it handle an undefined property?
        String[] argv;
        argv = new String[] {
            "--property", MISSING_PROPERTY
        };
        expectedSystemExit.expectSystemExitWithStatus(0);
        expectedSystemExit.checkAssertionAfterwards(new Assertion() {
            @Override public void checkAssertion() {
                String outputs = systemOutRule.getLogWithNormalizedLineSeparator();
                String[] output = outputs.split("\n");
                assertThat(output, arrayWithSize(0)); // Huh?  Shouldn't split() return { "" } ?
            }
        });
        systemOutRule.enableLog();
        Configuration.main(argv);
    }

    /**
     * Test fetching only the first value of an array property.
     */
    @Test
    public void testMainFirstArray() {
        String[] argv = new String[] {
            "--property", ARRAY_PROPERTY,
            "--first"
        };
        expectedSystemExit.expectSystemExitWithStatus(0);
        expectedSystemExit.checkAssertionAfterwards(() -> {
            String outputs = systemOutRule.getLogWithNormalizedLineSeparator();
            String[] output = outputs.split("\n");
            assertThat(output, arrayWithSize(1));
            assertEquals("--first should return first value", output[0], ARRAY_VALUE[0]);
        });
        systemOutRule.enableLog();
        Configuration.main(argv);
    }

    /**
     * Test fetching a single-valued property using {@code --first}
     */
    @Test
    public void testMainFirstSingle() {
        String[] argv = new String[] {
            "--property", SINGLE_PROPERTY,
            "--first"
        };
        expectedSystemExit.expectSystemExitWithStatus(0);
        expectedSystemExit.checkAssertionAfterwards(() -> {
            String outputs = systemOutRule.getLogWithNormalizedLineSeparator();
            String[] output = outputs.split("\n");
            assertThat(output, arrayWithSize(1));
            assertEquals("--first should return only value", output[0], SINGLE_VALUE);
        });
        systemOutRule.enableLog();
        Configuration.main(argv);
    }
}
