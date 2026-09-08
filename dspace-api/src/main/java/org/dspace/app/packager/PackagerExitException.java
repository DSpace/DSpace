/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

/**
 * Exception thrown by Packager to indicate the process should exit with a specific status code.
 * This replaces direct System.exit() calls to make the Packager testable when called via reflection
 * (e.g., from ScriptLauncher in integration tests).
 *
 * @author DSpace Developers
 */
public class PackagerExitException extends RuntimeException {
    private final int exitCode;

    /**
     * Create a new PackagerExitException with the specified exit code.
     *
     * @param exitCode the exit code that would have been passed to System.exit()
     */
    public PackagerExitException(int exitCode) {
        super("Packager exiting with code " + exitCode);
        this.exitCode = exitCode;
    }

    /**
     * Create a new PackagerExitException with the specified exit code and message.
     *
     * @param exitCode the exit code that would have been passed to System.exit()
     * @param message  the detail message
     */
    public PackagerExitException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * Get the exit code.
     *
     * @return the exit code
     */
    public int getExitCode() {
        return exitCode;
    }
}
