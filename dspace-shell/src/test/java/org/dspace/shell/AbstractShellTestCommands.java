/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.shell;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.test.ShellAssertions;
import org.springframework.shell.test.ShellTestClient;
import org.springframework.shell.test.ShellTestClient.InteractiveShellSession;
import org.springframework.shell.test.ShellTestClient.NonInteractiveShellSession;
import org.springframework.shell.test.autoconfigure.ShellTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

/**
 * Abstract class for commands
 * @author paulo-graca
 *
 */
@ShellTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractShellTestCommands {

    /**
     * How much time, in seconds, the DSpace Spring Shell app takes to load, an estimate
     */
    public static final int SHELL_LOADING_SECONDS = 15;

    /**
     * DSpaceShellApplication client
     */
    @Autowired
    protected ShellTestClient client;

    /**
     * Shell interactive session
     */
    protected InteractiveShellSession interactiveSession;

    /**
     * Shell non interactive session
     */
    protected NonInteractiveShellSession nonInteractiveSession;

    /**
     * Setup interactive session
     */
    @BeforeEach
    public void setupSession() {
        interactiveSession = client
                .interactive()
                .run();

        nonInteractiveSession = client
                .nonInterative("help")
                .run();
    }

    /**
     * Executes an interactive shell command waiting for the result
     * @param command command to execute
     * @param expectedOutput expected String in the result
     */
    protected void runCommandAndExpectOutput(String command, String expectedOutput) {
        interactiveSession.write(interactiveSession.writeSequence().text(command).carriageReturn().build());
        await().atMost(SHELL_LOADING_SECONDS, TimeUnit.SECONDS)
               .untilAsserted(() ->
                   ShellAssertions.assertThat(interactiveSession.screen()).containsText(expectedOutput));
    }
}
