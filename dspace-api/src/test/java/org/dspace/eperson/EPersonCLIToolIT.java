/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import com.ginsberg.junit.exit.SystemExitExtension;
import org.dspace.AbstractIntegrationTest;
import org.dspace.util.FakeConsoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@ExtendWith(SystemExitExtension.class)
public class EPersonCLIToolIT
        extends AbstractIntegrationTest {
    private static final String NEW_PASSWORD = "secret";
    private static final String BAD_PASSWORD = "not secret";

    /**
     * Test --modify --newPassword
     * @throws Exception passed through.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    @SuppressWarnings("static-access")
    public void testSetPassword()
            throws Exception {
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
        assertNotEquals(oldPasswordHash, newPasswordHash, "Password hash did not change");
    }

    /**
     * Test --modify --newPassword with an empty password
     * @throws Exception passed through.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    @SuppressWarnings("static-access")
    public void testSetEmptyPassword()
            throws Exception {
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
        String stderr = tapSystemErr(() -> {
            instance.main(argv);
        });
        String newPasswordHash = eperson.getPassword();
        assertEquals(oldPasswordHash, newPasswordHash, "Password hash changed");
        assertTrue(stderr.contains(EPersonCLITool.ERR_PASSWORD_EMPTY),
                "Standard error did not mention 'empty'");
    }

    /**
     * Test --modify --newPassword with mismatched confirmation.
     * This tests what happens when the user enters different strings at the
     * first and second new-password prompts.
     * @throws Exception passed through.
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    @SuppressWarnings("static-access")
    public void testSetMismatchedPassword()
            throws Exception {
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
        String stderr = tapSystemErr(() -> {
            instance.main(argv);
        });

        String newPasswordHash = eperson.getPassword();
        assertEquals(oldPasswordHash, newPasswordHash, "Password hash changed");

        assertTrue(stderr.contains(EPersonCLITool.ERR_PASSWORD_NOMATCH),
                "Standard error did not indicate password mismatch");
    }
}
