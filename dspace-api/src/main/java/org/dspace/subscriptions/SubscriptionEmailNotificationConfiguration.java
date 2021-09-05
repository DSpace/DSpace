/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Implementation of {@link DSpaceRunnable} to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */

public class SubscriptionEmailNotificationConfiguration<T
        extends SubscriptionEmailNotification> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public boolean isAllowedToExecute(Context context) {
        return true;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();
            options.addOption("t", "Type", true,
                    "Subscription type");
            options.getOption("t").setRequired(true);
            options.addOption("f", "Frequency", true,
                    "Subscription frequency");
            options.getOption("f").setRequired(true);
            super.options = options;
        }
        return options;
    }
}
