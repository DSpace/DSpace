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
import org.springframework.shell.test.autoconfigure.ShellTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

/**
 * Tests Database shell commands
 * @author paulo-graca
 *
 */
@ShellTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class DatabaseCommandsTest {

    @Autowired
    protected ShellTestClient client;
    /**
     * Test the test method, it's expected to try to connect to the database
     */
    @Test
    public void dbTestExecutionTest() {
        InteractiveShellSession interactiveSession = client
                .interactive()
                .run();

        await().atMost(AbstractShellTestCommands.SHELL_LOADING_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            ShellAssertions.assertThat(interactiveSession.screen())
                .containsText("shell");
        });

        interactiveSession.write(interactiveSession.writeSequence().text("database test").carriageReturn().build());
        await().atMost(AbstractShellTestCommands.SHELL_LOADING_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            ShellAssertions.assertThat(interactiveSession.screen())
                .containsText("Attempting to connect to database");
        });

    }
}