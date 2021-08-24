/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractIntegrationTest;
import org.dspace.util.FakeConsoleServiceImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class EPersonCLIToolIT
        extends AbstractIntegrationTest {
    private static final String NEW_PASSWORD = "secret";
    private static final String BAD_PASSWORD = "not secret";

    // Handle System.exit() from unit under test.
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    // Capture System.err() output.
    @Rule
    public final SystemErrRule sysErr = new SystemErrRule().enableLog();

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
        consoleService.setPassword(NEW_PASSWORD.toCharArray());

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
            "--newPassword"
        };
        instance.main(argv);

        String newPasswordHash = eperson.getPassword();
        assertNotEquals("Password hash did not change", oldPasswordHash, newPasswordHash);
    }

    /**
     * Test --modify --newPassword with an empty password
     * @throws Exception passed through.
     */
    @Test
    @SuppressWarnings("static-access")
    public void testSetEmptyPassword()
            throws Exception {
        exit.expectSystemExitWithStatus(0);
        System.out.println("main");

        // Create a source of "console" input.
        FakeConsoleServiceImpl consoleService = new FakeConsoleServiceImpl();
        consoleService.setPassword(new char[0]);

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
            "--newPassword"
        };
        instance.main(argv);

        String newPasswordHash = eperson.getPassword();
        assertEquals("Password hash changed", oldPasswordHash, newPasswordHash);

        String stderr = sysErr.getLog();
        assertTrue("Standard error did not mention 'empty'",
                stderr.contains(EPersonCLITool.ERR_PASSWORD_EMPTY));
    }

    /**
     * Test --modify --newPassword with mismatched confirmation.
     * This tests what happens when the user enters different strings at the
     * first and second new-password prompts.
     * @throws Exception passed through.
     */
    @Test
    @SuppressWarnings("static-access")
    public void testSetMismatchedPassword()
            throws Exception {
        exit.expectSystemExitWithStatus(0);
        System.out.println("main");

        // Create a source of "console" input.
        FakeConsoleServiceImpl consoleService = new FakeConsoleServiceImpl();
        consoleService.setPassword1(NEW_PASSWORD.toCharArray());
        consoleService.setPassword2(BAD_PASSWORD.toCharArray());

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
            "--newPassword"
        };
        instance.main(argv);

        String newPasswordHash = eperson.getPassword();
        assertEquals("Password hash changed", oldPasswordHash, newPasswordHash);

        String stderr = sysErr.getLog();
        assertTrue("Standard error did not indicate password mismatch",
                stderr.contains(EPersonCLITool.ERR_PASSWORD_NOMATCH));
    }
}
