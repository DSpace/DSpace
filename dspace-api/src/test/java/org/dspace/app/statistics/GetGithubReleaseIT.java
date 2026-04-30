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
        initializers = { DSpaceKernelInitializer.class, DSpaceConfigurationInitializer.class },
        locations = { "classpath:spring/*.xml" }
)
public class GetGithubReleaseIT
        extends AbstractIntegrationTest {
    /**
     * Test of getScriptConfiguration method, of class GetGithubRelease.
     */
    @Ignore
    @Test
    public void testGetScriptConfiguration() {
    }

    /**
     * Test of setup method, of class GetGithubRelease.
     */
    @Ignore
    @Test
    public void testSetup() {
    }

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
            "-" + GetGithubReleaseOptions.OPT_OWNER, "atmire",
            "-" + GetGithubReleaseOptions.OPT_REPO, "COUNTER-Robots",
        };

        // Create and configure the test instance.
        GetGithubRelease instance = new GetGithubRelease();
        instance.configuration = new GetGithubReleaseScriptConfiguration();
        instance.initialize(args, new TestDSpaceRunnableHandler(), eperson);

        // Test!
        instance.run();

        // Interpret results.
        Path archiveFilePath = instance.getArchiveFilePath();
        if (Files.exists(archiveFilePath)) {
            Files.delete(archiveFilePath); // Clean up our mess.
        } else {
            fail("Archive file should have been created at " + archiveFilePath.toString());
        }
    }
}
