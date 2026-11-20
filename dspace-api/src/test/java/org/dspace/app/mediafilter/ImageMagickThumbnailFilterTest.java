/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.im4java.core.CommandException;
import org.im4java.core.IMOperation;
import org.im4java.core.ImageCommand;
import org.im4java.process.ProcessEvent;
import org.im4java.process.ProcessListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the time limit on ImageMagick commands in {@link ImageMagickThumbnailFilter}.
 * <p>
 * ImageMagick is not required: the tests replace {@code convert} with standard POSIX commands.
 */
public class ImageMagickThumbnailFilterTest extends AbstractUnitTest {

    private static final String TIMEOUT_PROPERTY = ImageMagickThumbnailFilter.PRE + ".timeout";

    private final ConfigurationService configurationService
        = DSpaceServicesFactory.getInstance().getConfigurationService();

    private final ImageMagickThumbnailFilter filter = new ImageMagickImageThumbnailFilter();

    @Before
    public void requirePosix() {
        assumeFalse("Uses POSIX commands", SystemUtils.IS_OS_WINDOWS);
    }

    @After
    public void resetTimeout() {
        configurationService.setProperty(TIMEOUT_PROPERTY, null);
    }

    @Test
    public void commandThatFinishesInTimeSucceeds() throws Exception {
        configurationService.setProperty(TIMEOUT_PROPERTY, 10);
        filter.runCommand(command("true"), new IMOperation());
    }

    @Test
    public void failingCommandThrowsCommandException() {
        configurationService.setProperty(TIMEOUT_PROPERTY, 10);
        assertCommandFailed(assertThrows(CommandException.class,
            () -> filter.runCommand(command("false"), new IMOperation())));
    }

    @Test
    public void failingCommandWithoutTimeoutThrowsCommandException() {
        configurationService.setProperty(TIMEOUT_PROPERTY, 0);
        assertCommandFailed(assertThrows(CommandException.class,
            () -> filter.runCommand(command("false"), new IMOperation())));
    }

    @Test
    public void commandExceedingTimeoutIsStoppedWithItsChildProcesses() throws Exception {
        configurationService.setProperty(TIMEOUT_PROPERTY, 1);
        File childPidFile = File.createTempFile("imagemagick-timeout-test", ".pid");
        childPidFile.deleteOnExit();

        // A parent process that starts a long-running child, like ImageMagick starting Ghostscript
        ImageCommand cmd = command("sh");
        AtomicReference<Process> parent = new AtomicReference<>();
        cmd.addProcessListener(new ProcessListener() {
            @Override
            public void processStarted(Process p) {
                parent.set(p);
            }

            @Override
            public void processTerminated(ProcessEvent event) {
            }
        });
        IMOperation op = new IMOperation();
        op.addRawArgs("-c", "sleep 60 & echo $! > " + childPidFile.getAbsolutePath() + "; wait");

        long start = System.nanoTime();
        assertCommandFailed(assertThrows(CommandException.class, () -> filter.runCommand(cmd, op)));
        long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        assertTrue("Stopped soon after the time limit, took " + elapsedSeconds + "s", elapsedSeconds < 10);

        assertTrue("Parent process was stopped", parent.get().waitFor(5, TimeUnit.SECONDS));
        long childPid = Long.parseLong(
            Files.readString(childPidFile.toPath(), StandardCharsets.US_ASCII).trim());
        Optional<ProcessHandle> child = ProcessHandle.of(childPid);
        if (child.isPresent()) {
            child.get().onExit().get(5, TimeUnit.SECONDS);
            assertFalse("Child process was stopped", child.get().isAlive());
        }
    }

    /**
     * Fail if the exception means the test command could not be started at all, which would make
     * the test pass for the wrong reason.
     */
    private void assertCommandFailed(CommandException e) {
        assertFalse("Test command could not be started: " + ExceptionUtils.getStackTrace(e),
                    ExceptionUtils.getStackTrace(e).contains("Cannot run program"));
    }

    private ImageCommand command(String executable) {
        return new ImageCommand(executable);
    }
}
