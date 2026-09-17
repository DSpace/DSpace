/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;

import org.dspace.AbstractIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.util.DSpaceConfigurationInitializer;
import org.dspace.util.DSpaceKernelInitializer;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test suite for {@link GetGithubRelease}.
 *
 * @author mwood
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        initializers = {
            DSpaceKernelInitializer.class,
            DSpaceConfigurationInitializer.class
        },
        locations = { "classpath:spring/*.xml" }
)
public class GetGithubReleaseIT
        extends AbstractIntegrationTest {
    /* Use these after Project-Counter has a release. */
//    private static final String OWNER = "Project-Counter";
//    private static final String REPO = "counter-bots";
    /* Stop using these after Project-Counter has a release. */
    private static final String OWNER = "atmire";
    private static final String REPO = "COUNTER-Robots";

    /**
     * Test fetching the zip archive.
     *
     * @throws Exception passed through.
     */
    @Test
    public void testInternalRunGetZip()
            throws Exception {
        // Test with this repository
        final String[] args = {
            "-" + GetGithubReleaseOptions.OPT_OWNER, OWNER,
            "-" + GetGithubReleaseOptions.OPT_REPO, REPO,
        };

        // Create and configure the test instance.
        GetGithubRelease instance = new GetGithubRelease();
        instance.configuration = new GetGithubReleaseScriptConfiguration();
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        instance.initialize(args, handler, eperson);

        // Test!
        instance.run();

        // Interpret results.
        Path archiveFilePath = instance.getArchiveFilePath();
        if (null != archiveFilePath) {
            if (Files.exists(archiveFilePath)) {
                Files.delete(archiveFilePath); // Clean up our mess.
            } else {
                fail("Archive file should have been created at "
                        + archiveFilePath.toString());
            }
        }

        Exception e = handler.getException();
        if (null != e) {
            fail(e.toString());
        }
    }
}
