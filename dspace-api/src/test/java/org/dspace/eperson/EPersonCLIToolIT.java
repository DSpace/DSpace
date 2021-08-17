/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.junit.Assert.assertNotEquals;

import org.dspace.AbstractIntegrationTest;
import org.dspace.util.FakeConsoleServiceImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class EPersonCLIToolIT
        extends AbstractIntegrationTest {
    private static final String NEW_PASSWORD = "secret";

    // Handle System.exit() from unit under test.
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    /**
     * Test --modify --newPassword
     * @throws Exception passed through.
     */
    @Test
    @SuppressWarnings("static-access")
    public void testSetPassword()
            throws Exception {
        exit.expectSystemExitWithStatus(0);
        System.out.println("main");

        // Create a source of "console" input.
        FakeConsoleServiceImpl consoleService = new FakeConsoleServiceImpl();
        consoleService.setPassword("secret".toCharArray());

        // Make certain that we know the eperson's email and old password hash.
        String email = eperson.getEmail();
        String oldPasswordHash = eperson.getPassword();

        // Instantiate the unit under test.
        EPersonCLITool instance = new EPersonCLITool();
        instance.setConsoleService(consoleService);

        // Test!
        String[] argv = {
            "--modify",
            "--email", email,
            "--newPassword", NEW_PASSWORD
        };
        instance.main(argv);

        String newPasswordHash = eperson.getPassword();
        assertNotEquals("Password hash did not change", oldPasswordHash, newPasswordHash);
    }
}
