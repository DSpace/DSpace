/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.script;

import org.apache.commons.cli.Options;

/**
 * Extension of {@link OpenaireEventsImportScriptConfiguration} to run the script on
 * console.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class OpenaireEventsImportCliScriptConfiguration<T extends OpenaireEventsImport>
    extends OpenaireEventsImportScriptConfiguration<T> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("h", "help", false, "help");
        options.getOption("h").setType(boolean.class);
        super.options = options;
        return options;
    }

}
