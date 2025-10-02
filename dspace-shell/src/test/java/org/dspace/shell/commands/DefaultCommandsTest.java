/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.dspace.shell.AbstractShellTestCommands;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.test.ShellAssertions;
import org.springframework.shell.test.ShellTestClient;
import org.springframework.shell.test.ShellTestClient.InteractiveShellSession;
import org.springframework.shell.test.ShellTestClient.NonInteractiveShellSession;
import org.springframework.shell.test.autoconfigure.ShellTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@ShellTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
/**
 * This class aims to facilitate tests to working DSpace Shell interface
 * @author paulo-graca
 *
 */
public class DefaultCommandsTest {

    /**
     * How much time, in seconds, the DSpace Spring Shell app takes to load, an estimate
     */
    public static final int DSPACE_SPRING_SHELL_LOADING_SECONDS = 20;

    @Autowired
    protected ShellTestClient client;

    /**
     * Test if interactive shell is working by executing the 'help' command
     */
    @Test
    public void interativeHelpTest() {
        InteractiveShellSession interactiveSession = client
                .interactive()
                .run();

        await().atMost(AbstractShellTestCommands.SHELL_LOADING_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            ShellAssertions.assertThat(interactiveSession.screen())
                .containsText("shell");
        });

        interactiveSession.write(interactiveSession.writeSequence().text("help").carriageReturn().build());
        await().atMost(AbstractShellTestCommands.SHELL_LOADING_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            ShellAssertions.assertThat(interactiveSession.screen())
                .containsText("AVAILABLE COMMANDS");
        });
    }

    /**
     * Test if non interactive shell is working by executing the 'help' command
     */
    @Test
    public void nonInterativeHelpTest() {
        NonInteractiveShellSession nonInteractiveSession = client
                .nonInterative("help")
                .run();

        await().atMost(AbstractShellTestCommands.SHELL_LOADING_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            ShellAssertions.assertThat(nonInteractiveSession.screen())
                .containsText("AVAILABLE COMMANDS");
        });
    }
}
