/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration for the {@link MfaScript} CLI command.
 *
 * <p>Defines the available command-line options and their descriptions for the
 * MFA administration script. Registered as a Spring bean to be discoverable
 * by the DSpace script runner framework.</p>
 *
 * <h3>Available options:</h3>
 * <ul>
 *   <li>{@code -e, --eperson} — EPerson email address (required unless using --disable-all)</li>
 *   <li>{@code --disable} — Disable MFA for the specified user</li>
 *   <li>{@code --disable-all} — Disable MFA for ALL users (bulk operation)</li>
 *   <li>{@code --status} — Show MFA status for the specified user</li>
 *   <li>{@code --generate-recovery-codes} — Generate new recovery codes for the user</li>
 *   <li>{@code -h, --help} — Display help information</li>
 * </ul>
 *
 * @param <T> the concrete {@link MfaScript} type
 * @author DSpace Contributors
 * @see MfaScript
 */
public class MfaScriptConfiguration<T extends MfaScript> extends ScriptConfiguration<T> {

    /** The runnable class that this configuration is associated with. */
    private Class<T> dspaceRunnableClass;

    /**
     * {@inheritDoc}
     *
     * @return the {@link MfaScript} class (or subclass) to instantiate
     */
    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * {@inheritDoc}
     *
     * @param dspaceRunnableClass the runnable class to set
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the CLI options accepted by the MFA administration script.</p>
     *
     * @return the configured {@link Options} object
     */
    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption("e", "eperson", true, "EPerson email address (required unless using --disable-all)");
        options.addOption(null, "disable", false, "Disable MFA for the user");
        options.addOption(null, "disable-all", false, "Disable MFA for ALL users (bulk operation)");
        options.addOption(null, "status", false, "Show MFA status for the user");
        options.addOption(null, "generate-recovery-codes", false, "Generate new recovery codes");
        options.addOption("h", "help", false, "Display help");
        return options;
    }
}
