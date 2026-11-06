/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.mfa.service.MfaService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * CLI script for MFA administration tasks.
 *
 * <p>Provides command-line operations for administrators to manage Multi-Factor
 * Authentication without requiring access to the web UI. Supports querying status,
 * disabling MFA for individual users or all users, and generating new recovery codes.</p>
 *
 * <h3>Usage examples:</h3>
 * <pre>
 *   dspace mfa --status -e user@example.com
 *   dspace mfa --disable -e user@example.com
 *   dspace mfa --disable-all
 *   dspace mfa --generate-recovery-codes -e user@example.com
 * </pre>
 *
 * <p>This script runs with the authorisation system turned off, as it is intended
 * exclusively for system administrators with server access.</p>
 *
 * @author DSpace Contributors
 * @see MfaScriptConfiguration
 * @see MfaService
 */
public class MfaScript extends DSpaceRunnable<MfaScriptConfiguration> {

    /** Target user's email address, provided via {@code -e} option. */
    private String email;

    /** Flag indicating the {@code --disable} operation was requested. */
    private boolean disable;

    /** Flag indicating the {@code --disable-all} bulk operation was requested. */
    private boolean disableAll;

    /** Flag indicating the {@code --status} query was requested. */
    private boolean status;

    /** Flag indicating the {@code --generate-recovery-codes} operation was requested. */
    private boolean generateRecoveryCodes;

    /**
     * {@inheritDoc}
     *
     * @return the {@link MfaScriptConfiguration} bean registered in the Spring context
     */
    @Override
    public MfaScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("mfa", MfaScriptConfiguration.class);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Parses command-line options and validates that required arguments are present.
     * The {@code -e} (email) option is required for all operations except {@code --disable-all}.</p>
     *
     * @throws ParseException if required arguments are missing or no operation is specified
     */
    @Override
    public void setup() throws ParseException {
        email = commandLine.getOptionValue('e');
        disable = commandLine.hasOption("disable");
        disableAll = commandLine.hasOption("disable-all");
        status = commandLine.hasOption("status");
        generateRecoveryCodes = commandLine.hasOption("generate-recovery-codes");

        if (!disableAll && (email == null || email.isBlank())) {
            throw new ParseException("EPerson email (-e) is required unless using --disable-all");
        }
        if (!disable && !disableAll && !status && !generateRecoveryCodes) {
            throw new ParseException(
                "One of --status, --disable, --disable-all, or --generate-recovery-codes is required");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes the requested MFA administration operation within a read-write
     * database context with the authorisation system disabled.</p>
     *
     * @throws Exception if an error occurs during execution
     */
    @Override
    public void internalRun() throws Exception {
        Context context = new Context(Context.Mode.READ_WRITE);
        context.turnOffAuthorisationSystem();

        try {
            EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
            MfaService mfaService = new DSpace().getServiceManager()
                .getServiceByName("org.dspace.mfa.service.impl.MfaServiceImpl", MfaService.class);

            if (disableAll) {
                handleDisableAll(context, mfaService, ePersonService);
            } else {
                EPerson eperson = ePersonService.findByEmail(context, email);
                if (eperson == null) {
                    handler.logError("EPerson not found: " + email);
                    context.abort();
                    return;
                }

                if (status) {
                    handleStatus(context, mfaService, eperson);
                } else if (disable) {
                    handleDisable(context, mfaService, eperson);
                } else if (generateRecoveryCodes) {
                    handleGenerateRecoveryCodes(context, mfaService, eperson);
                }
            }

            context.complete();
        } catch (Exception e) {
            context.abort();
            throw e;
        }
    }

    /**
     * Displays the MFA status for a specific user.
     *
     * @param context    the DSpace context
     * @param mfaService the MFA service
     * @param eperson    the target user
     * @throws Exception if a database or service error occurs
     */
    private void handleStatus(Context context, MfaService mfaService, EPerson eperson) throws Exception {
        boolean enabled = mfaService.isMfaEnabled(context, eperson);
        handler.logInfo("EPerson: " + eperson.getEmail());
        handler.logInfo("MFA enabled: " + enabled);
        if (enabled) {
            int remaining = mfaService.countRemainingRecoveryCodes(context, eperson);
            handler.logInfo("Remaining recovery codes: " + remaining);
        }
    }

    /**
     * Disables MFA for a specific user and invalidates their active session.
     *
     * @param context    the DSpace context
     * @param mfaService the MFA service
     * @param eperson    the target user
     * @throws Exception if a database or service error occurs
     */
    private void handleDisable(Context context, MfaService mfaService, EPerson eperson) throws Exception {
        if (!mfaService.isMfaEnabled(context, eperson)) {
            handler.logInfo("MFA is not enabled for " + eperson.getEmail());
            return;
        }
        mfaService.disable(context, eperson);
        eperson.setSessionSalt("");
        EPersonServiceFactory.getInstance().getEPersonService().update(context, eperson);
        handler.logInfo("MFA disabled for " + eperson.getEmail());
        handler.logInfo("User session invalidated - user must re-login.");
    }

    /**
     * Generates new recovery codes for a user, replacing any existing codes.
     *
     * <p>Codes are printed to log output and must be securely communicated to the user.</p>
     *
     * @param context    the DSpace context
     * @param mfaService the MFA service
     * @param eperson    the target user
     * @throws Exception if a database or service error occurs
     */
    private void handleGenerateRecoveryCodes(Context context, MfaService mfaService, EPerson eperson)
        throws Exception {
        if (!mfaService.isMfaEnabled(context, eperson)) {
            handler.logError("MFA is not enabled for " + eperson.getEmail()
                + ". Cannot generate recovery codes.");
            return;
        }
        List<String> codes = mfaService.generateRecoveryCodes(context, eperson);
        handler.logInfo("New recovery codes for " + eperson.getEmail() + ":");
        for (String code : codes) {
            handler.logInfo("  " + code);
        }
        handler.logWarning("These codes will only be shown once. Provide them to the user securely.");
    }

    /**
     * Disables MFA for all users in the system who have it enabled.
     *
     * <p>Iterates over all EPersons, disables MFA for each active one,
     * and invalidates their sessions.</p>
     *
     * @param context        the DSpace context
     * @param mfaService     the MFA service
     * @param ePersonService the EPerson service for user iteration
     * @throws Exception if a database or service error occurs
     */
    private void handleDisableAll(Context context, MfaService mfaService, EPersonService ePersonService)
        throws Exception {
        handler.logInfo("Disabling MFA for all users...");
        Iterator<EPerson> allUsers = ePersonService.findAll(context, EPerson.EMAIL).iterator();
        int count = 0;
        while (allUsers.hasNext()) {
            EPerson ep = allUsers.next();
            if (mfaService.isMfaEnabled(context, ep)) {
                mfaService.disable(context, ep);
                ep.setSessionSalt("");
                ePersonService.update(context, ep);
                handler.logInfo("  Disabled MFA for: " + ep.getEmail());
                count++;
            }
        }
        handler.logInfo("Done. MFA disabled for " + count + " user(s).");
        if (count > 0) {
            handler.logWarning("All affected users' sessions have been invalidated.");
        }
    }
}
