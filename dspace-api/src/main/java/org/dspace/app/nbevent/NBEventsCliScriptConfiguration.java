/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import org.apache.commons.cli.Options;

public class NBEventsCliScriptConfiguration<T extends NBEventsRunnable> extends NBEventsScriptConfiguration<T> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("h", "help", false, "help");
        options.getOption("h").setType(boolean.class);
        super.options = options;
        return options;
    }

}
