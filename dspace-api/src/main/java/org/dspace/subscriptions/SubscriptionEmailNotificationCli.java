/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Extension of {@link SubscriptionEmailNotification} for CLI.
 */
public class SubscriptionEmailNotificationCli<T extends ScriptConfiguration<?>>
    extends SubscriptionEmailNotification<T> {

    /**
     * Constructor for SubscriptionEmailNotificationCli.
     * Command-line interface wrapper for SubscriptionEmailNotification script.
     * 
     * @param scriptConfiguration The CLI script configuration with command-line options
     */
    public SubscriptionEmailNotificationCli(T scriptConfiguration) {
        super(scriptConfiguration);
    }
}